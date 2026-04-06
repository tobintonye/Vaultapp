package Vaultproject.Vaultapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableJpaAuditing
public class VaultappApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultappApplication.class, args);
	}

}
