package br.leobarros.compracerta.integration.execucaocompra;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.itens.ItemController;
import br.leobarros.compracerta.itens.ItemDtos;
import br.leobarros.compracerta.itens.ItemRepository;
import br.leobarros.compracerta.itens.ItemService;
import br.leobarros.compracerta.listas.ListaDtos;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecucaoCompraFluxoIntegrationTest {
	@Test
	void beShop13IntegraHttpDominioPersistenciaEEventosComExternosSubstituidos() throws Exception {
		var account = new Conta(UUID.randomUUID(), "Ana", "ana@example.com", "hash", true);
		var listId = UUID.randomUUID();
		var itemId = UUID.randomUUID();
		var productId = UUID.randomUUID();
		var categoryId = UUID.randomUUID();
		var now = Instant.parse("2026-01-01T00:00:00Z");
		var item = new ItemDtos.ListItem(
				itemId, new ItemDtos.ProductSnapshot(productId, "Arroz"),
				new ItemDtos.CategorySnapshot(categoryId, "Mercearia", "🛍️"), "1", "UNIT", null,
				false, null, null, new ListaDtos.UserReference(account.getId(), "Ana"), now, now, 1);
		var checked = new ItemDtos.ListItem(
				itemId, item.product(), item.category(), "1", "UNIT", null, true, now,
				item.createdBy(), item.createdBy(), now, now, 2);
		var repository = mock(ItemRepository.class);
		var idempotency = mock(IdempotenciaRepository.class);
		var sessions = mock(SessaoService.class);
		when(sessions.obterContaAutenticada("token")).thenReturn(account);
		when(repository.list(listId, account.getId()))
				.thenReturn(Optional.of(new ItemRepository.ListState(listId, "ACTIVE", 1)));
		when(repository.find(listId, itemId)).thenReturn(Optional.of(item), Optional.of(checked));
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.touchList(any(), any())).thenReturn(2L);
		when(repository.summary(listId)).thenReturn(new ListaDtos.ListSummary(1, 1, 0, 100));
		when(repository.listVersion(listId)).thenReturn(2L);
		var service = new ItemService(Clock.fixed(now, java.time.ZoneOffset.UTC), repository,
				idempotency, mock(ListaEventService.class));
		var response = new ItemController(service, sessions, mock(ListaEventService.class)).check(
				"token", "csrf", "key", "\"1\"", listId, itemId, new ItemDtos.CheckInput(true));
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getETag()).isEqualTo("\"2\"");
		assertThat(response.getBody().listSummary().percentage()).isEqualTo(100);
	}
}
