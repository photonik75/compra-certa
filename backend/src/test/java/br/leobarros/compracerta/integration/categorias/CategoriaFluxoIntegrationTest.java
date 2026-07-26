package br.leobarros.compracerta.integration.categorias;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.categorias.CategoriaController;
import br.leobarros.compracerta.categorias.CategoriaDtos.Category;
import br.leobarros.compracerta.categorias.CategoriaDtos.Input;
import br.leobarros.compracerta.categorias.CategoriaRepository;
import br.leobarros.compracerta.categorias.CategoriaService;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoriaFluxoIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

	@Test
	void beCat16IntegraControllerServiceERepositoryStubNoFluxoCompleto() {
		var account = new Conta(UUID.randomUUID(), "Ana", "ana@example.com", "hash", true);
		var other = new Conta(UUID.randomUUID(), "Bia", "bia@example.com", "hash", true);
		var repository = new InMemoryCategoriaRepository();
		var idempotency = mock(IdempotenciaRepository.class);
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		var service = new CategoriaService(Clock.fixed(NOW, ZoneOffset.UTC), repository, idempotency);
		var sessions = mock(SessaoService.class);
		when(sessions.obterContaAutenticada("ana")).thenReturn(account);
		var controller = new CategoriaController(service, sessions);
		var created = controller.create("ana", "csrf", "key", new Input("Padaria", "🍞")).getBody();
		assertThat(created).isNotNull();
		assertThat(controller.list("ana", null, null, 30).getBody().items()).extracting(Category::name)
				.containsExactly("Padaria");
		var updated = controller.update(
				"ana", "csrf", "\"1\"", created.id(), new Input("Pães", null)).getBody();
		assertThat(updated.name()).isEqualTo("Pães");
		assertThat(repository.activeProductsUpdated).isTrue();
		assertThat(repository.itemSnapshotsUpdated).isFalse();
		assertThatThrownBy(() -> service.get(other, created.id()))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("NOT_FOUND"));
		assertThatThrownBy(() -> controller.update(
				"ana", "csrf", "\"1\"", created.id(), new Input("Frios", null)))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"2\""));
		controller.delete("ana", "csrf", "\"2\"", created.id());
		assertThatThrownBy(() -> service.get(account, created.id())).isInstanceOf(ApiException.class);
	}

	private static final class InMemoryCategoriaRepository extends CategoriaRepository {

		private final Map<UUID, Stored> categories = new HashMap<>();
		private boolean activeProductsUpdated;
		private boolean itemSnapshotsUpdated;

		private InMemoryCategoriaRepository() {
			super(null);
		}

		@Override
		public List<Category> list(UUID accountId, String search, String afterName, UUID afterId, int limit) {
			return categories.values().stream()
					.filter(value -> value.accountId.equals(accountId) && !value.deleted)
					.map(Stored::category)
					.sorted(Comparator.comparing(Category::name).thenComparing(Category::id))
					.limit(limit + 1L).toList();
		}

		@Override
		public Optional<Category> find(UUID id, UUID accountId) {
			return Optional.ofNullable(categories.get(id))
					.filter(value -> value.accountId.equals(accountId) && !value.deleted)
					.map(Stored::category);
		}

		@Override
		public boolean nameExists(UUID accountId, String normalizedName, UUID ignored) {
			return categories.values().stream().anyMatch(value -> value.accountId.equals(accountId)
					&& !value.deleted && !value.id.equals(ignored)
					&& value.name.toLowerCase().equals(normalizedName));
		}

		@Override
		public void create(UUID id, UUID accountId, String name, String icon, Instant now) {
			categories.put(id, new Stored(id, accountId, name, icon, now));
		}

		@Override
		public int update(UUID id, UUID accountId, String name, String icon, Instant now, long version) {
			var stored = categories.get(id);
			if (stored == null || !stored.accountId.equals(accountId) || stored.version != version) return 0;
			stored.name = name;
			stored.icon = icon;
			stored.updatedAt = now;
			stored.version++;
			activeProductsUpdated = true;
			return 1;
		}

		@Override
		public int delete(UUID id, UUID accountId, long version) {
			var stored = categories.get(id);
			if (stored == null || !stored.accountId.equals(accountId) || stored.version != version) return 0;
			stored.deleted = true;
			stored.version++;
			return 1;
		}
	}

	private static final class Stored {

		private final UUID id;
		private final UUID accountId;
		private final Instant createdAt;
		private String name;
		private String icon;
		private Instant updatedAt;
		private long version = 1;
		private boolean deleted;

		private Stored(UUID id, UUID accountId, String name, String icon, Instant now) {
			this.id = id;
			this.accountId = accountId;
			this.name = name;
			this.icon = icon;
			this.createdAt = now;
			this.updatedAt = now;
		}

		private Category category() {
			return new Category(id, name, icon, 0, createdAt, updatedAt, version);
		}
	}
}
