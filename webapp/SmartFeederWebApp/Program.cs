using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.OpenIdConnect;
using Microsoft.IdentityModel.Protocols.OpenIdConnect;
using SmartFeederWebApp.Services;
//MArco : un po un file centrale di configurazione
// ------------------------------------------------------------
// Program.cs – entry point and configuration of the SmartFeeder web app
// ------------------------------------------------------------
// This file creates the WebApplication builder, registers services (Razor Pages,
// HttpClient for the Java Spark REST API, authentication with Keycloak), and
// defines the request pipeline (static files, routing, authentication, Razor
// pages mapping). All configuration values are read from appsettings.json.
var builder = WebApplication.CreateBuilder(args);

// Add Razor Pages
builder.Services.AddRazorPages();

// Configure HttpClient for API calls
builder.Services.AddHttpClient<IServerRestService, ServerRestService>(client =>
{
    client.BaseAddress = new Uri(builder.Configuration["ApiSettings:BaseUrl"] ?? "http://localhost:8081");
});

// Configure Authentication with Keycloak
builder.Services.AddAuthentication(options =>
{
    options.DefaultScheme = CookieAuthenticationDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = OpenIdConnectDefaults.AuthenticationScheme;
})
.AddCookie(options =>
{
    options.Cookie.Name = "SmartFeederAuthCookie";
    options.AccessDeniedPath = "/AccessDenied";
})
.AddOpenIdConnect(options =>
{
    options.Authority = builder.Configuration["Keycloak:Authority"];
    options.ClientId = builder.Configuration["Keycloak:ClientId"];
    options.ClientSecret = builder.Configuration["Keycloak:ClientSecret"];
    options.ResponseType = OpenIdConnectResponseType.Code;
    options.SaveTokens = true;
    options.RequireHttpsMetadata = false;
    options.Scope.Add("openid");
    options.Scope.Add("profile");

    // Disable PAR (can cause issues with Keycloak in .NET 8+)
    options.PushedAuthorizationBehavior = PushedAuthorizationBehavior.Disable;

    options.TokenValidationParameters = new Microsoft.IdentityModel.Tokens.TokenValidationParameters
    {
        NameClaimType = "preferred_username",
        RoleClaimType = "roles"
    };
});

builder.Services.AddAuthorization();

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
}
app.UseStaticFiles();

app.UseRouting();

app.UseAuthentication();
app.UseAuthorization();

app.MapRazorPages();

app.Run();
