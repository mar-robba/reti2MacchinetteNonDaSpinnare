namespace SmartFeederWebApp.Models;


/*Marco: I Modelli Orm ma in C#

- Un DTO (Data Transfer Object) è un pattern architetturale informatico utilizzato
 per trasferire dati tra sottosistemi, client e server o diversi livelli di
  un'applicazione

  
*/
/*
 *Models/: Contiene le definizioni dei dati.

    ApiModels.cs: DTO (Data Transfer Object) come ParcoDto, DistributoreDto e TicketGuastoDto per mappare i payload JSON scambiati con il ServerREST.


 * */


/// <summary>
/// DTO per Parco (luogo di installazione).
/// </summary>
public class ParcoDto
{
        //MARCO : come una classe dove vengono definiti degli attributi che però possiedono delle graffe come se fossero delle struct nel c  
    public int Id { get; set; }
    public string Nome { get; set; } = "";
    public string Indirizzo { get; set; } = "";
    public string Citta { get; set; } = "";
}

/// <summary>
/// DTO per Distributore (dispositivo IoT).
/// </summary>
public class DistributoreDto
{
    public int Id { get; set; }
    public int IdParco { get; set; }
    public bool Guasta { get; set; }
    public bool Online { get; set; }
    public string? UltimoContatto { get; set; }
    public string? NomeParco { get; set; }
}

/// <summary>
/// DTO per Ticket Guasto (richiesta di manutenzione).
/// </summary>
public class TicketGuastoDto
{
    public int Id { get; set; }
    public string TipoGuasto { get; set; } = "";
    public int IdDistributore { get; set; }
    public string? TimestampRichiesta { get; set; }
    public string Stato { get; set; } = "aperta";
    public string? NomeParco { get; set; }
}
