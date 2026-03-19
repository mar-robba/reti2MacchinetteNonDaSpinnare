using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

public class EliminaMacchinettaModel : PageModel
{
    private readonly ApiService _api;
    public List<MacchinettaItem> Macchinette { get; set; } = new();
    public string? Messaggio { get; set; }
    public string? Errore { get; set; }

    public EliminaMacchinettaModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    public async Task OnGetAsync() { await CaricaMacchinette(); }

    public async Task<IActionResult> OnPostAsync(int idMacchinetta)
    {
        bool eliminata = await _api.EliminaMacchinettaAsync(idMacchinetta);
        Messaggio = eliminata ? "Macchinetta eliminata dall'elenco" : null;
        Errore = eliminata ? null : "Macchinetta non trovata";
        await CaricaMacchinette();
        return Page();
    }

    private async Task CaricaMacchinette()
    {
        Macchinette.Clear();
        var result = await _api.GetMacchinetteAsync();
        if (result != null)
        {
            foreach (var elem in result.RootElement.EnumerateArray())
            {
                Macchinette.Add(new MacchinettaItem
                {
                    Id = elem.GetProperty("id").GetInt32(),
                    Nome = elem.GetProperty("nome").GetString() ?? "",
                    NomeScuola = elem.TryGetProperty("nome_scuola", out var s) ? s.GetString() ?? "" : "",
                    Stato = elem.GetProperty("stato").GetString() ?? ""
                });
            }
        }
    }

    public class MacchinettaItem { public int Id { get; set; } public string Nome { get; set; } = ""; public string NomeScuola { get; set; } = ""; public string Stato { get; set; } = ""; }
}
