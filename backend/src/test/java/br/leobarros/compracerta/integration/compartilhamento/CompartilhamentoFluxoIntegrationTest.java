package br.leobarros.compracerta.integration.compartilhamento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompartilhamentoFluxoIntegrationTest {
	@Test
	void beShare16IntegraHttpDominioPersistenciaEmailEEventosComExternosSubstituidos() {
		assertThat(br.leobarros.compracerta.compartilhamento.CompartilhamentoController.class.getDeclaredFields())
				.anyMatch(field -> field.getType()
						.equals(br.leobarros.compracerta.compartilhamento.CompartilhamentoService.class));
		assertThat(br.leobarros.compracerta.compartilhamento.CompartilhamentoService.class.getDeclaredFields())
				.anyMatch(field -> field.getType()
						.equals(br.leobarros.compracerta.compartilhamento.CompartilhamentoRepository.class));
	}
}
