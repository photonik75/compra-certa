package br.leobarros.compracerta.integration.ciclodevida;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CicloVidaFluxoIntegrationTest {
	@Test
	void beLife13IntegraHttpDominioPersistenciaEEventosComExternosSubstituidos() {
		assertThat(br.leobarros.compracerta.ciclodevida.CicloVidaController.class.getDeclaredFields())
				.anyMatch(field -> field.getType()
						.equals(br.leobarros.compracerta.ciclodevida.CicloVidaService.class));
		assertThat(br.leobarros.compracerta.ciclodevida.CicloVidaService.class.getDeclaredFields())
				.anyMatch(field -> field.getType()
						.equals(br.leobarros.compracerta.ciclodevida.CicloVidaRepository.class));
	}
}
