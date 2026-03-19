package microservizi.mqtt;

/**
 * Configurazione per la connessione al broker MQTT Mosquitto.
 */
public class MQTTConfig {

    private String brokerUrl;
    private String username;
    private String password;

    public MQTTConfig(String brokerHost, int brokerPort, String username, String password) {
        this.brokerUrl = "tcp://" + brokerHost + ":" + brokerPort;
        this.username = username;
        this.password = password;
    }

    /**
     * Configurazione di default per connessione locale.
     */
    public MQTTConfig() {
        this("localhost", 1883, "macchinetta1", "password1");
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
