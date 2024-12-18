package com.equitycalc.ui.fx.components;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.stage.Popup;
import javafx.scene.shape.Path;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Shape;
import java.awt.Robot;
import java.awt.AWTException;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Interpolator;
import javafx.scene.effect.DropShadow;

public class MatrixTile extends Region {
    // Make these public static for matrix access
    public static final double TILE_SIZE = 15;
    // Reduce magnification scale to prevent matrix coverage
    public static final double MAGNIFICATION_SCALE = 2.5; // Changed from 4.0
    private static final Duration MAGNIFY_DURATION = Duration.millis(150);
    private static final double CORNER_RADIUS = 2;
    private static final Duration HOVER_DELAY = Duration.millis(300);
    private static final Color OUTLINE_COLOR = Color.web("#FFFFFF", 0.08);
    private static final double OUTLINE_WIDTH = 0.5;
    private static final double EDGE_DETECTION_THRESHOLD = 0.3; // Increased detection area
    private static final double CURSOR_OFFSET = 2; // Reduced offset
    private static final long MOVE_DELAY = 100; // ms between moves
    private long lastMoveTime = 0;
    
    private final int row;
    private final int col;
    private final int matrixSize;
    private Timeline hoverTimer;
    
    // Colors from shadcn palette
    private static final Color UNSELECTED_COLOR = Color.web("#3F3F46");
    private static final Color SELECTED_COLOR = Color.web("#22C55E");
    private static final Color HOVER_COLOR = Color.web("#52525B");
    private static final Color TEXT_COLOR = Color.web("#FFFFFF", 0.87);
    
    public enum Mode {
        HEATMAP,
        EDIT
    }
    
    // Remove final modifiers since these are initialized in initializeComponents
    private Rectangle background;
    private Text handText;
    private final String handType;
    private boolean isSelected;
    private Mode currentMode;
    private double equity;
    private double weight;
    private boolean isMagnified = false;

    // Add new constants for text positioning
    private static final double TITLE_Y_POSITION = 0.35;
    private static final double STATS_Y_POSITION = 0.75;
    private static final double TITLE_FONT_SIZE = 16;
    private static final double STATS_FONT_SIZE = 11;
    
    private static final double ADJACENT_SCALE = 0.85;
    private ParallelTransition currentAnimation;
    
    // Add new animation constants
    private static final Duration ANIMATION_DURATION = Duration.millis(150); // Reduced from 200
    private static final Interpolator ANIMATION_INTERPOLATOR = Interpolator.EASE_OUT; // Changed from EASE_BOTH
    private static final double MAGNIFIED_OPACITY = 0.95; // Slight transparency for better context
    
    // Add padding constant for text
    private static final double TEXT_PADDING = 4;
    private static final double TEXT_SCALE_THRESHOLD = 0.8; // Maximum width relative to tile size

    public MatrixTile(String handType, int row, int col, int matrixSize) {
        this.handType = handType;
        this.row = row;
        this.col = col;
        this.matrixSize = matrixSize;
        this.currentMode = Mode.EDIT;
        
        setPrefSize(TILE_SIZE, TILE_SIZE);
        setMinSize(TILE_SIZE, TILE_SIZE);
        setMaxSize(TILE_SIZE, TILE_SIZE);
        
        initializeComponents();
    }
    
    private void initializeComponents() {
        background = new Rectangle(TILE_SIZE, TILE_SIZE);
        background.setArcWidth(CORNER_RADIUS * 2);
        background.setArcHeight(CORNER_RADIUS * 2);
        
        handText = new Text(handType);
        handText.setFont(Font.font("Inter", FontWeight.MEDIUM, 8));
        handText.setFill(TEXT_COLOR);
        
        double textX = (TILE_SIZE - handText.getBoundsInLocal().getWidth()) / 2;
        double textY = (TILE_SIZE + handText.getBoundsInLocal().getHeight()) / 2 - 1;
        handText.setX(textX);
        handText.setY(textY);
        
        getChildren().addAll(background, handText);
        updateBackgroundColor();
    }
    
