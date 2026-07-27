package br.leobarros.compracerta.integration.ciclodevida;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.ciclodevida.CicloVidaController;
import br.leobarros.compracerta.ciclodevida.CicloVidaRepository;
import br.leobarros.compracerta.ciclodevida.CicloVidaService;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CicloVidaFluxoIntegrationTest {
	@Test
	void beLife13IntegraHttpDominioPersistenciaEEventosComExternosSubstituidos() {
		var account = new Conta(UUID.randomUUID(), "Ana", "ana@example.com", "hash", true);
		var listId = UUID.randomUUID();
		var repository = mock(CicloVidaRepository.class);
		var idempotency = mock(IdempotenciaRepository.class);
		var lists = mock(ListaService.class);
		var events = mock(ListaEventService.class);
		var sessions = mock(SessaoService.class);
		var detail = mock(ListDetail.class);
		when(sessions.obterContaAutenticada("token")).thenReturn(account);
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.find(listId, account.getId()))
				.thenReturn(Optional.of(new CicloVidaRepository.State(listId, account.getId(), "ACTIVE", 1)));
		when(repository.change(any(), any(), any(), any(Long.class))).thenReturn(1);
		when(lists.buscar(account, listId)).thenReturn(detail);
		when(detail.version()).thenReturn(2L);
		var service = new CicloVidaService(Clock.systemUTC(), repository, idempotency, lists, events);
		var controller = new CicloVidaController(service, sessions);
		var response = controller.change(
				"token", "csrf", "key", "\"1\"", listId,
				new CicloVidaController.ChangeStatus("COMPLETED"));
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"2\"");
	}
}
