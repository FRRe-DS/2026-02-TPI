package ar.edu.utn.frre.m3.hu03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Small standalone entry point for the HU-03 flow, without starting M3's database adapters. */
@SpringBootApplication(scanBasePackages = "ar.edu.utn.frre.m3.hu03")
public class Hu03Application {
    public static void main(String[] args) {
        SpringApplication.run(Hu03Application.class, args);
    }
}
