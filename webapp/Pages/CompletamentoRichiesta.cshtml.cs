using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Completamento Richiesta – STM pag.29-34, Sequenza pag.44.
/// Il tecnico vede le richieste aperte, esegue l'azione e chiude la richiesta.
/// Azioni: SvuotaCassa, RiempiCialde, RiempiBicchierini, AggiungiZucchero, AggiustaMacchinetta.
/// </summary>
public class CompletamentoRichiestaModel : PageModel
{
    private readonly ApiService _api;
    public List<RichiestaDisplay> Richieste { get; set; } = new();
    public string? Messaggio { get; set; }
    public string? Errore { get; set; }

    public CompletamentoRichiestaModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    public async Task OnGetAsync()
    {
        await CaricaRichieste();
    }

    public async Task<IActionResult> OnPostAsync(int idRichiesta, string azione)
    {
        switch (azione)
        {
            case "SVUOTA_CASSA":
                Messaggio = "Cassa svuotata con successo.";
                break;
            case "RIEMPI_CIALDE":
                Messaggio = "Cialde riempite con successo.";
                break;
            case "AGGIUNGI_ZUCCHERO":
                Messaggio = "Zucchero aggiunto con successo.";
                break;
            case "RIEMPI_BICCHIERINI":
                Messaggio = "Bicchierini riempiti con successo.";
                break;
            case "AGGIUSTA_MACCHINETTA":
                Messaggio = "Macchinetta aggiustata con successo.";
                break;
            case "ELIMINA_RICHIESTA":
                bool eliminata = await _api.EliminaRichiestaAsync(idRichiesta);
                if (eliminata)
                    Messaggio = "Richiesta completata ed eliminata.";
                else
                    Errore = "Errore durante l'eliminazione della richiesta.";
                break;
            default:
                Errore = "Azione non riconosciuta.";
                break;
        }

        // Se l'azione non è "elimina", mostra il messaggio ma non elimina ancora
        // Il tecnico dovrà poi premere "Elimina Richiesta" per chiuderla
        await CaricaRichieste();
        return Page();
    }

    private async Task CaricaRichieste()
    {
        Richieste.Clear();
        var result = await _api.GetRichiesteAsync();
        if (result != null)
        {
            foreach (var elem in result.RootElement.EnumerateArray())
            {
                Richieste.Add(new RichiestaDisplay
                {
                    Id = elem.GetProperty("id").GetInt32(),
                    IdMacchinetta = elem.GetProperty("id_macchinetta").GetInt32(),
                    NomeMacchinetta = elem.TryGetProperty("nome_macchinetta", out var nm) ? nm.GetString() ?? "" : "",
                    NomeScuola = elem.TryGetProperty("nome_scuola", out var ns) ? ns.GetString() ?? "" : "",
                    TipoGuasto = elem.GetProperty("tipo_guasto").GetString() ?? "",
                    Stato = elem.GetProperty("stato").GetString() ?? "",
                    DataApertura = elem.TryGetProperty("data_apertura", out var da) ? da.GetString() ?? "" : ""
                });
            }
        }
    }

    public class RichiestaDisplay
    {
        public int Id { get; set; }
        public int IdMacchinetta { get; set; }
        public string NomeMacchinetta { get; set; } = "";
        public string NomeScuola { get; set; } = "";
        public string TipoGuasto { get; set; } = "";
        public string Stato { get; set; } = "";
        public string DataApertura { get; set; } = "";
    }
}
