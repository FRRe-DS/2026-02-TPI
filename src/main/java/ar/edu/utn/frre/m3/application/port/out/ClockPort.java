package ar.edu.utn.frre.m3.application.port.out;

import java.time.Instant;

/**
 * Puerto de salida para acceso al reloj del sistema.
 * Permite controlar el tiempo en pruebas de verificación de vigencias y marcas temporales.
 */
public interface ClockPort {

    Instant now();
}
