using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using PissirWebApp.Services;

namespace PissirWebApp.Pages;

/// <summary>
/// Modello per visualizzare lo stato in dettaglio di una specifica macchinetta.
/// Rileva cassa piena, cialde o bicchieri in esaurimento, guasti e permette l'invio tecnico.
/// </summary>
public class StatoMacchinettaModel : PageModel
{
    private readonly ApiService _api;
    public MacchinettaStato? Macchinetta { get; set; }
    public string? Messaggio { get; set; }
    public string? Errore { get; set; }

    public StatoMacchinettaModel(IHttpClientFactory httpClientFactory)
    {
        _api = new ApiService(httpClientFactory);
    }

    /// <summary>
    /// Richiesta GET: Effettua il fetch dello stato attuale della macchinetta indicata dall'`id`.
    /// </summary>
    public async Task OnGetAsync(int id)
    {
        await CaricaStato(id);
    }

    /// <summary>
    /// Richiesta POST: Avvia una procedura di richiesta intervento tecnico (es: ricarica o riparazione guasto).
    /// </summary>
    public async Task<IActionResult> OnPostAsync(int idMacchinetta, string tipoGuasto)
    {
        var result = await _api.InviaTecnicoAsync(idMacchinetta, tipoGuasto);
        if (result != null)
        {
            var root = result.RootElement;
            if (root.TryGetProperty("messaggio", out var msg))
                Messaggio = msg.GetString();
            else if (root.TryGetProperty("errore", out var err))
                Errore = err.GetString();
        }
        else
        {
            Errore = "Errore di connessione al server";
        }

        await CaricaStato(idMacchinetta);
        return Page();
    }

    /// <summary>
    /// Helper: Legge i flag di stato della macchinetta dal JSON restituito dall'API convertendoli nel modello C#.
    /// Calcola dinamicamente la natura del guasto/problema.
    /// </summary>
    private async Task CaricaStato(int id)
    {
        var result = await _api.GetStatoMacchinettaAsync(id);
        if (result != null)
        {
            var e = result.RootElement;
            bool cp = e.TryGetProperty("flag_cassa_piena", out var f1) && f1.GetBoolean();
            bool ce = e.TryGetProperty("flag_cialde_esaurimento", out var f2) && f2.GetBoolean();
            bool ze = e.TryGetProperty("flag_zucchero_esaurimento", out var f3) && f3.GetBoolean();
            bool be = e.TryGetProperty("flag_bicchieri_esaurimento", out var f4) && f4.GetBoolean();
            bool gg = e.TryGetProperty("flag_guasto_generico", out var f5) && f5.GetBoolean();

            var guasti = new List<string>();
            if (cp) guasti.Add("CASSA_PIENA");
            if (ce) guasti.Add("CIALDE_ESAURIMENTO");
            if (ze) guasti.Add("ZUCCHERO_ESAURIMENTO");
            if (be) guasti.Add("BICCHIERI_ESAURIMENTO");
            if (gg) guasti.Add("GUASTO_GENERICO");

            Macchinetta = new MacchinettaStato
            {
                Id = e.GetProperty("id").GetInt32(),
                Nome = e.GetProperty("nome").GetString() ?? "",
                NomeScuola = e.TryGetProperty("nome_scuola", out var s) ? s.GetString() ?? "" : "",
                Stato = e.GetProperty("stato").GetString() ?? "",
                CassaTotale = e.TryGetProperty("cassa_totale", out var ct) ? ct.GetDouble() : 0,
                FlagCassaPiena = cp, FlagCialdeEsaurimento = ce,
                FlagZuccheroEsaurimento = ze, FlagBicchieriEsaurimento = be,
                FlagGuastoGenerico = gg,
                HasGuasto = guasti.Count > 0,
                GuastoDescrizione = string.Join(",", guasti)
            };
        }
    }

    public class MacchinettaStato
    {
        public int Id { get; set; } public string Nome { get; set; } = "";
        public string NomeScuola { get; set; } = ""; public string Stato { get; set; } = "";
        public double CassaTotale { get; set; }
        public bool FlagCassaPiena { get; set; } public bool FlagCialdeEsaurimento { get; set; }
        public bool FlagZuccheroEsaurimento { get; set; } public bool FlagBicchieriEsaurimento { get; set; }
        public bool FlagGuastoGenerico { get; set; }
        public bool HasGuasto { get; set; } public string GuastoDescrizione { get; set; } = "";
    }
}
