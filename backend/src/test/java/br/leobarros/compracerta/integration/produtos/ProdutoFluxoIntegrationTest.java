package br.leobarros.compracerta.integration.produtos;

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
import br.leobarros.compracerta.produtos.ProdutoDtos.CategoryReference;
import br.leobarros.compracerta.produtos.ProdutoDtos.Input;
import br.leobarros.compracerta.produtos.ProdutoDtos.Product;
import br.leobarros.compracerta.produtos.ProdutoRepository;
import br.leobarros.compracerta.produtos.ProdutoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdutoFluxoIntegrationTest {
	@Test
	void beProd16IntegraServiceRepositoryStubEmCrudFiltrosIsolamentoEConflito() {
		var account = new Conta(UUID.randomUUID(), "Ana", "ana@example.com", "hash", true);
		var other = new Conta(UUID.randomUUID(), "Bia", "bia@example.com", "hash", true);
		var categoryId = UUID.randomUUID();
		var repository = new StubRepository(categoryId);
		var idempotency = mock(IdempotenciaRepository.class);
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		var service = new ProdutoService(
				Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), repository, idempotency);
		var created = service.create(account, new Input("Arroz", categoryId, "UNIT"), "key");
		assertThat(service.list(account, "Arroz", categoryId, "ACTIVE", null, 10).items()).hasSize(1);
		var updated = service.update(account, created.id(), new Input("Feijão", null, null), 1);
		assertThat(updated.name()).isEqualTo("Feijão");
		assertThatThrownBy(() -> service.update(account, created.id(), new Input("Frios", null, null), 1))
				.isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.get(other, created.id())).isInstanceOf(ApiException.class);
		service.deactivate(account, created.id(), 2);
		assertThat(repository.snapshotsChanged).isFalse();
	}

	private static final class StubRepository extends ProdutoRepository {
		private final UUID categoryId;
		private final Map<UUID, Stored> products = new HashMap<>();
		private boolean snapshotsChanged;
		StubRepository(UUID categoryId) {
			super(null);
			this.categoryId = categoryId;
		}
		@Override public Optional<CategoryReference> category(UUID id, UUID accountId) {
			return id.equals(categoryId)
					? Optional.of(new CategoryReference(id, "Mercearia", "🛍️", true)) : Optional.empty();
		}
		@Override public boolean nameExists(UUID accountId, String name, UUID ignored) {
			return products.values().stream().anyMatch(value -> value.accountId.equals(accountId)
					&& value.active && !value.id.equals(ignored) && value.name.toLowerCase().equals(name));
		}
		@Override public void create(UUID id, UUID accountId, Input input, CategoryReference category, Instant now) {
			products.put(id, new Stored(id, accountId, input.name(), input.defaultUnit(), category, now));
		}
		@Override public Optional<Product> find(UUID id, UUID accountId) {
			return Optional.ofNullable(products.get(id)).filter(value -> value.accountId.equals(accountId))
					.map(Stored::product);
		}
		@Override public List<Product> list(
				UUID accountId, String search, UUID category, String status, int limit, int offset) {
			return products.values().stream().filter(value -> value.accountId.equals(accountId))
					.filter(value -> !"ACTIVE".equals(status) || value.active).map(Stored::product).toList();
		}
		@Override public int update(
				UUID id, UUID accountId, Input input, CategoryReference category, Instant now, long version) {
			var value = products.get(id);
			if (value == null || value.version != version) return 0;
			value.name = input.name(); value.unit = input.defaultUnit(); value.category = category; value.version++;
			return 1;
		}
		@Override public int deactivate(UUID id, UUID accountId, long version) {
			var value = products.get(id);
			if (value == null || value.version != version) return 0;
			value.active = false; value.version++; return 1;
		}
	}
	private static final class Stored {
		final UUID id; final UUID accountId; final Instant now;
		String name; String unit; CategoryReference category; boolean active = true; long version = 1;
		Stored(UUID id, UUID accountId, String name, String unit, CategoryReference category, Instant now) {
			this.id = id; this.accountId = accountId; this.name = name; this.unit = unit;
			this.category = category; this.now = now;
		}
		Product product() {
			return new Product(id, name, category, unit, active, now, now, version);
		}
	}
}
