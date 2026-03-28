using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.OpenIdConnect;
using System.Security.Claims;

// comprendi come la web app si fa ad autenticare al server 

/// <summary>
/// Entry point principale dell'applicazione Web.
/// Configura i servizi, l'autenticazione Keycloak (OpenID Connect), l'HttpClient e la pipeline HTTP.
/// </summary>
var builder = WebApplication.CreateBuilder(args);

// Aggiungi i servizi Razor Pages
builder.Services.AddRazorPages();

// Configura HttpClient per comunicare con il Server REST
builder.Services.AddHttpClient("ServerREST", client =>
{
    client.BaseAddress = new Uri("http://localhost:8081/api/");
    client.DefaultRequestHeaders.Add("Accept", "application/json");//?
});

// Configura autenticazione con Keycloak (OpenID Connect)
builder.Services.AddAuthentication(options =>
{
    options.DefaultScheme = CookieAuthenticationDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = OpenIdConnectDefaults.AuthenticationScheme;
})
.AddCookie()
.AddOpenIdConnect(options =>
{
    options.Authority = "http://localhost:8080/realms/pissir";
    options.ClientId = "pissir-webapp";
    options.ClientSecret = "pissir-secret";
    options.ResponseType = "code";
    options.SaveTokens = true;
    options.GetClaimsFromUserInfoEndpoint = true;
    options.RequireHttpsMetadata = false; // Solo per sviluppo

    options.Scope.Add("openid");
    options.Scope.Add("profile");
    options.Scope.Add("roles");

    // Mappa i ruoli di Keycloak
    options.TokenValidationParameters.RoleClaimType = ClaimTypes.Role;
                                                        // credo che debba essere un file con le regole di Keycloak
    options.ClaimActions.MapJsonKey(ClaimTypes.Role, "realm_roles");
});

builder.Services.AddAuthorization();

/// <summary>
/// Costruzione dell'applicazione con i servizi configurati.
/// </summary>
var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();

app.UseAuthentication();
app.UseAuthorization();

app.MapRazorPages();

/// <summary>
/// Esecuzione dell'applicazione Web.
/// </summary>
app.Run();
