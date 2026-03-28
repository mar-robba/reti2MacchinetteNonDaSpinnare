using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Modello per la pagina che mostra l'elenco delle macchinette a sistema.
/// Può mostrare tutte le macchinette o filtrarle per una specifica scuola.
/// </summary>
public class ElencoMacchinetteModel : PageModel
{
    private readonly ApiService _api;
    public List<MacchinettaDisplay> Macchinette { get; set; } = new();
    public string? NomeScuola { get; set; }

    public ElencoMacchinetteModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    /// <summary>
    /// Richiesta GET: Recupera dall'API le macchinette esistenti. 
    /// Se specificato `idScuola`, filtra l'elenco.
    /// </summary>
    public async Task OnGetAsync(int? idScuola)
    {
        var result = idScuola.HasValue
            ? await _api.GetMacchinetteByScuolaAsync(idScuola.Value)
            : await _api.GetMacchinetteAsync();

        if (result != null)
        {
            foreach (var elem in result.RootElement.EnumerateArray())
            {
                var m = new MacchinettaDisplay
                {
                    Id = elem.GetProperty("id").GetInt32(),
                    Nome = elem.GetProperty("nome").GetString() ?? "",
                    NomeScuola = elem.TryGetProperty("nome_scuola", out var s) ? s.GetString() ?? "" : "",
                    Stato = elem.GetProperty("stato").GetString() ?? "",
                    FlagCassaPiena = elem.TryGetProperty("flag_cassa_piena", out var f1) && f1.GetBoolean(),
                    FlagCialdeEsaurimento = elem.TryGetProperty("flag_cialde_esaurimento", out var f2) && f2.GetBoolean(),
                    FlagZuccheroEsaurimento = elem.TryGetProperty("flag_zucchero_esaurimento", out var f3) && f3.GetBoolean(),
                    FlagBicchieriEsaurimento = elem.TryGetProperty("flag_bicchieri_esaurimento", out var f4) && f4.GetBoolean(),
                    FlagGuastoGenerico = elem.TryGetProperty("flag_guasto_generico", out var f5) && f5.GetBoolean()
                };
                Macchinette.Add(m);
                if (NomeScuola == null) NomeScuola = m.NomeScuola;
            }
        }

        if (!idScuola.HasValue) NomeScuola = null;
    }

    public class MacchinettaDisplay
    {
        public int Id { get; set; }
        public string Nome { get; set; } = "";
        public string NomeScuola { get; set; } = "";
        public string Stato { get; set; } = "";
        public bool FlagCassaPiena { get; set; }
        public bool FlagCialdeEsaurimento { get; set; }
        public bool FlagZuccheroEsaurimento { get; set; }
        public bool FlagBicchieriEsaurimento { get; set; }
        public bool FlagGuastoGenerico { get; set; }
    }
}
