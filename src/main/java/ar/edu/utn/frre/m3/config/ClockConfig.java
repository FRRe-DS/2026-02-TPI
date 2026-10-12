package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.port.out.ClockPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * Configuración del reloj del sistema para la capa de aplicación.
 */
@Configuration
public class ClockConfig {

    @Bean
    public ClockPort clockPort() {
        return Instant::now;
    }
}
