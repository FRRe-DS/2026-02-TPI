package ar.edu.utn.frre.m3.adapter.security;
import ar.edu.utn.frre.m3.application.usecase.DriverAccessService;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.web.filter.OncePerRequestFilter;
public class DriverAuthorizationFilter extends OncePerRequestFilter {
    private final DriverAccessService access; private final CurrentPrincipalPort principal; private final DevelopmentMode mode;
    private final Set<String> serviceReadRoles;
    private static final Pattern DRIVER=Pattern.compile("^/(?:api/v1|ui)/(?:operator/)?drivers/([0-9a-fA-F-]{36})(?:/.*)?$");
    /** Lecturas del contrato M3 2.0.0 que hace M4: estado, ubicación y vehículo de un conductor. */
    private static final Pattern CONTRACT_READ=Pattern.compile("^/api/v1/conductores/([0-9a-fA-F-]{36})/(?:estado|ubicacion|vehiculos/[0-9a-fA-F-]{36})$");
    /** Rol "servicio" del contrato y sus equivalentes en el realm compartido (M4, M5, M6). */
    public static final String DEFAULT_SERVICE_READ_ROLES="SERVICIO,SCOPE_m3:drivers:read,ubicacion-be,solicitud-be,viajes-be";
    public DriverAuthorizationFilter(DriverAccessService access,CurrentPrincipalPort principal,DevelopmentMode mode) { this(access,principal,mode,DEFAULT_SERVICE_READ_ROLES); }
    public DriverAuthorizationFilter(DriverAccessService access,CurrentPrincipalPort principal,DevelopmentMode mode,String serviceReadRoles) {
        this.access=access;this.principal=principal;this.mode=mode;
        this.serviceReadRoles=Arrays.stream(serviceReadRoles==null?new String[0]:serviceReadRoles.split(",")).map(String::trim).filter(r->!r.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String path=request.getRequestURI(); request.setAttribute("m3.development",mode.enabled());
        if(!path.startsWith("/api/") && !path.startsWith("/ui")) { chain.doFilter(request,response);return; }
        // Development-only fixtures may intentionally exercise legacy controllers without an identity.
        if(mode.enabled() && !principal.isAuthenticated()) { chain.doFilter(request,response);return; }
        try {
            if(path.startsWith("/api/v1/integration/")) {
                String scope;
                if(path.endsWith("/m6/trips/completed")) scope="SCOPE_m6:trip:completed:write";
                else if(path.endsWith("/m5/offers")) scope="SCOPE_m5:offers:write";
                else if(path.endsWith("/summary")) scope="SCOPE_m3:drivers:read";
                else scope="SCOPE_m3:eligibility:read";
                if(!principal.hasRole(scope) && !access.operator()) throw new DriverAccessDeniedException("Scope de integración insuficiente.");
            } else if(CONTRACT_READ.matcher(path).matches() && "GET".equals(request.getMethod()) && serviceReadRoles.stream().anyMatch(principal::hasRole)) {
                // Identidad de servicio (M4/M5/M6): puede leer el estado de cualquier conductor; el 404 lo decide el controlador.
            } else {
                if(!mode.enabled() && !(principal.hasRole("DRIVER") || principal.hasRole("CONDUCTOR") || access.operator()))
                    throw new DriverAccessDeniedException("La operación requiere un rol humano autorizado.");
                if(path.startsWith("/ui/operator/") || (!"GET".equals(request.getMethod()) && (path.endsWith("/reviews") || path.endsWith("/review") || path.endsWith("/qualification-decisions")))) access.requireOperator();
                var match=DRIVER.matcher(path); if(match.matches()) access.driver(UUID.fromString(match.group(1)));
                var contract=CONTRACT_READ.matcher(path); if(contract.matches()) access.driver(UUID.fromString(contract.group(1)));
                if(path.startsWith("/ui/") && request.getParameter("driverId")!=null) access.driver(UUID.fromString(request.getParameter("driverId")));
            }
            request.setAttribute("m3.actor",principal.getSubject().orElse("local"));
            chain.doFilter(request,response);
        } catch(DriverAccessDeniedException ex) { problem(response,403,"Acceso denegado","ACCESO_PROHIBIDO"); }
          catch(DriverNotFoundException ex) { problem(response,404,"Conductor no encontrado","NO_ENCONTRADO"); }
          catch(IllegalArgumentException ex) { problem(response,400,"Identificador inválido","SOLICITUD_INVALIDA"); }
    }
    /** problem+json (RFC 9457) con los miembros codigo y mensaje del esquema Error del contrato M3 2.0.0. */
    private void problem(HttpServletResponse response,int status,String title,String code) throws IOException {
        response.setStatus(status);response.setContentType("application/problem+json");response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":"+status+",\"title\":\""+title+"\",\"codigo\":\""+code+"\",\"mensaje\":\""+title+".\"}");
    }
}
