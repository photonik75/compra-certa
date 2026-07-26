package br.leobarros.compracerta.listas;

import java.util.List;
import java.util.Optional;

import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.listas.ListaDtos.CreateListRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static br.leobarros.compracerta.listas.ListaTestFixtures.CONTA;
import static br.leobarros.compracerta.listas.ListaTestFixtures.LISTA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ListaControllerTest {

	@Mock
	private ListaService service;
	@Mock
	private SessaoService sessions;
	private ListaController controller;

	@BeforeEach
	void setUp() {
		controller = new ListaController(service, sessions);
		lenient().when(sessions.obterContaAutenticada("token")).thenReturn(CONTA);
	}

	@Test
	void beLis01GetValidaSessaoQueryERetornaColecaoComResumo() {
		var collection = ListaTestFixtures.colecao(List.of(), false);
		when(service.listar(CONTA, "ACTIVE", "feira", null, 10)).thenReturn(collection);
		var response = controller.listar("token", "ACTIVE", "feira", null, 10);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isSameAs(collection);
		verify(sessions).obterContaAutenticada("token");
	}

	@Test
	void beLis05PostExigeCsrfEChaveERetornaLocationETag() {
		var request = new CreateListRequest("Mercado", null);
		var detail = ListaTestFixtures.detalhe("Mercado", null, "ACTIVE", "OWNER", 1);
		when(service.criar(CONTA, request, "key")).thenReturn(detail);
		var response = controller.criar("token", "csrf", "key", request);
		verify(sessions).validarCsrf("token", "csrf");
		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getHeaders().getLocation().toString()).endsWith("/" + LISTA_ID);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
	}

	@Test
	void beLis09GetDetalheRetornaEtagENaoEncontradoUsaErroNormativo() {
		var detail = ListaTestFixtures.detalhe("Mercado", null, "ACTIVE", "OWNER", 4);
		when(service.buscar(CONTA, LISTA_ID)).thenReturn(detail);
		var response = controller.buscar("token", LISTA_ID);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"4\"");
		when(service.buscar(CONTA, LISTA_ID)).thenThrow(new ListaExceptions.NaoEncontrada());
		assertThatThrownBy(() -> controller.buscar("token", LISTA_ID))
				.isInstanceOf(ListaExceptions.NaoEncontrada.class);
	}

	@Test
	void beLis10PatchAceitaSomenteCamposPermitidosEExigeIfMatch() throws Exception {
		var mapper = new ObjectMapper();
		var detail = ListaTestFixtures.detalhe("Feira", null, "ACTIVE", "OWNER", 2);
		when(service.atualizar(CONTA, LISTA_ID, 1L, true, "Feira", true, null))
				.thenReturn(detail);
		var response = controller.atualizar(
				"token", "csrf", "\"1\"", LISTA_ID,
				mapper.readTree("{\"name\":\"Feira\",\"description\":null}"));
		assertThat(response.getHeaders().getETag()).isEqualTo("\"2\"");
		assertThatThrownBy(() -> controller.atualizar(
				"token", "csrf", "\"1\"", LISTA_ID, mapper.readTree("{}")))
				.isInstanceOf(ListaExceptions.Validacao.class);
		assertThatThrownBy(() -> controller.atualizar(
				"token", "csrf", "\"1\"", LISTA_ID, mapper.readTree("{\"status\":\"COMPLETED\"}")))
				.isInstanceOf(ListaExceptions.Validacao.class);
	}

	@Test
	void beLis14MapeiaTodosOsErrosNormativos() {
		var handler = new ListaExceptionHandler(new ApiErrorResponseService());
		assertThat(handler.validacao(new ListaExceptions.Validacao("name", "inválido"))
				.getBody().code()).isEqualTo("VALIDATION_ERROR");
		assertThat(handler.nomeEmUso().getBody().code()).isEqualTo("LIST_NAME_ALREADY_IN_USE");
		assertThat(handler.proibida().getBody().code()).isEqualTo("FORBIDDEN");
		assertThat(handler.concluida().getBody().code()).isEqualTo("LIST_COMPLETED");
		assertThat(handler.conflito(new ListaExceptions.Conflito(3)).getHeaders().getETag())
				.isEqualTo("\"3\"");
		assertThat(handler.naoEncontrada().getBody().code()).isEqualTo("NOT_FOUND");
	}
}
