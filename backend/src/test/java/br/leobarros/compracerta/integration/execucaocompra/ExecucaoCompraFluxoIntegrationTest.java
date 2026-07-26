package br.leobarros.compracerta.integration.execucaocompra;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecucaoCompraFluxoIntegrationTest {
	@Test
	void beShop13IntegraHttpDominioPersistenciaEEventosComExternosSubstituidos() throws Exception {
		var controller = br.leobarros.compracerta.itens.ItemController.class;
		var service = br.leobarros.compracerta.itens.ItemService.class;
		var repository = br.leobarros.compracerta.itens.ItemRepository.class;
		assertThat(controller.getDeclaredFields()).anyMatch(field -> field.getType().equals(service));
		assertThat(service.getDeclaredFields()).anyMatch(field -> field.getType().equals(repository));
		assertThat(service.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(br.leobarros.compracerta.eventos.ListaEventService.class));
	}
}
