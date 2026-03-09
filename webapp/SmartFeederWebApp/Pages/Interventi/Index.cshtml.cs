using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using SmartFeederWebApp.Models;
using SmartFeederWebApp.Services;

namespace SmartFeederWebApp.Pages.Interventi;

[Authorize]
public class IndexModel : PageModel
{
    private readonly IServerRestService _api;

    public IndexModel(IServerRestService api)
    {
        _api = api;
    }

    public List<TicketGuastoDto> Ticket { get; set; } = new();
    public string? FiltroStato { get; set; }

    public async Task OnGetAsync(string? stato)
    {
        FiltroStato = stato;
        try
        {
            Ticket = await _api.GetTicketAsync(stato);
        }
        catch (Exception ex)
        {
            Ticket = new List<TicketGuastoDto>();
            ModelState.AddModelError("", "Errore: " + ex.Message);
        }
    }

    public async Task<IActionResult> OnPostRisolviAsync(int ticketId)
    {
        await _api.RisolviTicketAsync(ticketId);
        return RedirectToPage();
    }
}
