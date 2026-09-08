package tech.veterinaria_api;

import org.springframework.boot.SpringApplication;

public class TestVeterinariaApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(VeterinariaApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
