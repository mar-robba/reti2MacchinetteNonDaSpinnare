package com.smartfeeder.common;

/**
 * Costanti dei topic MQTT per il sistema Smart Feeder.
 * Struttura topic: /smartfeeder/{idDistributore}/{mittente}/{destinatario}/
 */
public class TopicUtils {

    public static final String BASE = "/smartfeeder";

    // Componenti
    public static final String CASSA = "cassa";
    public static final String EROGATORE = "erogatore";
    public static final String ASSISTENZA = "assistenza";
    public static final String INTERFACCIA_UTENTE = "interfaccia_utente";
    public static final String SERVER_REST = "serverREST";

    /**
     * Costruisce un topic nel formato standard.
     */
    public static String buildTopic(int idDistributore, String mittente, String destinatario) {
        return BASE + "/" + idDistributore + "/" + mittente + "/" + destinatario + "/";
    }

    /**
     * Costruisce il prefisso base per un distributore.
     */
    public static String baseDistributore(int id) {
        return BASE + "/" + id + "/";
    }

    // --- Topic predefiniti ---

    public static String cassaAssistenza(int id) {
        return buildTopic(id, CASSA, ASSISTENZA);
    }

    public static String cassaErogatore(int id) {
        return buildTopic(id, CASSA, EROGATORE);
    }

    public static String cassaInterfaccia(int id) {
        return buildTopic(id, CASSA, INTERFACCIA_UTENTE);
    }

    public static String erogatoreAssistenza(int id) {
        return buildTopic(id, EROGATORE, ASSISTENZA);
    }

    public static String erogatoreCassa(int id) {
        return buildTopic(id, EROGATORE, CASSA);
    }

    public static String erogatoreInterfaccia(int id) {
        return buildTopic(id, EROGATORE, INTERFACCIA_UTENTE);
    }

    public static String assistenzaServerRest(int id) {
        return buildTopic(id, ASSISTENZA, SERVER_REST);
    }

    public static String assistenzaInterfaccia(int id) {
        return buildTopic(id, ASSISTENZA, INTERFACCIA_UTENTE);
    }

    public static String assistenzaCassa(int id) {
        return buildTopic(id, ASSISTENZA, CASSA);
    }

    public static String assistenzaErogatore(int id) {
        return buildTopic(id, ASSISTENZA, EROGATORE);
    }

    public static String interfacciaUtenteCassa(int id) {
        return buildTopic(id, INTERFACCIA_UTENTE, CASSA);
    }

    public static String interfacciaUtenteAssistenza(int id) {
        return buildTopic(id, INTERFACCIA_UTENTE, ASSISTENZA);
    }

    public static String serverRestAssistenza(int id) {
        return buildTopic(id, SERVER_REST, ASSISTENZA);
    }

    // --- Messaggi standard ---
    public static final String MSG_CASSA_PIENA = "La cassa è piena";
    public static final String MSG_MANGIME_ESAURITO = "Il mangime è esaurito";
    public static final String MSG_GUASTO_CASSA = "Si è verificato un guasto - cassa";
    public static final String MSG_GUASTO_EROGATORE = "Si è verificato un guasto - erogatore";
    public static final String MSG_GUASTO_INTERFACCIA = "Si è verificato un guasto - interfaccia_utente";
    public static final String MSG_RIPARAZIONE_EFFETTUATA = "riparazione effettuata";
    public static final String MSG_ABILITA_GUI = "abilita gui";
    public static final String MSG_DISABILITA_GUI = "disabilita gui";
    public static final String MSG_EROGAZIONE_COMPLETATA = "erogazione completata";
    public static final String MSG_INSERISCI_MONETA = "moneta inserita";
    public static final String MSG_EROGA = "eroga porzione";
    public static final String MSG_FUORI_SERVIZIO = "fuori servizio";
    public static final String MSG_PING = "ping";
}
