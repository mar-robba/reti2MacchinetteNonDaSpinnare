using System.Net.Http.Json;
using SmartFeederWebApp.Models;

namespace SmartFeederWebApp.Services;

/// <summary>
/// Implementazione del servizio REST che consuma le API Java (SparkJava) tramite HttpClient.
/// </summary>

/* Marco:
Services/: Contiene la logica di business e di comunicazione esterna.

    IServerRestService.cs / ServerRestService.cs: Incapsula la logica delle chiamate HTTP (GET, POST, DELETE) verso gli endpoint del ServerREST (/api/parchi, /api/distributori, /api/ticket). Tramite dependency injection di HttpClient, si occupa di serializzare e deserializzare i dati interfacciandosi in modo asincrono alle API.
*/

public class ServerRestService : IServerRestService
{
    
    // HttpClient è una classe del framework .NET che viene utilizzata per effettuare richieste HTTP.
    // Viene fornita dal pacchetto NuGet System.Net.Http.
    private readonly HttpClient _httpClient;

    public ServerRestService(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    // === Parchi ===
    
    // Prende la lista dei parchi 
                //?
    public async Task<List<ParcoDto>> GetParchiAsync()
    {                                                                   //quando dal ho l'utente o il codice del browser effettua tale pat nella url allora viene eseguito tale codice
        var result = await _httpClient.GetFromJsonAsync<List<ParcoDto>>("/api/parchi"); //restituisce la lista de parchi //e come viene sfuttata la definizione DTO. la richiesta al server Spack ma come avviene?
        return result ?? new List<ParcoDto>();
    }

    //le altre funzioni/enpoint dovrebbero essere molto simili alla precedente

    public async Task<ParcoDto?> GetParcoAsync(int id)
    {
        return await _httpClient.GetFromJsonAsync<ParcoDto>($"/api/parchi/{id}");
    }

    public async Task<ParcoDto> CreateParcoAsync(ParcoDto parco)
    {
        var response = await _httpClient.PostAsJsonAsync("/api/parchi", parco);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<ParcoDto>() ?? parco;
    }

    public async Task<bool> DeleteParcoAsync(int id)
    {
        var response = await _httpClient.DeleteAsync($"/api/parchi/{id}");
        return response.IsSuccessStatusCode;
    }

    // === Distributori ===

    public async Task<List<DistributoreDto>> GetDistributoriAsync()
    {
        var result = await _httpClient.GetFromJsonAsync<List<DistributoreDto>>("/api/distributori");
        return result ?? new List<DistributoreDto>();
    }

    public async Task<List<DistributoreDto>> GetDistributoriByParcoAsync(int idParco)
    {
        var result = await _httpClient.GetFromJsonAsync<List<DistributoreDto>>($"/api/distributori/parco/{idParco}");
        return result ?? new List<DistributoreDto>();
    }

    public async Task<DistributoreDto> CreateDistributoreAsync(DistributoreDto distributore)
    {
        var response = await _httpClient.PostAsJsonAsync("/api/distributori", distributore);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<DistributoreDto>() ?? distributore;
    }

    public async Task<bool> DeleteDistributoreAsync(int id)
    {
        var response = await _httpClient.DeleteAsync($"/api/distributori/{id}");
        return response.IsSuccessStatusCode;
    }

    // === Ticket ===

    public async Task<List<TicketGuastoDto>> GetTicketAsync(string? stato = null)
    {
        var url = "/api/ticket";
        if (!string.IsNullOrEmpty(stato))
            url += $"?stato={stato}";
        var result = await _httpClient.GetFromJsonAsync<List<TicketGuastoDto>>(url);
        return result ?? new List<TicketGuastoDto>();
    }

    public async Task<List<TicketGuastoDto>> GetTicketByDistributoreAsync(int idDistributore)
    {
        var result = await _httpClient.GetFromJsonAsync<List<TicketGuastoDto>>($"/api/ticket/distributore/{idDistributore}");
        return result ?? new List<TicketGuastoDto>();
    }

    public async Task<bool> RisolviTicketAsync(int ticketId)
    {
        var response = await _httpClient.PostAsync($"/api/ticket/{ticketId}/risolvi", null);
        return response.IsSuccessStatusCode;
    }
}
