using System.Text;
using System.Text.Json;

namespace PissirWebApp.Services;

/// <summary>
/// Servizio per comunicare con il Server REST (Apache Spark).
/// Centralizza tutte le chiamate HTTP verso l'API backend.
/// </summary>
public class ApiService
{
    private readonly HttpClient _httpClient;

    public ApiService(IHttpClientFactory httpClientFactory)
    {
        _httpClient = httpClientFactory.CreateClient("ServerREST");
    }

    // =================== SCUOLE ===================

    /// <summary>
    /// Recupera l'elenco di tutte le scuole disponibili.
    /// </summary>
    public async Task<JsonDocument?> GetScuoleAsync()
    {
        var response = await _httpClient.GetAsync("scuole");
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Aggiunge una nuova scuola al sistema inviando i dati al Server REST.
    /// </summary>
    public async Task<JsonDocument?> AggiungiScuolaAsync(string nome, string indirizzo, string citta, string provincia, string cap)
    {
        var body = new { nome, indirizzo, citta, provincia, cap };
        var response = await _httpClient.PostAsync("scuole", ToJsonContent(body));
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Elimina una scuola dal sistema, comprese tutte le sue macchinette associate.
    /// </summary>
    public async Task<bool> EliminaScuolaAsync(int id)
    {
        var response = await _httpClient.DeleteAsync($"scuole/{id}");
        return response.IsSuccessStatusCode;
    }

    // =================== MACCHINETTE ===================

    /// <summary>
    /// Recupera l'elenco di tutte le macchinette registrate nel sistema.
    /// </summary>
    public async Task<JsonDocument?> GetMacchinetteAsync()
    {
        var response = await _httpClient.GetAsync("macchinette");
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Recupera l'elenco delle macchinette appartenenti a una specifica scuola.
    /// </summary>
    public async Task<JsonDocument?> GetMacchinetteByScuolaAsync(int idScuola)
    {
        var response = await _httpClient.GetAsync($"scuole/{idScuola}/macchinette");
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Recupera lo stato dettagliato di una specifica macchinetta (livelli, guasti, ecc.).
    /// </summary>
    public async Task<JsonDocument?> GetStatoMacchinettaAsync(int id)
    {
        var response = await _httpClient.GetAsync($"macchinette/{id}/stato");
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Aggiunge una nuova macchinetta a una specifica scuola.
    /// </summary>
    public async Task<JsonDocument?> AggiungiMacchinettaAsync(int idScuola, string nome)
    {
        var body = new { id_scuola = idScuola, nome };
        var response = await _httpClient.PostAsync("macchinette", ToJsonContent(body));
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Elimina una macchinetta esistente.
    /// </summary>
    public async Task<bool> EliminaMacchinettaAsync(int id)
    {
        var response = await _httpClient.DeleteAsync($"macchinette/{id}");
        return response.IsSuccessStatusCode;
    }

    /// <summary>
    /// Invia una richiesta di intervento tecnico per una specifica macchinetta e un tipo di guasto.
    /// </summary>
    public async Task<JsonDocument?> InviaTecnicoAsync(int idMacchinetta, string tipoGuasto)
    {
        var body = new { tipo_guasto = tipoGuasto };
        var response = await _httpClient.PostAsync($"macchinette/{idMacchinetta}/invia-tecnico", ToJsonContent(body));
        return await ParseResponseAsync(response);
    }

    // =================== RICHIESTE TECNICO ===================

    /// <summary>
    /// Recupera tutte le richieste di intervento tecnico pendenti o processate.
    /// </summary>
    public async Task<JsonDocument?> GetRichiesteAsync()
    {
        var response = await _httpClient.GetAsync("richieste");
        return await ParseResponseAsync(response);
    }

    /// <summary>
    /// Elimina una richiesta di intervento (segna la richiesta come completata/rimossa).
    /// </summary>
    public async Task<bool> EliminaRichiestaAsync(int id)
    {
        var response = await _httpClient.DeleteAsync($"richieste/{id}");
        return response.IsSuccessStatusCode;
    }

    // =================== HELPERS ===================

    private static StringContent ToJsonContent(object obj)
    {
        return new StringContent(JsonSerializer.Serialize(obj), Encoding.UTF8, "application/json");
    }

    private static async Task<JsonDocument?> ParseResponseAsync(HttpResponseMessage response)
    {
        var content = await response.Content.ReadAsStringAsync();
        if (string.IsNullOrEmpty(content)) return null;
        return JsonDocument.Parse(content);
    }
}
