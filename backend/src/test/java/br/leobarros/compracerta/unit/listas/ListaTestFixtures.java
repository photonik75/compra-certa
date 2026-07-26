package br.leobarros.compracerta.listas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.listas.ListaDtos.CollectionSummary;
import br.leobarros.compracerta.listas.ListaDtos.ListCard;
import br.leobarros.compracerta.listas.ListaDtos.ListCollection;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import br.leobarros.compracerta.listas.ListaDtos.PageInfo;
import br.leobarros.compracerta.listas.ListaDtos.UserReference;

final class ListaTestFixtures {

	static final UUID CONTA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	static final UUID LISTA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	static final Instant AGORA = Instant.parse("2026-07-26T12:00:00Z");
	static final Conta CONTA = new Conta(CONTA_ID, "Ana", "ana@example.com", "hash", true);

	private ListaTestFixtures() {
	}

	static ListDetail detalhe(String nome, String descricao, String estado, String papel, long versao) {
		var proprietarioId = "OWNER".equals(papel) ? CONTA_ID : UUID.randomUUID();
		return new ListDetail(
				LISTA_ID,
				nome,
				descricao,
				estado,
				new UserReference(proprietarioId, "Ana"),
				papel,
				false,
				new ListSummary(0, 0, 0, 0),
				AGORA,
				AGORA,
				null,
				versao);
	}

	static ListCard cartao(UUID id, Instant atualizadaEm) {
		return new ListCard(
				id,
				"Mercado",
				"ACTIVE",
				"OWNER",
				new UserReference(CONTA_ID, "Ana"),
				false,
				new ListSummary(0, 0, 0, 0),
				atualizadaEm,
				null,
				1);
	}

	static ListCollection colecao(List<ListCard> itens, boolean hasMore) {
		return new ListCollection(
				itens,
				new PageInfo(null, hasMore),
				new CollectionSummary(itens.size(), 0));
	}
}
