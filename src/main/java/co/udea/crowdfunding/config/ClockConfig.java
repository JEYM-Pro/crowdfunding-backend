package co.udea.crowdfunding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable: permite probar reglas dependientes de la fecha sin depender de la hora real. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
