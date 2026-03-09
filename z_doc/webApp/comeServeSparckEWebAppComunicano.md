> **Nota sull'Iniezione delle Dipendenze (DI):** Il principio fondamentale è che *un oggetto o una funzione che desidera utilizzare un determinato servizio non debba sapere come costruire tali servizi*.

# Come è realizzata l'interfaccia web e dove girano i vari codici

## 1. Architettura generale
Il progetto **SmartFeederWebApp** è un'applicazione ASP.NET Core basata su Razor Pages.

* **Server-side (C#):** Il codice C# (es. `Program.cs`, le Razor Pages, i servizi) viene eseguito sul server web (ASP.NET Core).
* **Client-side (browser):** Il browser riceve HTML, CSS e JavaScript generati dal server. Le pagine sono renderizzate sul server, ma il browser può effettuare chiamate AJAX per aggiornare i dati senza ricaricare l’intera pagina.

> **Conclusione:** L'app non è una pura *Single-Page Application* (SPA), né un puro *Server-Side Rendering* (SSR). È un approccio **ibrido**: rendering iniziale sul server + richieste dinamiche dal client.

---

## 2. Comunicazione con il back-end (Java Spark)

| Parte | Tecnologia | Dove gira | Come comunica |
| :--- | :--- | :--- | :--- |
| **WebApp** (ASP.NET Core) | C# / Razor Pages | Server .NET (es. `dotnet run`) | Espone HTML e endpoint API (es. `/api/...`) per il browser. |
| **ServerREST** (Spark) | Java + Spark framework | Server Java (JAR avviato con `java -jar …` o script `04-start-server-rest.sh`) | Espone REST API (JSON) su `http://localhost:8081` (configurabile). |
| **Browser** | HTML / CSS / JS | Lato client (utente) | Usa `HttpClient` (registrato in `Program.cs`) per chiamare le API del ServerREST. |

Nel file `Program.cs` trovi la configurazione del client HTTP:

```csharp
builder.Services.AddHttpClient<IServerRestService, ServerRestService>(client =>
{
    client.BaseAddress = new Uri(
        builder.Configuration["ApiSettings:BaseUrl"] ?? "http://localhost:8081");
});
```

IServerRestService` is a C# interface that defines the methods for calling the Spark server APIs. 
The HTTP client is injected (via Dependency Injection) into Razor Pages or Blazor components (if present) and sends HTTP requests to the Java backend.

### 3. Typical Page Flow

* **Initial request** → The browser requests `GET /` from the ASP.NET server.
* **ASP.NET** renders the Razor Page (`.cshtml`) and returns it to the browser (static HTML + scripts).
* **Browser** loads the page and, via JavaScript (or via C# calls in Razor), sends an HTTP request to the ServerREST (e.g., `GET /elenco/macchinette`).
* **ServerREST (Spark)** processes the request, accesses the MySQL database (via HikariCP), and returns JSON.
* **Browser** receives the JSON and displays it dynamically (e.g., as a table of machines).

### 4. Security / Authentication

* The app uses **OpenID Connect** with **Keycloak** (`AddOpenIdConnect`).
* After login, the JWT token is saved in cookies (`CookieAuthentication`).
* Calls to the ServerREST include the token (e.g., in the header `Authorization: Bearer <jwt>`), allowing the backend to verify the user's identity.

### 5. Summary

* **C# Code (ASP.NET Core)** → Executed on the web server (rendering + API).
* **Java Code (Spark)** → Executed on another server process (REST API).
* **Browser** → Executes only HTML/CSS/JS; it doesn't contain all the business logic, but calls the backend APIs.

> **Note:** The architecture is hybrid: SSR (Server-Side Rendering) for the initial UI + client-side calls for dynamic data.

---
*If you want to dive deeper into any part (e.g., see the Spark routes, JSON models, or the Keycloak configuration), let me know!*
