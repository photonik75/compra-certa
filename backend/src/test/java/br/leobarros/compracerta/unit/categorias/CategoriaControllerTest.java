package br.leobarros.compracerta.categorias;

import java.util.List;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.categorias.CategoriaDtos.Collection;
import br.leobarros.compracerta.categorias.CategoriaDtos.Input;
import br.leobarros.compracerta.categorias.CategoriaDtos.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CATEGORIA_ID;
import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CONTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

	@Mock
	private CategoriaService service;
	@Mock
	private SessaoService sessions;
	private CategoriaController controller;

	@BeforeEach
	void setUp() {
		controller = new CategoriaController(service, sessions);
		when(sessions.obterContaAutenticada("token")).thenReturn(CONTA);
	}

	@Test
	void beCat02GetValidaSessaoQueryERetornaPagina200() {
		var collection = new Collection(List.of(), new PageInfo(null, false));
		when(service.list(CONTA, "feira", null, 10)).thenReturn(collection);
		var response = controller.list("token", "feira", null, 10);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isSameAs(collection);
	}

	@Test
	void beCat05PostExigeCsrfEChaveERetorna201LocationEtag() {
		var input = new Input("Padaria", "🍞");
		var category = CategoriaTestFixtures.categoria("Padaria", "🍞", 0, 1);
		when(service.create(CONTA, input, "key")).thenReturn(category);
		var response = controller.create("token", "csrf", "key", input);
		verify(sessions).validarCsrf("token", "csrf");
		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getHeaders().getLocation().toString()).endsWith("/" + CATEGORIA_ID);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
	}
}
