package co.udea.crowdfunding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMethodSecurity
@EnableScheduling
public class CrowdfundingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrowdfundingApplication.class, args);
	}

}
