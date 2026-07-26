package br.leobarros.compracerta.integration.listas;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.listas.ListaController;
import br.leobarros.compracerta.listas.ListaDtos.CollectionSummary;
import br.leobarros.compracerta.listas.ListaDtos.CreateListRequest;
import br.leobarros.compracerta.listas.ListaDtos.ListCard;
import br.leobarros.compracerta.listas.ListaDtos.ListCollection;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import br.leobarros.compracerta.listas.ListaDtos.PageInfo;
import br.leobarros.compracerta.listas.ListaDtos.UserReference;
import br.leobarros.compracerta.listas.ListaRepository;
import br.leobarros.compracerta.listas.ListaService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListaFluxoIntegrationTest {

	private static final UUID CONTA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final Instant AGORA = Instant.parse("2026-07-26T12:00:00Z");

	@Test
	void beLis16IntegraControllerServiceERepositoryStubComPersistenciaIsolamentoEConcorrencia() {
		var conta = new Conta(CONTA_ID, "Ana", "ana@example.com", "hash", true);
		var repository = new ListaRepositoryStub();
		var service = new ListaService(Clock.fixed(AGORA, ZoneOffset.UTC), repository);
		var sessions = mock(SessaoService.class);
		when(sessions.obterContaAutenticada("token")).thenReturn(conta);
		var controller = new ListaController(service, sessions);
		var criada = controller.criar(
				"token", "csrf", "key-1", new CreateListRequest("  Mercado  ", "Casa")).getBody();
		assertThat(criada.name()).isEqualTo("Mercado");
		assertThat(criada.status()).isEqualTo("ACTIVE");
		assertThat(controller.listar("token", null, null, null, null).getBody().items())
				.extracting(ListCard::id).containsExactly(criada.id());
		var atualizada = service.atualizar(
				conta, criada.id(), 1L, true, "Feira", false, null);
		assertThat(atualizada.name()).isEqualTo("Feira");
		assertThat(atualizada.version()).isEqualTo(2);
		assertThat(repository.details).hasSize(1);
	}

	private static class ListaRepositoryStub implements ListaRepository {

		private final Map<UUID, ListDetail> details = new LinkedHashMap<>();
		private final Map<String, Idempotencia> idempotencies = new LinkedHashMap<>();

		@Override
		public ListCollection listar(
				UUID contaId, String status, String search, Instant cursorData, UUID cursorId, int limit) {
			var cards = details.values().stream()
					.filter(detail -> detail.owner().id().equals(contaId))
					.filter(detail -> "ALL".equals(status) || detail.status().equals(status))
					.limit(limit)
					.map(this::card)
					.toList();
			return new ListCollection(
					cards,
					new PageInfo(null, false),
					new CollectionSummary(cards.size(), 0));
		}

		@Override
		public CollectionSummary resumirAtivas(UUID contaId) {
			return new CollectionSummary(details.size(), 0);
		}

		@Override
		public Optional<ListDetail> buscarAcessivel(UUID listaId, UUID contaId) {
			return Optional.ofNullable(details.get(listaId))
					.filter(detail -> detail.owner().id().equals(contaId));
		}

		@Override
		public boolean nomeEmUso(UUID contaId, String nome, UUID listaIgnorada) {
			return details.values().stream()
					.anyMatch(detail -> detail.owner().id().equals(contaId)
							&& detail.name().equalsIgnoreCase(nome)
							&& !detail.id().equals(listaIgnorada));
		}

		@Override
		public void criar(UUID id, UUID contaId, String nome, String descricao, Instant agora) {
			details.put(id, new ListDetail(
					id, nome, descricao, "ACTIVE", new UserReference(contaId, "Ana"), "OWNER", false,
					new ListSummary(0, 0, 0, 0), agora, agora, null, 1));
		}

		@Override
		public int atualizar(UUID id, String nome, String descricao, Instant agora, long versao) {
			var atual = details.get(id);
			if (atual == null || atual.version() != versao) return 0;
			details.put(id, new ListDetail(
					id, nome, descricao, atual.status(), atual.owner(), atual.role(), atual.shared(),
					atual.summary(), atual.createdAt(), agora, atual.completedAt(), versao + 1));
			return 1;
		}

		@Override
		public Optional<Idempotencia> buscarIdempotencia(UUID contaId, String chave) {
			return Optional.ofNullable(idempotencies.get(contaId + chave));
		}

		@Override
		public boolean iniciarIdempotencia(UUID contaId, String chave, String fingerprint) {
			idempotencies.put(contaId + chave, new Idempotencia(fingerprint, null));
			return true;
		}

		@Override
		public void concluirIdempotencia(UUID contaId, String chave, UUID listaId) {
			var atual = idempotencies.get(contaId + chave);
			idempotencies.put(contaId + chave, new Idempotencia(atual.fingerprint(), listaId));
		}

		private ListCard card(ListDetail detail) {
			return new ListCard(
					detail.id(), detail.name(), detail.status(), detail.role(), detail.owner(),
					detail.shared(), detail.summary(), detail.updatedAt(), detail.completedAt(), detail.version());
		}
	}
}
