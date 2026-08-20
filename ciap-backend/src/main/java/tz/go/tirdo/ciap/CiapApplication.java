package tz.go.tirdo.ciap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Consultancy & Industrial Advisory Portal (CIAP) — TIRDO.
 * Revenue-generating industrial consultancy management platform.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class CiapApplication {

    public static void main(String[] args) {
        SpringApplication.run(CiapApplication.class, args);
    }
}
