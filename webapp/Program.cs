using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.OpenIdConnect;
using System.Security.Claims;

var builder = WebApplication.CreateBuilder(args);

// Aggiungi i servizi Razor Pages
builder.Services.AddRazorPages();

// Configura HttpClient per comunicare con il Server REST
builder.Services.AddHttpClient("ServerREST", client =>
{
    client.BaseAddress = new Uri("http://localhost:8081/api/");
    client.DefaultRequestHeaders.Add("Accept", "application/json");
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
    options.ClaimActions.MapJsonKey(ClaimTypes.Role, "realm_roles");
});

builder.Services.AddAuthorization();

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

app.Run();
