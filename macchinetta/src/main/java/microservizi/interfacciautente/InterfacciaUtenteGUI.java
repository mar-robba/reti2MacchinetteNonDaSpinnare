package microservizi.interfacciautente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * InterfacciaUtenteGUI – GUI Swing del distributore automatico.
 * Come da screenshot pagina 47 dello schema progettuale.
 *
 * Layout: lista bevande, display prezzo/credito/zucchero,
 * tastiera numerica, bottone restituzione monete, output erogazione.
 */
public class InterfacciaUtenteGUI extends JFrame {

    private Interfacciautente logica;

    // Componenti display
    private JTextArea listaBevande;
    private JLabel labelPrezzo;
    private JLabel labelCredito;
    private JLabel labelZucchero;
    private JTextField inputBevanda;
    private JLabel labelOutput;
    private JLabel labelMoneteRestituite;
    private JLabel labelMessaggio;

    // Stato corrente
    private int bevandaSelezionata = -1;
    private int livelloZucchero = 0;
    private static final int MAX_ZUCCHERO = 3;

    public InterfacciaUtenteGUI(Interfacciautente logica) {
        this.logica = logica;
        initGUI();
    }

    private void initGUI() {
        setTitle("Macchinetta");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // === Pannello sinistro: lista bevande + output ===
        JPanel pannelloSinistro = new JPanel(new BorderLayout(5, 5));
        pannelloSinistro.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        // Lista bevande
        listaBevande = new JTextArea(10, 25);
        listaBevande.setEditable(false);
        listaBevande.setFont(new Font("Monospaced", Font.PLAIN, 14));
        listaBevande.setBorder(BorderFactory.createTitledBorder("Lista bevande"));
        aggiornaListaBevande();
        pannelloSinistro.add(new JScrollPane(listaBevande), BorderLayout.CENTER);

        // Output bevanda e monete
        JPanel pannelloOutput = new JPanel(new GridLayout(2, 1, 5, 5));
        labelOutput = new JLabel("Nothing to retrieve");
        labelOutput.setBorder(BorderFactory.createTitledBorder("Output bevanda erogata"));
        labelOutput.setFont(new Font("SansSerif", Font.BOLD, 12));
        pannelloOutput.add(labelOutput);

        labelMoneteRestituite = new JLabel("Nothing to retrieve");
        labelMoneteRestituite.setBorder(BorderFactory.createTitledBorder("Monete restituite"));
        labelMoneteRestituite.setFont(new Font("SansSerif", Font.BOLD, 12));
        pannelloOutput.add(labelMoneteRestituite);

        pannelloSinistro.add(pannelloOutput, BorderLayout.SOUTH);
        add(pannelloSinistro, BorderLayout.WEST);

        // === Pannello destro: info, input, tastiera ===
        JPanel pannelloDestro = new JPanel(new BorderLayout(5, 5));
        pannelloDestro.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        // Info prezzo e credito
        JPanel pannelloInfo = new JPanel(new GridLayout(4, 1, 3, 3));
        pannelloInfo.setBorder(BorderFactory.createTitledBorder("Info"));

        labelPrezzo = new JLabel("Prezzo bevanda: —");
        labelPrezzo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pannelloInfo.add(labelPrezzo);

        labelCredito = new JLabel("Importo inserito: —");
        labelCredito.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pannelloInfo.add(labelCredito);

        labelZucchero = new JLabel("Zucchero: 0/" + MAX_ZUCCHERO);
        labelZucchero.setFont(new Font("SansSerif", Font.PLAIN, 13));
        labelZucchero.setBorder(BorderFactory.createTitledBorder("Zucchero"));
        pannelloInfo.add(labelZucchero);

        labelMessaggio = new JLabel(" ");
        labelMessaggio.setFont(new Font("SansSerif", Font.ITALIC, 12));
        labelMessaggio.setForeground(Color.BLUE);
        pannelloInfo.add(labelMessaggio);

        pannelloDestro.add(pannelloInfo, BorderLayout.NORTH);

        // Input bevanda
        JPanel pannelloInput = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pannelloInput.setBorder(BorderFactory.createTitledBorder("Input bevanda"));
        pannelloInput.add(new JLabel("Inserisci il numero:"));
        inputBevanda = new JTextField(5);
        pannelloInput.add(inputBevanda);
        JButton btnInserisci = new JButton("Inserisci");
        btnInserisci.addActionListener(e -> onSelezionaBevanda());
        pannelloInput.add(btnInserisci);
        pannelloDestro.add(pannelloInput, BorderLayout.CENTER);

        // Tastiera numerica + bottoni speciali
        JPanel pannelloTastiera = new JPanel(new GridLayout(5, 3, 3, 3));
        pannelloTastiera.setBorder(BorderFactory.createTitledBorder("Tastiera"));

        String[] tasti = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "0", "-", "Canc", "Enter", "€"};
        for (String tasto : tasti) {
            JButton btn = new JButton(tasto);
            btn.setFont(new Font("SansSerif", Font.BOLD, 14));

            if (tasto.equals("Enter")) {
                btn.setBackground(new Color(76, 175, 80));
                btn.setForeground(Color.WHITE);
            } else if (tasto.equals("Canc")) {
                btn.setBackground(new Color(244, 67, 54));
                btn.setForeground(Color.WHITE);
            }

            btn.addActionListener(createTastoListener(tasto));
            pannelloTastiera.add(btn);
        }

        // Bottone restituzione monete
        JButton btnRestituisci = new JButton("Ridai Monete");
        btnRestituisci.setBackground(Color.RED);
        btnRestituisci.setForeground(Color.WHITE);
        btnRestituisci.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRestituisci.addActionListener(e -> onRichiediMoneteIndietro());

