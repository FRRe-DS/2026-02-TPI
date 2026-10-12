package ar.edu.utn.frre.m3.application.usecase;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import java.util.UUID;
public class DriverAccessService {
    private final DriverRepository drivers;
    private final CurrentPrincipalPort principal;
    public DriverAccessService(DriverRepository drivers, CurrentPrincipalPort principal) { this.drivers=drivers; this.principal=principal; }
    public String actor() { return principal.getSubject().orElseThrow(() -> new DriverAccessDeniedException("Se requiere una identidad autenticada.")); }
    public boolean operator() { return principal.hasRole("OPERATOR") || principal.hasRole("OPERADOR") || principal.hasRole("ADMIN") || principal.hasRole("ADMINISTRADOR"); }
    public void requireOperator() { actor(); if (!operator()) throw new DriverAccessDeniedException("Se requiere rol de operador o administrador."); }
    public Driver driver(UUID id) {
        String subject=actor();
        Driver d=drivers.findById(id).orElseThrow(() -> new DriverNotFoundException(id));
        if (!operator() && !d.isOwnedBy(subject)) throw new DriverAccessDeniedException("No tenés permiso para acceder a este conductor.");
        return d;
    }
    public void own(UUID id) {
        Driver d=driver(id);
        if(!d.isOwnedBy(actor())) throw new DriverAccessDeniedException("La operación corresponde al propio conductor.");
    }
}