    private void updateBackgroundColor() {
        if (currentMode == Mode.EDIT) {
            background.setFill(isSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
            handText.setVisible(true);
        } else {
            if (handText != null) { // Guard against NPE
                handText.setVisible(false);
            }
            if (!isSelected) {
                background.setFill(UNSELECTED_COLOR);
            } else {
                Color equityColor = getEquityColor(equity);
                Color weightColor = getWeightColor(weight);
                background.setFill(blendColors(equityColor, weightColor, weight));
            }
        }
        updateCornerRounding();
    }

    private void updateCornerRounding() {
        boolean isTopLeft = row == 0 && col == 0;
        boolean isTopRight = row == 0 && col == matrixSize - 1;
        boolean isBottomLeft = row == matrixSize - 1 && col == 0;
        boolean isBottomRight = row == matrixSize - 1 && col == matrixSize - 1;
        
        // Reset clip and corners
        background.setClip(null);
        background.setArcWidth(0);
        background.setArcHeight(0);
        
        // Add subtle outline
        background.setStroke(OUTLINE_COLOR);
        background.setStrokeWidth(OUTLINE_WIDTH);
        
        // Create corner path only for corner tiles
        if (isTopLeft || isTopRight || isBottomLeft || isBottomRight) {
            Path cornerPath = new Path();
            if (isTopLeft) {
                cornerPath.getElements().addAll(
                    new MoveTo(CORNER_RADIUS, 0),
                    new LineTo(TILE_SIZE, 0),
                    new LineTo(TILE_SIZE, TILE_SIZE),
                    new LineTo(0, TILE_SIZE),
                    new LineTo(0, CORNER_RADIUS),
                    new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, CORNER_RADIUS, 0, false, true)
                );
            } else if (isTopRight) {
                cornerPath.getElements().addAll(
                    new MoveTo(0, 0),
                    new LineTo(TILE_SIZE - CORNER_RADIUS, 0),
                    new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, TILE_SIZE, CORNER_RADIUS, false, false),
                    new LineTo(TILE_SIZE, TILE_SIZE),
                    new LineTo(0, TILE_SIZE),
                    new LineTo(0, 0)
                );
            } else if (isBottomLeft) {
                cornerPath.getElements().addAll(
                    new MoveTo(0, 0),
                    new LineTo(TILE_SIZE, 0),
                    new LineTo(TILE_SIZE, TILE_SIZE),
                    new LineTo(CORNER_RADIUS, TILE_SIZE),
                    new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, 0, TILE_SIZE - CORNER_RADIUS, false, false),
                    new LineTo(0, 0)
                );
            } else if (isBottomRight) {
                cornerPath.getElements().addAll(
                    new MoveTo(0, 0),
                    new LineTo(TILE_SIZE, 0),
                    new LineTo(TILE_SIZE, TILE_SIZE - CORNER_RADIUS),
                    new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, TILE_SIZE - CORNER_RADIUS, TILE_SIZE, false, false),
                    new LineTo(0, TILE_SIZE),
                    new LineTo(0, 0)
                );
            }
            cornerPath.setFill(background.getFill());
            background.setClip(cornerPath);
        }
    }

    private Shape createCornerClip(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        Path path = new Path();
        
        // Start from top-left
        if (topLeft) {
            path.getElements().add(new MoveTo(CORNER_RADIUS, 0));
            path.getElements().add(new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, 0, CORNER_RADIUS, false, true));
        } else {
            path.getElements().add(new MoveTo(0, 0));
        }
        
        // Move to top-right
        if (topRight) {
            path.getElements().add(new LineTo(TILE_SIZE - CORNER_RADIUS, 0));
            path.getElements().add(new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, TILE_SIZE, CORNER_RADIUS, false, false));
        } else {
            path.getElements().add(new LineTo(TILE_SIZE, 0));
        }
        
        // Move to bottom-right
        if (bottomRight) {
            path.getElements().add(new LineTo(TILE_SIZE, TILE_SIZE - CORNER_RADIUS));
            path.getElements().add(new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, TILE_SIZE - CORNER_RADIUS, TILE_SIZE, false, false));
        } else {
            path.getElements().add(new LineTo(TILE_SIZE, TILE_SIZE));
        }
        
        // Move to bottom-left
        if (bottomLeft) {
            path.getElements().add(new LineTo(CORNER_RADIUS, TILE_SIZE));
            path.getElements().add(new ArcTo(CORNER_RADIUS, CORNER_RADIUS, 0, 0, TILE_SIZE - CORNER_RADIUS, false, false));
        } else {
            path.getElements().add(new LineTo(0, TILE_SIZE));
        }
        
        path.getElements().add(new LineTo(0, 0));
        return path;
    }

    public boolean isScaled() {
        return getScaleX() != 1.0 || getScaleY() != 1.0;
    }
    
    public void setMagnified(boolean magnified) {
        if (this.isMagnified == magnified) return;
        
        this.isMagnified = magnified;
        if (magnified) {
            createMagnifiedView();
        } else {
            restoreNormalView();
        }
    }

    public void setMagnified(boolean magnified, Point2D mousePoint) {
        if (this.isMagnified == magnified) return;
        
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        this.isMagnified = magnified;
        if (magnified) {
            createMagnifiedView(mousePoint);
        } else {
            restoreNormalView();
        }
    }

    private void createMagnifiedView() {
        double magnifiedSize = TILE_SIZE * MAGNIFICATION_SCALE;
        
        Rectangle magnifiedBg = new Rectangle(magnifiedSize, magnifiedSize);
        magnifiedBg.setFill(background.getFill());
        
        // Create title text with proper sizing
        Text magnifiedText = new Text(handType);
        magnifiedText.setFont(Font.font("Inter", FontWeight.MEDIUM, TITLE_FONT_SIZE));
        magnifiedText.setFill(TEXT_COLOR);
        
        // Create stats text with wrapping
        Text statsText = new Text(String.format("Equity: %.1f%%\nWeight: %.1f%%", equity, weight * 100));
        statsText.setFont(Font.font("Inter", FontWeight.MEDIUM, STATS_FONT_SIZE));
        statsText.setFill(TEXT_COLOR);
        statsText.setTextAlignment(TextAlignment.CENTER);
        
        // Calculate text bounds and adjust if necessary
        double titleWidth = magnifiedText.getBoundsInLocal().getWidth();
        double statsWidth = statsText.getBoundsInLocal().getWidth();
        
        // Scale down font sizes if text is too wide
        if (titleWidth > magnifiedSize * 0.9) {
            double scaleFactor = (magnifiedSize * 0.9) / titleWidth;
            magnifiedText.setFont(Font.font("Inter", FontWeight.MEDIUM, TITLE_FONT_SIZE * scaleFactor));
        }
        
        if (statsWidth > magnifiedSize * 0.9) {
            double scaleFactor = (magnifiedSize * 0.9) / statsWidth;
            statsText.setFont(Font.font("Inter", FontWeight.MEDIUM, STATS_FONT_SIZE * scaleFactor));
        }
        
        // Recalculate positions after potential font adjustments
        double magnifiedHeight = magnifiedSize;
        double textX = (magnifiedSize - magnifiedText.getBoundsInLocal().getWidth()) / 2;
        double statsX = (magnifiedSize - statsText.getBoundsInLocal().getWidth()) / 2;
        
        magnifiedText.setX(textX);
        magnifiedText.setY(magnifiedHeight * TITLE_Y_POSITION);
        
        statsText.setX(statsX);
        statsText.setY(magnifiedHeight * STATS_Y_POSITION);
        
        // Clear and add magnified elements
        getChildren().clear();
        getChildren().addAll(magnifiedBg, magnifiedText, statsText);
        
        toFront();
        
        setTranslateX(-magnifiedSize / 2 + TILE_SIZE / 2);
        setTranslateY(-magnifiedSize / 2 + TILE_SIZE / 2);
    }

    private void createMagnifiedView(Point2D mousePoint) {
        double magnifiedSize = TILE_SIZE * MAGNIFICATION_SCALE;

        // Calculate the position in grid coordinates
        double gridX = getLayoutX();
        double gridY = getLayoutY();

        // Create magnification components
        Rectangle magnifiedBg = new Rectangle(magnifiedSize, magnifiedSize);
        magnifiedBg.setFill(background.getFill());
        magnifiedBg.setOpacity(MAGNIFIED_OPACITY);

        // Create and position texts
        Text magnifiedText = new Text(handType);
        magnifiedText.setFont(Font.font("Inter", FontWeight.BOLD, TITLE_FONT_SIZE));
        magnifiedText.setFill(TEXT_COLOR);

        Text statsText = new Text(String.format("Equity: %.1f%%\nWeight: %.1f%%", equity * 100, weight * 100));
        statsText.setFont(Font.font("Inter", FontWeight.MEDIUM, STATS_FONT_SIZE));
        statsText.setFill(TEXT_COLOR);
        statsText.setTextAlignment(TextAlignment.CENTER);

        // Update UI
        getChildren().clear();
        getChildren().addAll(magnifiedBg, magnifiedText, statsText);

        // Position the texts within the magnified view
        layoutMagnifiedContent(magnifiedSize, magnifiedText, statsText);

        // Setup animations with corrected positioning
        setupMagnificationAnimations(magnifiedText, statsText, mousePoint, magnifiedSize);

        // Correct the position of the magnified tile
        double offsetX = (magnifiedSize - TILE_SIZE) / 2;
        double offsetY = (magnifiedSize - TILE_SIZE) / 2;
        setTranslateX(gridX - offsetX);
        setTranslateY(gridY - offsetY);

        toFront();
    }

    private void layoutMagnifiedContent(double magnifiedSize, Text titleText, Text statsText) {
        // Ensure text fits within bounds with padding
        double maxWidth = magnifiedSize - (TEXT_PADDING * 2);
        
        // Scale title text if needed
        double titleWidth = titleText.getBoundsInLocal().getWidth();
        if (titleWidth > maxWidth * TEXT_SCALE_THRESHOLD) {
            double scale = (maxWidth * TEXT_SCALE_THRESHOLD) / titleWidth;
            titleText.setFont(Font.font("Inter", FontWeight.BOLD, TITLE_FONT_SIZE * scale));
        }
        
        // Scale stats text if needed
        double statsWidth = statsText.getBoundsInLocal().getWidth();
        if (statsWidth > maxWidth * TEXT_SCALE_THRESHOLD) {
            double scale = (maxWidth * TEXT_SCALE_THRESHOLD) / statsWidth;
            statsText.setFont(Font.font("Inter", FontWeight.MEDIUM, STATS_FONT_SIZE * scale));
        }
        
        // Recalculate positions with padding
        titleText.setX(TEXT_PADDING + (maxWidth - titleText.getBoundsInLocal().getWidth()) / 2);
        titleText.setY(magnifiedSize * TITLE_Y_POSITION);
        
        statsText.setX(TEXT_PADDING + (maxWidth - statsText.getBoundsInLocal().getWidth()) / 2);
        statsText.setY(magnifiedSize * STATS_Y_POSITION);
        
        // Add drop shadow
        DropShadow shadow = new DropShadow(3, Color.BLACK);
        titleText.setEffect(shadow);
        statsText.setEffect(shadow);
    }

    private void setupMagnificationAnimations(Text titleText, Text statsText, Point2D mousePoint, double magnifiedSize) {
        // Set pivot point to center of original tile
        setScaleX(1.0);
        setScaleY(1.0);
        
        ScaleTransition scaleUp = new ScaleTransition(ANIMATION_DURATION, this);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(MAGNIFICATION_SCALE);
        scaleUp.setToY(MAGNIFICATION_SCALE);
        
        // Add fade transitions
        FadeTransition fadeInTitle = new FadeTransition(ANIMATION_DURATION, titleText);
        fadeInTitle.setFromValue(0.0);
        fadeInTitle.setToValue(1.0);
        
        FadeTransition fadeInStats = new FadeTransition(ANIMATION_DURATION, statsText);
        fadeInStats.setFromValue(0.0);
        fadeInStats.setToValue(1.0);
        
        // Combine and play animations
        currentAnimation = new ParallelTransition(scaleUp, fadeInTitle, fadeInStats);
        currentAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        
        // Position first, then animate
        updatePosition(mousePoint, magnifiedSize);
        currentAnimation.play();
    }

    private void updatePosition(Point2D mousePoint, double magnifiedSize) {
        // Calculate grid-based positioning
        double gridX = getLayoutX();
        double gridY = getLayoutY();
    
        // Calculate offsets to center the magnified view on the original tile
        double offsetX = (magnifiedSize - TILE_SIZE) / 2;
        double offsetY = (magnifiedSize - TILE_SIZE) / 2;
    
        // Update position maintaining grid alignment
        setTranslateX(gridX - offsetX);
        setTranslateY(gridY - offsetY);
    
        toFront();
    }

    private double calculateOptimalX(double mouseX, double magnifiedSize) {
        double sceneX = localToScene(0, 0).getX();
        double maxX = getScene().getWidth() - magnifiedSize;
        return Math.min(Math.max(-magnifiedSize/3, mouseX - magnifiedSize/2), maxX);
    }

    private double calculateOptimalY(double mouseY, double magnifiedSize) {
        double sceneY = localToScene(0, 0).getY();
        double maxY = getScene().getHeight() - magnifiedSize;
        return Math.min(Math.max(-magnifiedSize/3, mouseY - magnifiedSize/2), maxY);
    }

    private void restoreNormalView() {
        ScaleTransition scaleDown = new ScaleTransition(ANIMATION_DURATION, this);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(ANIMATION_INTERPOLATOR);
        
        FadeTransition fadeOut = new FadeTransition(ANIMATION_DURATION, this);
        fadeOut.setToValue(1.0);
        fadeOut.setInterpolator(ANIMATION_INTERPOLATOR);
        
        currentAnimation = new ParallelTransition(scaleDown, fadeOut);
        currentAnimation.setOnFinished(e -> {
            setTranslateX(0);
            setTranslateY(0);
            getChildren().clear();
            getChildren().addAll(background, handText);
            updateBackgroundColor();
        });
        currentAnimation.play();
    }
    
    public void setMode(Mode mode) {
        this.currentMode = mode;
        updateBackgroundColor();
    }
    
    public void setHeatmapValues(double equity, double weight) {
        this.equity = equity;
        this.weight = weight;
        if (currentMode == Mode.HEATMAP) {
            updateBackgroundColor();
        }
    }
    
    private Color getEquityColor(double equity) {
        // Convert equity (0-100) to color
        if (equity >= 80) return Color.web("#22C55E"); // Strong hands - Green
        if (equity >= 60) return Color.web("#EAB308"); // Medium hands - Yellow
        if (equity >= 40) return Color.web("#F97316"); // Marginal hands - Orange
        return Color.web("#EF4444"); // Weak hands - Red
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateBackgroundColor();
    }
    
    private Color getWeightColor(double weight) {
        return Color.web("#3F3F46").interpolate(Color.web("#22C55E"), weight);
    }
    
    private Color blendColors(Color c1, Color c2, double weight) {
        return c1.interpolate(c2, weight);
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public String getHandType() {
        return handType;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}