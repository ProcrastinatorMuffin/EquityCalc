package com.equitycalc.ui.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import com.equitycalc.ui.fx.components.EquityBar;
import com.equitycalc.ui.fx.components.HandPanel;
import com.equitycalc.ui.fx.components.ResultDisplay;
import com.equitycalc.model.Card;

public class PlayerPanelController {

    @FXML
    private Label AnalysisLabel;

    @FXML
    private EquityBar equityBar;    

    @FXML
    private HandPanel handPanel;

    @FXML
    private Button HandSelectButton;

    @FXML
    private Button ModeSwitchButton;

    @FXML
    private Label OppActionLabel;

    @FXML
    private Button OppBet100Button;

    @FXML
    private Button OppBet125Button;

    @FXML
    private Button OppBet150Button;

    @FXML
    private Button OppBet33Button;

    @FXML
    private Button OppBet50Button;

    @FXML
    private Button OppBet75Button;

    @FXML
    private Button OppCheckButton;

    @FXML
    private Pane PlayerPanel;

    @FXML
    private Pane PlayerPanelContentPanel;

    @FXML
    private Line PlayerPanelSeparator;

    @FXML
    private Pane PlayerPanelSideBar;

    @FXML
    private Button PowerButton;

    @FXML
    private Button RangeUploadButton;

    @FXML
    private Button RefreshButton;

    @FXML
    private Label ResultLabel;

    @FXML
    private ResultDisplay resultDisplay;

    @FXML
    private Line SideBarSeparator;

    @FXML
    private void initialize() {
        Card aceSpades = new Card(Card.Rank.SEVEN, Card.Suit.SPADES);
        Card kingSpades = new Card(Card.Rank.KING, Card.Suit.SPADES);
        
        equityBar.setEquity(30, 10, 60);
        
        // Use FXML-injected handPanel directly
        handPanel.setCards(aceSpades, kingSpades);
        
        // Initialize ResultDisplay
        resultDisplay.setState(ResultDisplay.State.FOLD);
    }

}
