package com.equitycalc.ui.fx.components;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Path;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.ArcTo;

public class EquityBar extends Region {
    private static final double DEFAULT_HEIGHT = 15;
    private static final double DEFAULT_WIDTH = 187;
    private static final double CORNER_RADIUS = 6;
    
    private final Rectangle winSection = new Rectangle();
    private final Rectangle splitSection = new Rectangle();
    private final Rectangle loseSection = new Rectangle();
    private final Text winText = new Text();
    private final Text loseText = new Text();
    private final Pane mainPane = new Pane();
    
    private double winPercentage;
    private double splitPercentage;
    private double losePercentage;
    private boolean isNullState = true;

    public EquityBar() {
        // Set fixed size
        setPrefSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMaxSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        
        // Style the pane
        mainPane.setStyle("-fx-background-color: transparent;");
        
        // Configure text
        winText.setFill(Color.WHITE);
        loseText.setFill(Color.WHITE);
        winText.setFont(Font.font("Inter", 10));
        loseText.setFont(Font.font("Inter", 10));
        
        // Add elements to pane
        mainPane.getChildren().addAll(winSection, splitSection, loseSection, winText, loseText);
        getChildren().add(mainPane);
        
        // Initialize with default values instead of null state
        setEquity(0.0, 0.0, 0.0);
    }

    public void setEquity(double win, double split, double lose) {
        this.winPercentage = win;
        this.splitPercentage = split;
        this.losePercentage = lose;
        this.isNullState = false;
        updateBar();
    }

    public void setNullState() {
        this.isNullState = true;
        updateBar();
    }

    private void updateBar() {
        if (isNullState) {
            drawNullState();
        } else {
            drawEquityState();
        }
    }

    private void drawNullState() {
        mainPane.getChildren().clear();
        
        // Create background
        Rectangle nullRect = new Rectangle(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        nullRect.setFill(Color.web("#52525B"));
        nullRect.setArcWidth(CORNER_RADIUS * 2);
        nullRect.setArcHeight(CORNER_RADIUS * 2);
        
        // Center question mark
        Text questionMark = new Text("?");
        questionMark.setFill(Color.WHITE);
        questionMark.setFont(Font.font("Inter", 10));
        
        // Get text bounds for precise centering
        double textWidth = questionMark.getLayoutBounds().getWidth();
        double textHeight = questionMark.getLayoutBounds().getHeight();
        
        questionMark.setX((DEFAULT_WIDTH - textWidth) / 2);
        // Adjust Y position to account for font baseline
        questionMark.setY((DEFAULT_HEIGHT + textHeight) / 2 - textHeight * 0.2);
        
        mainPane.getChildren().addAll(nullRect, questionMark);
    }

    private void drawEquityState() {
        mainPane.getChildren().clear();
        
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        
        // Calculate widths
        double winWidth = (width * winPercentage) / 100;
        double splitWidth = (width * splitPercentage) / 100;
        double loseWidth = width - winWidth - splitWidth;
        
        // Create solid bar background first
        Rectangle background = new Rectangle(0, 0, width, height);
        background.setArcWidth(CORNER_RADIUS * 2);
        background.setArcHeight(CORNER_RADIUS * 2);
        
        // Create sections with exact positioning
        winSection.setX(0);
        winSection.setWidth(winWidth);
        winSection.setHeight(height);
        winSection.setFill(Color.web("#22C55E"));
        
        splitSection.setX(winWidth);
        splitSection.setWidth(splitWidth);
        splitSection.setHeight(height);
        splitSection.setFill(Color.web("#F59E0B"));
        
        loseSection.setX(winWidth + splitWidth);
        loseSection.setWidth(loseWidth);
        loseSection.setHeight(height);
        loseSection.setFill(Color.web("#EF4444"));
        
        // Add sections to pane
        mainPane.getChildren().addAll(background, winSection, splitSection, loseSection);
        
        // Center text in sections
        if (winWidth > 30) {
            winText.setText(String.format("%.1f%%", winPercentage));
            double textWidth = winText.getLayoutBounds().getWidth();
            double textHeight = winText.getLayoutBounds().getHeight();
            winText.setX(winWidth/2 - textWidth/2);
            winText.setY((height + textHeight)/2 - textHeight * 0.2);
            mainPane.getChildren().add(winText);
        }
        
        if (loseWidth > 30) {
            loseText.setText(String.format("%.1f%%", losePercentage));
            double textWidth = loseText.getLayoutBounds().getWidth();
            double textHeight = loseText.getLayoutBounds().getHeight();
            loseText.setX(winWidth + splitWidth + loseWidth/2 - textWidth/2);
            loseText.setY((height + textHeight)/2 - textHeight * 0.2);
            mainPane.getChildren().add(loseText);
        }
        
        // Apply clip to maintain rounded corners
        Rectangle clip = new Rectangle(0, 0, width, height);
        clip.setArcWidth(CORNER_RADIUS * 2);
        clip.setArcHeight(CORNER_RADIUS * 2);
        mainPane.setClip(clip);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        mainPane.setLayoutX(0);
        mainPane.setLayoutY(0);
        mainPane.setPrefSize(getWidth(), getHeight());
        updateBar();
    }

    private void initializeStyles() {
        setStyle("-fx-background-color: transparent;");
        winSection.setStyle("-fx-background-radius: " + CORNER_RADIUS + ";");
        splitSection.setStyle("-fx-background-radius: 0;");
        loseSection.setStyle("-fx-background-radius: " + CORNER_RADIUS + ";");
    }
}