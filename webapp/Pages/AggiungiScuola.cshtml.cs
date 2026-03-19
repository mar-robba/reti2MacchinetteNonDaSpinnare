using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Aggiungi Scuola – STM pag.20, Sequenza pag.36.
/// Mostra form → Controlla elenco scuole → Aggiungi oppure MostraErrore.
/// </summary>
public class AggiungiScuolaModel : PageModel
{
    private readonly ApiService _api;

    [BindProperty] public string Nome { get; set; } = "";
    [BindProperty] public string Indirizzo { get; set; } = "";
    [BindProperty] public string Citta { get; set; } = "";
    [BindProperty] public string Provincia { get; set; } = "";
    [BindProperty] public string Cap { get; set; } = "";

    public string? Messaggio { get; set; }
    public string? Errore { get; set; }

    public AggiungiScuolaModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    public void OnGet() { }

    public async Task<IActionResult> OnPostAsync()
    {
        // Validazione (pag.20: "Dati non validi")
        if (string.IsNullOrWhiteSpace(Nome))
        {
            Errore = "Dati non validi: il nome è obbligatorio";
            return Page();
        }

        var result = await _api.AggiungiScuolaAsync(Nome, Indirizzo, Citta, Provincia, Cap);

        if (result != null)
        {
            var root = result.RootElement;
            if (root.TryGetProperty("id", out var idElem))
            {
                Messaggio = $"Scuola aggiunta con successo (ID: {idElem.GetInt32()})";
                Nome = ""; Indirizzo = ""; Citta = ""; Provincia = ""; Cap = "";
            }
            else if (root.TryGetProperty("errore", out var errElem))
            {
                Errore = errElem.GetString();
            }
        }
        else
        {
            Errore = "Errore di connessione al server";
        }

        return Page();
    }
}
