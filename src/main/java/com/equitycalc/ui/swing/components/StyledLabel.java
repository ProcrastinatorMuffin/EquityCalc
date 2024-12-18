package com.equitycalc.ui.swing.components;

import com.equitycalc.ui.swing.theme.AppColors;
import javax.swing.JLabel;
import java.awt.Font;

public class StyledLabel extends JLabel {
    public StyledLabel(String text) {
        super(text);
        setForeground(AppColors.TEXT);
        setFont(new Font("SF Pro Display", Font.PLAIN, 12));
    }
}