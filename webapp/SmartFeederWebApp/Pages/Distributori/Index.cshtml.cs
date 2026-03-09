using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using SmartFeederWebApp.Models;
using SmartFeederWebApp.Services;

namespace SmartFeederWebApp.Pages.Distributori;

[Authorize]
public class IndexModel : PageModel
{
    private readonly IServerRestService _api;

    public IndexModel(IServerRestService api)
    {
        _api = api;
    }
                                                                //
    public List<DistributoreDto> Distributori { get; set; } = new();
                //
    public int? IdParco { get; set; }
    public string? NomeParco { get; set; }

    public async Task OnGetAsync(int? idParco)
    {
        IdParco = idParco;
        try
        {
            if (idParco.HasValue)
            {
                Distributori = await _api.GetDistributoriByParcoAsync(idParco.Value);
                var parco = await _api.GetParcoAsync(idParco.Value);
                NomeParco = parco?.Nome;
            }
            else
            {
                Distributori = await _api.GetDistributoriAsync();
            }
        }
        catch (Exception ex)
        {
            Distributori = new List<DistributoreDto>();
            ModelState.AddModelError("", "Errore: " + ex.Message);
        }
    }

    public async Task<IActionResult> OnPostCreateAsync(int idParco)
    {
        var d = new DistributoreDto { IdParco = idParco };
        await _api.CreateDistributoreAsync(d);
        return RedirectToPage(new { idParco });
    }

    public async Task<IActionResult> OnPostDeleteAsync(int id)
    {
        await _api.DeleteDistributoreAsync(id);
        return RedirectToPage(new { idParco = IdParco });
    }
}
