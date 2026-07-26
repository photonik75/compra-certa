package br.leobarros.compracerta.integration.itens;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.itens.ItemDtos;
import br.leobarros.compracerta.itens.ItemDtos.ListItem;
import br.leobarros.compracerta.itens.ItemRepository;
import br.leobarros.compracerta.itens.ItemService;
import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemFluxoIntegrationTest {
	@Test
	void beItem16IntegraServiceRepositoryStubComSnapshotResumoIsolamentoEConflito() {
		var account = new Conta(UUID.randomUUID(), "Ana", "ana@example.com", "hash", true);
		var other = new Conta(UUID.randomUUID(), "Bia", "bia@example.com", "hash", true);
		var listId = UUID.randomUUID();
		var productId = UUID.randomUUID();
		var categoryId = UUID.randomUUID();
		var repository = new StubRepository(account.getId(), listId, productId, categoryId);
		var idempotency = mock(IdempotenciaRepository.class);
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		var service = new ItemService(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
				repository, idempotency, mock(ListaEventService.class));
		var input = new ItemDtos.Input(productId, "2", "UNIT", categoryId, null, null, null);
		var created = service.create(account, listId, input, "create").item();
		assertThat(created.product().name()).isEqualTo("Arroz");
		assertThat(created.category().name()).isEqualTo("Mercearia");
		assertThat(service.list(account, listId, null, 30).listSummary().total()).isEqualTo(1);
		assertThatThrownBy(() -> service.get(other, listId, created.id())).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.update(
				account, listId, created.id(), input, 0, "update")).isInstanceOf(ApiException.class);
		service.delete(account, listId, created.id(), 1, "delete");
		assertThat(service.list(account, listId, null, 30).items()).isEmpty();
	}

	private static final class StubRepository extends ItemRepository {
		private final UUID owner; private final UUID listId; private final UUID productId; private final UUID categoryId;
		private final Map<UUID, ListItem> items = new HashMap<>(); private long listVersion = 1;
		StubRepository(UUID owner, UUID listId, UUID productId, UUID categoryId) {
			super(null); this.owner = owner; this.listId = listId; this.productId = productId; this.categoryId = categoryId;
		}
		@Override public Optional<ListState> list(UUID id, UUID account) {
			return id.equals(listId) && account.equals(owner)
					? Optional.of(new ListState(id, "ACTIVE", listVersion)) : Optional.empty();
		}
		@Override public Optional<ProductData> product(UUID product, UUID category, UUID account) {
			return product.equals(productId) && category.equals(categoryId) && account.equals(owner)
					? Optional.of(new ProductData(productId, "Arroz", categoryId, "Mercearia", "🛍️"))
					: Optional.empty();
		}
		@Override public Optional<ListItem> duplicate(UUID id, String name, UUID ignored) { return Optional.empty(); }
		@Override public void create(UUID id, UUID list, UUID actor, InputData data, Instant now) {
			items.put(id, new ListItem(id, new ItemDtos.ProductSnapshot(productId, data.product().name()),
					new ItemDtos.CategorySnapshot(categoryId, data.product().categoryName(),
							data.product().categoryIcon()),
					data.quantity().toPlainString(), data.unit(), data.notes(), false, null, null,
					new br.leobarros.compracerta.listas.ListaDtos.UserReference(actor, "Ana"), now, now, 1));
		}
		@Override public Optional<ListItem> find(UUID list, UUID item) { return Optional.ofNullable(items.get(item)); }
		@Override public List<ListItem> listItems(UUID list, int limit, int offset) {
			return items.values().stream().toList();
		}
		@Override public int delete(UUID id, long version) { return items.remove(id) == null ? 0 : 1; }
		@Override public long touchList(UUID list, Instant now) { return ++listVersion; }
		@Override public long listVersion(UUID list) { return listVersion; }
		@Override public ListSummary summary(UUID list) {
			return new ListSummary(items.size(), 0, items.size(), 0);
		}
	}
}
