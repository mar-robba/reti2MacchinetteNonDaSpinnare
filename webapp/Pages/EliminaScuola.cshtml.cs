using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Modello per la pagina che permette di rimuovere un istituto/scuola, comprese tutte le sue macchinette.
/// </summary>
public class EliminaScuolaModel : PageModel
{
    private readonly ApiService _api;
    public List<ScuolaItem> Scuole { get; set; } = new();
    public string? Messaggio { get; set; }
    public string? Errore { get; set; }

    public EliminaScuolaModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    /// <summary>
    /// Richiesta GET: Carica le scuole e le visualizza in una lista o tendina.
    /// </summary>
    public async Task OnGetAsync() { await CaricaScuole(); }

    /// <summary>
    /// Richiesta POST: Esegue l'eliminazione in coda su database tramite l'API.
    /// Ricarica i dati al termine per aggiornare la UI.
    /// </summary>
    public async Task<IActionResult> OnPostAsync(int idScuola)
    {
        bool eliminata = await _api.EliminaScuolaAsync(idScuola);
        Messaggio = eliminata ? "Scuola eliminata con tutte le macchinette" : null;
        Errore = eliminata ? null : "Errore durante l'eliminazione";
        await CaricaScuole();
        return Page();
    }

    /// <summary>
    /// Helper: Chiama l'endpoint REST per avere i dati delle scuole aggiornati.
    /// </summary>
    private async Task CaricaScuole()
    {
        Scuole.Clear();
        var result = await _api.GetScuoleAsync();
        if (result != null)
        {
            foreach (var elem in result.RootElement.EnumerateArray())
            {
                Scuole.Add(new ScuolaItem
                {
                    Id = elem.GetProperty("id").GetInt32(),
                    Nome = elem.GetProperty("nome").GetString() ?? "",
                    Citta = elem.TryGetProperty("citta", out var c) ? c.GetString() ?? "" : ""
                });
            }
        }
    }

    public class ScuolaItem { public int Id { get; set; } public string Nome { get; set; } = ""; public string Citta { get; set; } = ""; }
}
