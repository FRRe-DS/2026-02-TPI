package ar.edu.utn.frre.m3.adapter.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Captura el ingreso antes de autenticación, validación y ejecución del controlador. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestTimingFilter extends OncePerRequestFilter {
    static final String START_NANOS = RequestTimingFilter.class.getName() + ".startNanos";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        request.setAttribute(START_NANOS, System.nanoTime());
        chain.doFilter(request, response);
    }
}
