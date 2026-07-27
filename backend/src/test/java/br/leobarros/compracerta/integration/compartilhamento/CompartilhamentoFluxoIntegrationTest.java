package br.leobarros.compracerta.integration.compartilhamento;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoController;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoRepository;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoService;
import br.leobarros.compracerta.compartilhamento.EntregaConvite;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompartilhamentoFluxoIntegrationTest {
	@Test
	void beShare16IntegraHttpDominioPersistenciaEmailEEventosComExternosSubstituidos() {
		var account = new Conta(UUID.randomUUID(), "Ana", "ana@example.com", "hash", true);
		var listId = UUID.randomUUID();
		var repository = mock(CompartilhamentoRepository.class);
		var sessions = mock(SessaoService.class);
		when(sessions.obterContaAutenticada("token")).thenReturn(account);
		when(repository.list(listId, account.getId())).thenReturn(Optional.of(
				new CompartilhamentoRepository.ListState(
						listId, "Compras", "ACTIVE", account.getId(), "Ana", "ana@example.com", 1)));
		when(repository.members(listId)).thenReturn(List.of());
		when(repository.invitations(listId)).thenReturn(List.of());
		var service = new CompartilhamentoService(
				Clock.systemUTC(), repository, mock(IdempotenciaRepository.class),
				mock(GeradorIdentificadorService.class), mock(EntregaConvite.class),
				mock(ListaEventService.class));
		var response = new CompartilhamentoController(service, sessions).access("token", listId);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
		assertThat(response.getBody().owner().role()).isEqualTo("OWNER");
	}
}
