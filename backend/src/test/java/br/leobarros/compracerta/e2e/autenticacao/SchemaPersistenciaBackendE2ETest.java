package br.leobarros.compracerta.e2e.autenticacao;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaPersistenciaBackendE2ETest extends AutenticacaoBackendE2ESupport {

  @Autowired
  private Flyway flyway;

  @Test
  void beE2e01IniciaAplicacaoComPostgresqlEMigrationsAplicadas() {
    assertThat(flyway.info().applied())
      .as("Ao menos uma migration Flyway deve ser aplicada ao PostgreSQL vazio")
      .isNotEmpty();
  }

	@Test
	void beE2e16SchemaImpoeRestricoesCompativeisComODominio() {
		super.beE2e16SchemaImpoeRestricoesCompativeisComODominio();
	}

	@Test
	void beE2e17DadosPermanecemDisponiveisEmNovoContexto() throws Exception {
		super.beE2e17DadosPermanecemDisponiveisEmNovoContexto();
	}
}