        JPanel pannelloBottoni = new JPanel(new BorderLayout(5, 5));
        pannelloBottoni.add(pannelloTastiera, BorderLayout.CENTER);
        pannelloBottoni.add(btnRestituisci, BorderLayout.SOUTH);

        pannelloDestro.add(pannelloBottoni, BorderLayout.SOUTH);
        add(pannelloDestro, BorderLayout.CENTER);

        // Monete inseribili
        JPanel pannelloMonete = new JPanel(new FlowLayout());
        pannelloMonete.setBorder(BorderFactory.createTitledBorder("Inserisci monete"));
        double[] monete = {0.05, 0.10, 0.20, 0.50, 1.00, 2.00};
        for (double moneta : monete) {
            JButton btn = new JButton(String.format("%.2f€", moneta));
            btn.addActionListener(e -> onInserisciMoneta(moneta));
            pannelloMonete.add(btn);
        }
        add(pannelloMonete, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Crea un listener per i tasti della tastiera numerica.
     */
    private ActionListener createTastoListener(String tasto) {
        return e -> {
            switch (tasto) {
                case "+" -> onCambiaZucchero(1);
                case "-" -> onCambiaZucchero(-1);
                case "Canc" -> onCancel();
                case "Enter" -> onEnter();
                case "€" -> {} // placeholder per inserimento monete fisiche
                default -> {
                    // Numeri: appendi al campo input
                    inputBevanda.setText(inputBevanda.getText() + tasto);
                }
            }
        };
    }

    // === Azioni utente ===

    private void onSelezionaBevanda() {
        try {
            int num = Integer.parseInt(inputBevanda.getText().trim());
            bevandaSelezionata = num;
            logica.selezionaBevanda(num);
            livelloZucchero = 0;
            labelZucchero.setText("Zucchero: 0/" + MAX_ZUCCHERO);
            labelMessaggio.setText("Bevanda selezionata: " + num);
        } catch (NumberFormatException ex) {
            mostraErrore("Inserisci un numero valido");
        }
    }

    private void onInserisciMoneta(double importo) {
        if (bevandaSelezionata < 0) {
            mostraErrore("Seleziona prima una bevanda");
            return;
        }
        logica.inserisciMoneta(importo);
    }

    private void onCambiaZucchero(int delta) {
        int nuovo = livelloZucchero + delta;
        if (nuovo < 0) {
            mostraMessaggio("Non puoi togliere altro zucchero");
            return;
        }
        if (nuovo > MAX_ZUCCHERO) {
            mostraMessaggio("Non puoi aggiungere altro zucchero");
            return;
        }
        livelloZucchero = nuovo;
        labelZucchero.setText("Zucchero: " + livelloZucchero + "/" + MAX_ZUCCHERO);
    }

    private void onEnter() {
        if (bevandaSelezionata < 0) {
            mostraErrore("Seleziona prima una bevanda");
            return;
        }
        logica.confermaAcquisto(bevandaSelezionata, livelloZucchero);
    }

    private void onCancel() {
        bevandaSelezionata = -1;
        livelloZucchero = 0;
        inputBevanda.setText("");
        labelPrezzo.setText("Prezzo bevanda: —");
        labelCredito.setText("Importo inserito: —");
        labelZucchero.setText("Zucchero: 0/" + MAX_ZUCCHERO);
        labelMessaggio.setText(" ");
        labelOutput.setText("Nothing to retrieve");
        labelMoneteRestituite.setText("Nothing to retrieve");
    }

    private void onRichiediMoneteIndietro() {
        logica.richiediMoneteIndietro();
    }

    // === Metodi chiamati dalla logica (callback MQTT) ===

    public void aggiornaListaBevande() {
        List<String> bevande = logica != null ? logica.getBevande() : List.of("(caricamento...)");
        SwingUtilities.invokeLater(() -> {
            listaBevande.setText("");
            for (String b : bevande) {
                listaBevande.append(b + "\n");
            }
        });
    }

    public void mostraPrezzo(double prezzo) {
        SwingUtilities.invokeLater(() -> labelPrezzo.setText("Prezzo bevanda: " + String.format("%.2f", prezzo) + "€"));
    }

    public void aggiornaCreditoInserito(double credito) {
        SwingUtilities.invokeLater(() -> labelCredito.setText("Importo inserito: " + String.format("%.2f", credito) + "€"));
    }

    public void mostraMoneteRestituite(double importo) {
        SwingUtilities.invokeLater(() -> {
            labelMoneteRestituite.setText(String.format("%.2f€ restituite", importo));
            labelCredito.setText("Importo inserito: —");
        });
    }

    public void mostraBevandaErogata(String bevanda) {
        SwingUtilities.invokeLater(() -> labelOutput.setText("Bevanda " + bevanda + " erogata!"));
    }

    public void mostraResto(double resto) {
        SwingUtilities.invokeLater(() -> {
            if (resto > 0) {
                labelMoneteRestituite.setText("Resto: " + String.format("%.2f", resto) + "€");
            }
            // Reset per nuovo acquisto
            bevandaSelezionata = -1;
            livelloZucchero = 0;
            inputBevanda.setText("");
            labelPrezzo.setText("Prezzo bevanda: —");
            labelCredito.setText("Importo inserito: —");
            labelZucchero.setText("Zucchero: 0/" + MAX_ZUCCHERO);
        });
    }

    public void mostraErrore(String messaggio) {
        SwingUtilities.invokeLater(() -> {
            labelMessaggio.setForeground(Color.RED);
            labelMessaggio.setText("Errore: " + messaggio);
        });
    }

    public void mostraMessaggio(String messaggio) {
        SwingUtilities.invokeLater(() -> {
            labelMessaggio.setForeground(Color.BLUE);
            labelMessaggio.setText(messaggio);
        });
    }
}
