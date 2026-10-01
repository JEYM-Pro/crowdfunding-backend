package co.udea.crowdfunding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class CrowdfundingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrowdfundingApplication.class, args);
	}

}
