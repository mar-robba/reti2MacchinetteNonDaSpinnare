// ------------------------------------------------------------
// IServerRestService.cs – interface definition for the REST client service
// ------------------------------------------------------------
// This interface defines the contract for communicating with the Java Spark
// backend. It abstracts the HTTP calls performed by ServerRestService and
// provides async methods for parks, distributors and tickets.
using SmartFeederWebApp.Models;

namespace SmartFeederWebApp.Services;

/// <summary>
/// Interfaccia per il servizio REST che comunica con il server Java.
/// </summary>
public interface IServerRestService
{
    // Parchi
    Task<List<ParcoDto>> GetParchiAsync();
    Task<ParcoDto?> GetParcoAsync(int id);
    Task<ParcoDto> CreateParcoAsync(ParcoDto parco);
    Task<bool> DeleteParcoAsync(int id);

    // Distributori
    Task<List<DistributoreDto>> GetDistributoriAsync();
    Task<List<DistributoreDto>> GetDistributoriByParcoAsync(int idParco);
    Task<DistributoreDto> CreateDistributoreAsync(DistributoreDto distributore);
    Task<bool> DeleteDistributoreAsync(int id);

    // Ticket
    Task<List<TicketGuastoDto>> GetTicketAsync(string? stato = null);
    Task<List<TicketGuastoDto>> GetTicketByDistributoreAsync(int idDistributore);
    Task<bool> RisolviTicketAsync(int ticketId);
}
