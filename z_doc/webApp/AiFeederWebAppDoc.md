# Documentazione Tecnica: Smart Feeder WebApp

## 1. Panoramica del Progetto

La **Smart Feeder WebApp** è l'interfaccia utente web del sistema *Smart Feeder*, progettata per permettere agli operatori e agli amministratori di monitorare e gestire i parchi, i distributori (dispositivi IoT) e i ticket di guasto/manutenzione.

- **Tecnologia Principale**: ASP.NET Core MVC (Razor Pages) con .NET (6.0 o superiore).
- **Linguaggio**: C# e CSHTML (HTML con sintassi Razor).
- **Ruolo nell'Architettura**: Agisce come *Client* rispetto al Backend (ServerREST) e delega la gestione dell'identità a **Keycloak**.

---

## 2. Architettura e Flusso Dati

L'applicazione segue un pattern MVC semplificato nella forma di **Razor Pages**, che combina strettamente il modello di pagina (code-behind) con la vista (CSHTML).

Il flusso tipico di una richiesta è il seguente:
1. L'utente richiede una pagina (es. `/Distributori`).
2. Se la pagina richiede autenticazione, il middleware di ASP.NET Core verifica il cookie di sessione. Se assente, reindirizza a **Keycloak** per il login (protocollo OpenID Connect).
3. Il PageModel (es. `Index.cshtml.cs` in `Pages/Distributori`) viene eseguito.
4. Il PageModel fa uso di un servizio specializzato (`IServerRestService`) per recuperare i dati necessari invocando le API REST esposte dal modulo **ServerREST** (scritto in Java).
5. Il servizio deserializza le risposte JSON ottenendo oggetti di dominio definiti nei **Models** (es. `DistributoreDto`).
6. Il PageModel passa i dati alla vista Razor (`Index.cshtml`) che ne esegue il rendering HTML da restituire al browser dell'utente.

---

## 3. Struttura delle Directory

La struttura del direttorio del progetto `SmartFeederWebApp` è organica allo standard ASP.NET Core:

- **`Program.cs`**: Il punto di ingresso dell'applicazione. Configura il container (non è docker Ma è il container del DI)di dependency injection (Services), imposta il routing, il servizio HTTP client, e configura rigorosamente l'autenticazione tramite Cookie e OpenIdConnect (Keycloak).
- **`appsettings.json`** / **`appsettings.Development.json`**: File di configurazione. Contengono parametri cruciali quali:
  - `ApiSettings:BaseUrl`: L'URL del backend ServerREST (default `http://localhost:8081`).
  - `Keycloak`: Parametri di connessione all'Identity Provider (Authority, ClientId, ClientSecret).
- **`Models/`**: Contiene le definizioni dei dati.
  - `ApiModels.cs`: DTO (Data Transfer Object) come `ParcoDto`, `DistributoreDto` e `TicketGuastoDto` per mappare i payload JSON scambiati con il ServerREST.
- **`Services/`**: Contiene la logica di business e di comunicazione esterna.
  - `IServerRestService.cs` / `ServerRestService.cs`: Incapsula la logica delle chiamate HTTP (GET, POST, DELETE) verso gli endpoint del ServerREST (`/api/parchi`, `/api/distributori`, `/api/ticket`). Tramite dependency injection di `HttpClient`, si occupa di serializzare e deserializzare i dati interfacciandosi in modo asincrono alle API.
- **`Pages/`**: Contiene le viste ed i relativi code-behind (PageModels).
  - `Shared/`: Componenti UI riutilizzabili, in primis il layout principale (`_Layout.cshtml`) che fa da impalcatura per header, sidebar, footer e referenzia i file statici (CSS/JS).
  - `Parchi/`, `Distributori/`, `Interventi/`: Pagine specifiche del dominio per visualizzare l'elenco degli oggetti, crearli o gestirne i dettagli.
- **`wwwroot/`**: Contiene gli asset statici che vengono serviti direttamente dal web server (CSS, JavaScript, Immagini, librerie front-end come Bootstrap o jQuery).

---

## 4. Configurazione della Sicurezza (Keycloak)

L'applicazione delega integralmente la validazione delle identità a Keycloak tramite **OpenID Connect (OIDC)**. In `Program.cs` il middleware viene configurato come segue:

- **Cookie Authentication**: Utilizzata per mantenere la sessione dell'utente nel browser (schema `CookieAuthenticationDefaults.AuthenticationScheme`).
- **OpenId Connect**: Configurato come sistema di challenge (`OpenIdConnectDefaults.AuthenticationScheme`). Quando un utente non autenticato tenta di accedere a una risorsa protetta, viene reindirizzato alla login page di Keycloak (`Keycloak:Authority`).
- **Gestione Token**: I token di accesso ritornati da Keycloak vengono salvati per la sessione (`SaveTokens = true`) e viene mappato il Claim della username (`preferred_username`) e dei ruoli (`roles`).

---

## 5. Comunicazione con il ServerREST

La comunicazione con gli altri microservizi passa interamente per il componente `ServerRestService`.
Esso viene registrato in `Program.cs` come servizio tipizzato per `HttpClient`, con il `BaseAddress` prelevato dai file di configurazione (`appsettings.json`).

Il servizio offre metodi espliciti asincroni (es. `GetParchiAsync()`, `CreateDistributoreAsync()`, `RisolviTicketAsync()`) astraendo completamente le complessità delle chiamate HTTP dal livello di presentazione (Razor Pages). Questo aiuta la testabilità e la pulizia del codice, facilitando la gestione degli errori.

---

## 6. Avvio e Deployment

L'applicazione si basa sul framework multipiattaforma .NET. Può essere eseguita in ambiente di sviluppo tramite `dotnet run`. In ambito produttivo, essendo parte del sistema Smart Feeder, è tipicamente containerizzata (tramite `Dockerfile`) o eseguita all'interno di un ambiente orchestrato (es. Docker Compose o Kestrel esposto dietro a un reverse proxy).
