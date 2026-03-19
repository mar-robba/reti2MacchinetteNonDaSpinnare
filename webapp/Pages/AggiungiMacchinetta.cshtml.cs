using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Aggiungi Macchinetta – STM pag.21, Sequenza pag.37.
/// </summary>
public class AggiungiMacchinettaModel : PageModel
{
    private readonly ApiService _api;

    [BindProperty] public int IdScuola { get; set; }
    [BindProperty] public string Nome { get; set; } = "";

    public string? Messaggio { get; set; }
    public string? Errore { get; set; }
    public List<ScuolaItem> Scuole { get; set; } = new();

    public AggiungiMacchinettaModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    public async Task OnGetAsync()
    {
        await CaricaScuole();
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (string.IsNullOrWhiteSpace(Nome) || IdScuola <= 0)
        {
            Errore = "Errore nella compilazione del form";
            await CaricaScuole();
            return Page();
        }

        var result = await _api.AggiungiMacchinettaAsync(IdScuola, Nome);

        if (result != null)
        {
            var root = result.RootElement;
            if (root.TryGetProperty("id", out var idElem))
            {
                Messaggio = $"Macchinetta aggiunta con successo (ID: {idElem.GetInt32()})";
                Nome = "";
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

        await CaricaScuole();
        return Page();
    }

    private async Task CaricaScuole()
    {
        var result = await _api.GetScuoleAsync();
        if (result != null)
        {
            foreach (var elem in result.RootElement.EnumerateArray())
            {
                Scuole.Add(new ScuolaItem
                {
                    Id = elem.GetProperty("id").GetInt32(),
                    Nome = elem.GetProperty("nome").GetString() ?? ""
                });
            }
        }
    }

    public class ScuolaItem
    {
        public int Id { get; set; }
        public string Nome { get; set; } = "";
    }
}
