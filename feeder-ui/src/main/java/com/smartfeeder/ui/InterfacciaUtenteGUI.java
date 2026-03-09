package com.smartfeeder.ui;

import javax.swing.*;
import java.awt.*;

/**
 * GUI Swing semplificata per il distributore Smart Feeder.
 * Schermino con: un pulsante "Inserisci Moneta (1€)", un display di stato.
 */
public class InterfacciaUtenteGUI extends JFrame {

    private final int idDistributore;
    private final InterfacciaUtente interfaccia;

    private JLabel labelSchermo;
    private JButton btnInserisciMoneta;

    public InterfacciaUtenteGUI(int idDistributore, InterfacciaUtente interfaccia) {
        this.idDistributore = idDistributore;
        this.interfaccia = interfaccia;
        initComponents();
    }

    private void initComponents() {
        setTitle("Smart Feeder #" + idDistributore);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(350, 250);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Pannello schermino
        JPanel panelSchermo = new JPanel();
        panelSchermo.setBackground(new Color(40, 40, 40));
        panelSchermo.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        labelSchermo = new JLabel("Inserisci moneta", SwingConstants.CENTER);
        labelSchermo.setForeground(Color.GREEN);
        labelSchermo.setFont(new Font("Monospaced", Font.BOLD, 18));
        panelSchermo.add(labelSchermo);
        add(panelSchermo, BorderLayout.CENTER);

        // Pannello pulsante
        JPanel panelBottoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnInserisciMoneta = new JButton("🪙 Inserisci Moneta (1€)");
        btnInserisciMoneta.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnInserisciMoneta.setPreferredSize(new Dimension(250, 50));
        btnInserisciMoneta.addActionListener(e -> {
            interfaccia.inviaMonetaInserita();
        });
        panelBottoni.add(btnInserisciMoneta);
        add(panelBottoni, BorderLayout.SOUTH);

        // Titolo
        JLabel labelTitolo = new JLabel("Smart Feeder #" + idDistributore, SwingConstants.CENTER);
        labelTitolo.setFont(new Font("SansSerif", Font.BOLD, 14));
        labelTitolo.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(labelTitolo, BorderLayout.NORTH);
    }

    /**
     * Aggiorna il testo sullo schermino.
     */
    public void aggiornaSchermo(String testo) {
        SwingUtilities.invokeLater(() -> {
            labelSchermo.setText(testo);
        });
    }

    /**
     * Disabilita la GUI (Fuori Servizio).
     */
    public void disabilitaGUI() {
        SwingUtilities.invokeLater(() -> {
            labelSchermo.setText("FUORI SERVIZIO");
            labelSchermo.setForeground(Color.RED);
            btnInserisciMoneta.setEnabled(false);
        });
    }

    /**
     * Riabilita la GUI.
     */
    public void abilitaGUI() {
        SwingUtilities.invokeLater(() -> {
            labelSchermo.setText("Inserisci moneta");
            labelSchermo.setForeground(Color.GREEN);
            btnInserisciMoneta.setEnabled(true);
        });
    }
}
