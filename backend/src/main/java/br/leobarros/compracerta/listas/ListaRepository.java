package br.leobarros.compracerta.listas;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.listas.ListaDtos.CollectionSummary;
import br.leobarros.compracerta.listas.ListaDtos.ListCollection;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;

interface ListaRepository {

	ListCollection listar(UUID contaId, String status, String search, Instant cursorData, UUID cursorId, int limit);

	CollectionSummary resumirAtivas(UUID contaId);

	Optional<ListDetail> buscarAcessivel(UUID listaId, UUID contaId);

	boolean nomeEmUso(UUID contaId, String nome, UUID listaIgnorada);

	void criar(UUID id, UUID contaId, String nome, String descricao, Instant agora);

	int atualizar(UUID id, String nome, String descricao, Instant agora, long versao);

	Optional<Idempotencia> buscarIdempotencia(UUID contaId, String chave);

	boolean iniciarIdempotencia(UUID contaId, String chave, String fingerprint);

	void concluirIdempotencia(UUID contaId, String chave, UUID listaId);

	record Idempotencia(String fingerprint, UUID listaId) {
	}
}
