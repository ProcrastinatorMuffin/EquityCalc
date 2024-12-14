package com.equitycalc.ui.panel;

import com.equitycalc.ui.components.button.RunButton;
import com.equitycalc.ui.theme.AppColors;

import com.equitycalc.ui.components.NumberField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class ControlPanel extends JPanel {
    private static final int BUTTON_SIZE = 36;
    private static final int FIELD_HEIGHT = 28;
    private static final Insets PANEL_PADDING = new Insets(12, 16, 12, 16);
    private static final int GROUP_SPACING = 16;
    
    private final RunButton runButton;
    private final NumberField iterationsField;
    private SimulationCallback callback;
    private boolean isSimulationRunning = false;

    public interface SimulationCallback {
        void onSimulationStart(SimulationParameters params);
    }

    public static class SimulationParameters {
        public final int iterations;
        public final int batchSize;
        
    
        public SimulationParameters(int iterations, int batchSize) {
            this.iterations = iterations;
            this.batchSize = batchSize;
        }
    }

    public ControlPanel() {
        super(new BorderLayout(GROUP_SPACING, 0));
        setBackground(AppColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
            PANEL_PADDING.top, 
            PANEL_PADDING.left, 
            PANEL_PADDING.bottom, 
            PANEL_PADDING.right
        ));
        
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled() || isSimulationRunning) return;
                iterationsField.dispatchEvent(e); // Delegate to NumberField
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (!isEnabled() || isSimulationRunning) return;
                iterationsField.dispatchEvent(e); // Delegate to NumberField
            }
        });

        // Left side controls group with improved layout
        JPanel controlsGroup = new JPanel(new GridBagLayout());
        controlsGroup.setBackground(AppColors.BACKGROUND);
        
        // Modern label with SF font
        JLabel iterationsLabel = createStyledLabel("Iterations");
        
        // Configure number field with better sizing
        iterationsField = new NumberField(50000, 1000, 1000000);
        iterationsField.setFocusable(false); // Make focusable
        iterationsField.setMinimumSize(new Dimension(90, FIELD_HEIGHT));
        iterationsField.setPreferredSize(new Dimension(90, FIELD_HEIGHT));
        iterationsField.setToolTipText(
            "<html><b>Number of Monte Carlo simulations</b><br>" +
            "Range: 1,000 - 1,000,000<br><br>" +
            "<span style='color: gray'>Keyboard shortcuts:</span><br>" +
            "↑↓: Adjust by 1,000<br>" +
            "⇧ + ↑↓: Adjust by 10,000<br>"
        );

        // Layout with proper constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.anchor = GridBagConstraints.LINE_START;
        controlsGroup.add(iterationsLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsGroup.add(iterationsField, gbc);

        // Configure run button
        runButton = new RunButton(BUTTON_SIZE);
        runButton.setToolTipText(
            "<html><b>Run Monte Carlo Simulation</b><br>" +
            "<span style='color: gray'>⌘R</span></html>"
        );
        
        runButton.addActionListener(e -> {
            if (callback != null && !isSimulationRunning) {
                SimulationParameters params = new SimulationParameters(
                    getIterations(),
                    1000
                );
                startSimulation();
                callback.onSimulationStart(params);
            }
        });

        // Register keyboard shortcut
        registerKeyboardShortcut();

        // Add components with proper layout
        add(controlsGroup, BorderLayout.CENTER);
        add(runButton, BorderLayout.EAST);
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
        label.setForeground(AppColors.TEXT);
        return label;
    }

    private void registerKeyboardShortcut() {
        String shortcut = System.getProperty("os.name").contains("Mac") ? 
            "meta R" : "control R";
            
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(shortcut), "runSimulation");
        actionMap.put("runSimulation", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (runButton.isEnabled() && !isSimulationRunning) {
                    runButton.doClick();
                }
            }
        });
    }

    public void startSimulation() {
        isSimulationRunning = true;
        runButton.setEnabled(false);
        runButton.setRunning(true);
    }
    
    public void completeSimulation() {
        isSimulationRunning = false;
        runButton.setRunning(false);
        runButton.setEnabled(true);
    }
    
    public void failSimulation() {
        completeSimulation();
    }

    // Add new getters
    public int getIterations() {
        return iterationsField.getValue();
    }

    public void setSimulationCallback(SimulationCallback callback) {
        this.callback = callback;
    }

    public void setRunButtonEnabled(boolean enabled) {
        runButton.setEnabled(enabled);
    }
}