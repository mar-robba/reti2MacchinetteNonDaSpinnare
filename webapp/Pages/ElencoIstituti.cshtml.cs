using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Modello per la pagina che mostra l'elenco di tutte le scuole registrate.
/// </summary>
public class ElencoIstitutiModel : PageModel
{
    private readonly ApiService _api;
    public List<ScuolaDisplay> Scuole { get; set; } = new();

    public ElencoIstitutiModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    /// <summary>
    /// Richiesta GET: Effettua una chiamata all'API per recuperare e mostrare le scuole.
    /// </summary>
    public async Task OnGetAsync()
    {
        var result = await _api.GetScuoleAsync();
        if (result != null)
        {
            foreach (var elem in result.RootElement.EnumerateArray())
            {
                Scuole.Add(new ScuolaDisplay
                {
                    Id = elem.GetProperty("id").GetInt32(),
                    Nome = elem.GetProperty("nome").GetString() ?? "",
                    Indirizzo = elem.TryGetProperty("indirizzo", out var i) ? i.GetString() ?? "" : "",
                    Citta = elem.TryGetProperty("citta", out var c) ? c.GetString() ?? "" : ""
                });
            }
        }
    }

    public class ScuolaDisplay { public int Id { get; set; } public string Nome { get; set; } = ""; public string Indirizzo { get; set; } = ""; public string Citta { get; set; } = ""; }
}
