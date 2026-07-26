package br.leobarros.compracerta.itens;

import java.util.List;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.itens.ItemDtos.Collection;
import br.leobarros.compracerta.itens.ItemDtos.Mutation;
import br.leobarros.compracerta.itens.ItemDtos.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.itens.ItemTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
	@Mock ItemService service;
	@Mock SessaoService sessions;
	@Mock ListaEventService events;
	private ItemController controller;

	@BeforeEach
	void setUp() {
		controller = new ItemController(service, sessions, events);
		when(sessions.obterContaAutenticada("token")).thenReturn(ACCOUNT);
	}

	@Test
	void beItem01GetValidaAcessoPaginacaoEOrdem() {
		var body = new Collection(List.of(), new PageInfo(null, false), SUMMARY, 1);
		when(service.list(ACCOUNT, LIST_ID, null, 30)).thenReturn(body);
		assertThat(controller.list("token", LIST_ID, null, 30).getBody()).isSameAs(body);
	}

	@Test
	void beItem03PostExigeSegurancaSchemaERetorna201() {
		var input = input("1", "UNIT", null);
		var item = item(ITEM_ID, "1", "UNIT", 1);
		when(service.create(ACCOUNT, LIST_ID, input, "key"))
				.thenReturn(new Mutation("CREATED", item, null, SUMMARY, 2));
		var response = controller.create("token", "csrf", "key", LIST_ID, input);
		verify(sessions).validarCsrf("token", "csrf");
		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
	}
}
