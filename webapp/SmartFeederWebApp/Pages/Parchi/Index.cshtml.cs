using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using SmartFeederWebApp.Models;
using SmartFeederWebApp.Services;

namespace SmartFeederWebApp.Pages.Parchi;

[Authorize]
public class IndexModel : PageModel
{
    private readonly IServerRestService _api;

    public IndexModel(IServerRestService api)
    {
        _api = api;
    }

    public List<ParcoDto> Parchi { get; set; } = new();

    public async Task OnGetAsync()
    {
        try
        {
            Parchi = await _api.GetParchiAsync();
        }
        catch (Exception ex)
        {
            Parchi = new List<ParcoDto>();
            ModelState.AddModelError("", "Errore comunicazione con il server: " + ex.Message);
        }
    }

    public async Task<IActionResult> OnPostDeleteAsync(int id)
    {
        await _api.DeleteParcoAsync(id);
        return RedirectToPage();
    }
}
