package microservizi.interfacciautente;

import microservizi.db.DBManagement;
import microservizi.mqtt.MQTTConfig;
import microservizi.mqtt.MQTTPublisher;
import microservizi.mqtt.MQTTSubscriber;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Interfacciautente – Logica del microservizio Interfaccia Utente.
 * Come da diagramma delle classi pagina 6.
 *
 * Gestisce la comunicazione tra l'utente (GUI) e gli altri microservizi
 * (Cassa, Erogatore, Assistenza) tramite MQTT.
 */
public class Interfacciautente {

    private int idMacchinetta;
    private String mqttUsername;
    private String mqttPassword;
    private String topicRadix;
    private ArrayList<MQTTSubscriber> listaSubscriber;
    private MQTTConfig mqttConfig;
    private DBManagement db;
    private InterfacciaUtenteGUI gui;

    /**
     * @param idMacchinetta id della macchinetta
     * @param password      password MQTT
     * @param devOrTest     "dev" o "test"
     */
    public Interfacciautente(int idMacchinetta, String password, String devOrTest) {
        this.idMacchinetta = idMacchinetta; // passato dagli argomenti del mai una volta che si runna
        this.mqttUsername = "macchinetta" + idMacchinetta;
        this.mqttPassword = password;
        this.topicRadix = "macchinetta/" + idMacchinetta + "/";
        this.listaSubscriber = new ArrayList<>();
        this.mqttConfig = new MQTTConfig("localhost", 1883, mqttUsername, mqttPassword);
        this.db = new DBManagement(devOrTest, idMacchinetta);
    }


    // ==================================== SET METODS ==========================================
    public void setGui(InterfacciaUtenteGUI gui) { this.gui = gui; }

    // ==================================== END SET METODS ==========================================

    // ==================================== GET METODS ==========================================
    // spesso subordinate alle sucessive
    // spesso query al database
                                            // radice
    public String getTopicRadix() { return topicRadix; }// non usato perche si usa l'incapsulamento (quell'uso è perchè viene usato da un test)

    public ArrayList<MQTTSubscriber> getListaSubscribers() { return listaSubscriber; }

