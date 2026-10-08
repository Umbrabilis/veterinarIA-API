package tech.veterinaria_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import tech.veterinaria_api.testing.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
