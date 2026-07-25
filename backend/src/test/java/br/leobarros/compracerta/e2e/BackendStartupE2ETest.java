package br.leobarros.compracerta.e2e;

import br.leobarros.compracerta.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackendStartupE2ETest {

	@Autowired
	private Flyway flyway;

	@Test
	void beE2e01IniciaAplicacaoComPostgresqlEMigrationsAplicadas() {
		assertThat(flyway.info().applied())
				.as("Ao menos uma migration Flyway deve ser aplicada ao PostgreSQL vazio")
				.isNotEmpty();
	}
}
