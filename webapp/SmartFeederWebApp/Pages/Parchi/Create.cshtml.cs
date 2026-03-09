using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using SmartFeederWebApp.Models;
using SmartFeederWebApp.Services;

namespace SmartFeederWebApp.Pages.Parchi;

[Authorize(Roles = "admin")]
public class CreateModel : PageModel
{
    private readonly IServerRestService _api;

    public CreateModel(IServerRestService api)
    {
        _api = api;
    }

    [BindProperty]
    public ParcoDto Parco { get; set; } = new();

    public void OnGet() { }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid) return Page();

        await _api.CreateParcoAsync(Parco);
        return RedirectToPage("/Parchi/Index");
    }
}
