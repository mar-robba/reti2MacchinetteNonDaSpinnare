1. Configurazione e Avvio

    È necessario avere installato MySQL in locale.

    Il database deve essere creato con il nome esatto ProgettoMacchinetteParteAmministrativa.

    Il server MySQL deve essere in esecuzione sulla porta 3306.

    Per l'accesso al database occorre utilizzare l'utente root e la password zLfVSEAz8rKaUi.

    Per caricare la struttura e i dati, è necessario importare il file .sql di backup che si trova nella cartella backupmysql.

2. Ruolo nel Sistema

    Il DBMS MySQL è adibito alla gestione della parte amministrativa e gestionale del progetto.

    È connesso a un backend RESTful basato su Apache Spark, che si occupa dell'elaborazione dei dati ed esegue le interrogazioni.

3. Struttura dei Dati (Tabelle)

Il database è composto da diverse tabelle necessarie per gestire le macchinette distribuite nei vari istituti scolastici. I "template dati" principali previsti sono:

    Scuola (Istituto):

        Memorizza i dati relativi alle sedi.

        I campi previsti sono: nome, id, città, via e numero civico.

    Macchinetta:

        Tiene traccia dello stato di ogni singolo distributore.

        I campi da salvare includono: id, idScuola (per legarla a un istituto specifico), stato della cassa, disponibilità di cialde, zucchero e bicchierini, oltre a dei flag per gestire le eccezioni o i problemi.

    Richiesta tecnico (Guasti):

        Ogni segnalazione di malfunzionamento o problema viene salvata all'interno di una tabella dedicata.

        Questa entità si lega alla macchinetta e all'istituto salvando i rispettivi id macchinetta e id scuola.

        Contiene svariati flag per indicare la natura precisa della manutenzione richiesta: cassa piena, bicchierini in esaurimento, cialde in esaurimento o zucchero in esaurimento. Da questa tabella il tecnico legge le informazioni per poter operare in loco.
