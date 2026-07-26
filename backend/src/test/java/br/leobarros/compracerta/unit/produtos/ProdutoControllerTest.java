package br.leobarros.compracerta.produtos;

import java.util.List;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.produtos.ProdutoDtos.Collection;
import br.leobarros.compracerta.produtos.ProdutoDtos.Input;
import br.leobarros.compracerta.produtos.ProdutoDtos.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.produtos.ProdutoTestFixtures.ACCOUNT;
import static br.leobarros.compracerta.produtos.ProdutoTestFixtures.CATEGORY_ID;
import static br.leobarros.compracerta.produtos.ProdutoTestFixtures.PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoControllerTest {
	@Mock ProdutoService service;
	@Mock SessaoService sessions;
	private ProdutoController controller;

	@BeforeEach
	void setUp() {
		controller = new ProdutoController(service, sessions);
		when(sessions.obterContaAutenticada("token")).thenReturn(ACCOUNT);
	}

	@Test
	void beProd01ControllerValidaFiltrosEDevolvePagina() {
		var body = new Collection(List.of(), new PageInfo(null, false));
		when(service.list(ACCOUNT, "arroz", CATEGORY_ID, "ACTIVE", null, 10)).thenReturn(body);
		assertThat(controller.list("token", "arroz", CATEGORY_ID, "ACTIVE", null, 10).getBody()).isSameAs(body);
	}

	@Test
	void beProd05PostExigeSegurancaERetornaMetadados() {
		var input = new Input("Arroz", CATEGORY_ID, "UNIT");
		when(service.create(ACCOUNT, input, "key")).thenReturn(ProdutoTestFixtures.product("Arroz", true, 1));
		var response = controller.create("token", "csrf", "key", input);
		verify(sessions).validarCsrf("token", "csrf");
		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getHeaders().getLocation().toString()).endsWith("/" + PRODUCT_ID);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
	}
}