    /**
     * Restituisce l'elenco delle bevande disponibili dal DB locale.
     * STM pag.9: MostraBevande – esclude quelle non disponibili.
     */
    protected List<String> getBevande() {
        List<String> bevande = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nome, prezzo FROM bevande WHERE disponibile = 1")) {
            while (rs.next()) {
                bevande.add(rs.getInt("id") + ": " + rs.getString("nome"));
            }
        } catch (SQLException e) {
            System.err.println("[InterfacciaUtente] Errore lettura bevande: " + e.getMessage());
        }
        return bevande;
    }

    /**
     * Legge il prezzo di una bevanda dal DB.
     */
    public double getPrezzoBevanda(int numeroBevanda) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT prezzo FROM bevande WHERE id = ? AND disponibile = 1")) {
            ps.setInt(1, numeroBevanda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("prezzo");
            }
        } catch (SQLException e) {
            System.err.println("[InterfacciaUtente] Errore: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Legge la quantità di zucchero disponibile dallo stoccaggio.
     */
    public int getZuccheroDisponibile() {
        // per ogni get si apre una nuova connessione, non sarebbe meglio aprirla una sola volta
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT zucchero FROM cialde WHERE id = 1")) {
            if (rs.next()) return rs.getInt("zucchero");
        } catch (SQLException e) {
            System.err.println("[InterfacciaUtente] Errore: " + e.getMessage());
        }
        return 0;
    }

    // ====================================  END GET METODS  ==========================================


    // ==================== Azioni invocate dalla GUI dunque anche azioni dell'utente =====================
    // Spesso Operazioni MQTT
    /**
     * L'utente seleziona una bevanda.
     */
    public void selezionaBevanda(int numeroBevanda) {
        double prezzo = getPrezzoBevanda(numeroBevanda);
        if (prezzo >= 0 && gui != null) {
            gui.mostraPrezzo(prezzo);
        } else if (gui != null) {
            gui.mostraErrore("Bevanda non disponibile");
        }
    }

    /**
     * L'utente inserisce una moneta.
     */
    public void inserisciMoneta(double importo) {

        createPublisher(String.valueOf(importo), topicRadix + "cassa/monete");
    }

    /**
     * L'utente chiede indietro le monete.
     */
    public void richiediMoneteIndietro() {
        createPublisher("true", topicRadix + "cassa/ridaiSoldi");
    }

    /**
     * L'utente conferma l'acquisto (preme Enter).
     */
    public void confermaAcquisto(int numeroBevanda, int zucchero) {
        // Prima invia il livello zucchero all'erogatore
        createPublisher(String.valueOf(zucchero), topicRadix + "erogatore/setZucchero");
        // Poi invia il numero della bevanda alla cassa
        createPublisher(String.valueOf(numeroBevanda), topicRadix + "cassa/numeroBevanda");
    }

    // ==================== END Azioni invocate dalla GUI dunque anche azioni dell'utente =====================

    // =================================== MQTT SUPPORT ============================

    /**
     * Crea e registra un subscriber MQTT.
     */
    private MQTTSubscriber createSubscriber(String topic) {
        MQTTSubscriber sub = new MQTTSubscriber(mqttConfig, topic, this::sceltaRisposta);
        listaSubscriber.add(sub);
        return sub;
    }

    /**
     * Pubblica un messaggio MQTT sul topic specificato.
     */
    private void createPublisher(String message, String topic) {
        MQTTPublisher publisher = new MQTTPublisher(mqttConfig);
        publisher.publish(topic, message);
        publisher.disconnect();
    }

    /**
     * Routing dei messaggi MQTT ricevuti.
     * Aggiorna la GUI in base al tipo di risposta.
     */
    protected void sceltaRisposta(String currentTopic, String mex) {
        System.out.println("[InterfacciaUtente] Ricevuto: " + currentTopic + " → " + mex);

        if (currentTopic.endsWith("/creditoInserito")) {
            double credito = Double.parseDouble(mex);
            if (gui != null) gui.aggiornaCreditoInserito(credito);

        } else if (currentTopic.endsWith("/espelliMoneta")) {
            if (gui != null) gui.mostraMessaggio("Moneta espulsa: " + mex + "€");

        } else if (currentTopic.endsWith("/moneteRestituite")) {
            double importo = Double.parseDouble(mex);
            if (gui != null) gui.mostraMoneteRestituite(importo);

        } else if (currentTopic.endsWith("/bevandaErogata")) {
            if (gui != null) gui.mostraBevandaErogata(mex);

        } else if (currentTopic.endsWith("/resto")) {
            double resto = Double.parseDouble(mex);
            if (gui != null) gui.mostraResto(resto);

        } else if (currentTopic.endsWith("/errore")) {
            if (gui != null) gui.mostraErrore(mex);

        } else {
            System.out.println("[InterfacciaUtente] Topic non gestito: " + currentTopic);
        }
    }

    /**
     * Avvia tutti i subscriber per ricevere risposte.
     */
    public void startListening() {
        createSubscriber(topicRadix + "interfaccia/creditoInserito");
        createSubscriber(topicRadix + "interfaccia/espelliMoneta");
        createSubscriber(topicRadix + "interfaccia/moneteRestituite");
        createSubscriber(topicRadix + "interfaccia/bevandaErogata");
        createSubscriber(topicRadix + "interfaccia/resto");
        createSubscriber(topicRadix + "interfaccia/errore");
        System.out.println("[InterfacciaUtente] " + listaSubscriber.size() + " subscriber attivi.");
    }

    public void stopListening() {
        for (MQTTSubscriber sub : listaSubscriber) sub.disconnect();
        listaSubscriber.clear();
    }

    // =================================== END MQTT SUPPORT ============================


// ======================= MAIN ====================
    // perche tale eettere il main nell'iterfaccia utente
//Però la scelta attuale ha una giustificazione: Interfacciautente è il cuore del microservizio, la GUI 
// è solo uno strato di presentazione intercambiabile. Mettendo il main nella logica si lascia aperta la
//e porta a eseguire il microservizio in modalità headless (senza GUI, magari per test automatici o da terminale), 
// semplicemente non creando la GUI. Con il main nella GUI invece, avviare il microservizio senza interfaccia grafica 
// richiederebbe comunque di passare dalla classe GUI._
    /**
     * Entry point del microservizio InterfacciaUtente.
     * Argomenti: idMacchinetta mqttPassword [devOrTest]
     */
    // Java sintax reminder: il metodo main può costruire l'oggette della crasse che lo contiene perchè è un metodo statico
    // Chiamare il main da un'altra classe non crea un nuovo processo che esegue questa parte di codice ma soltanto esegue il codice  nel processo già esistente 

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: Interfacciautente <idMacchinetta> <mqttPassword> [dev|test]");
            System.exit(1);
        }

        int idMacchinetta = Integer.parseInt(args[0]);
        String mqttPassword = args[1];
        String mode = args.length > 2 ? args[2] : "dev";

        Interfacciautente interfaccia = new Interfacciautente(idMacchinetta, mqttPassword, mode);
        // avvia gli ascoltatori i subscriber dei topic : sezioni MQTT SUPPORTO
        interfaccia.startListening();

        // Crea e mostra la GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            InterfacciaUtenteGUI gui = new InterfacciaUtenteGUI(interfaccia);
            interfaccia.setGui(gui);
        });
    }
}
