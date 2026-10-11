package ar.edu.utn.frre.m3.adapter.rest;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoints according to OpenAPI contract (/health/live, /health/ready).
 */
@RestController
public class HealthRestController {

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, String>> checkLiveness() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, String>> checkReadiness() {
        return ResponseEntity.ok(Map.of("status", "ready"));
    }
}
