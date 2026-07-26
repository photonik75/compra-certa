package br.leobarros.compracerta.ciclodevida;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CicloVidaService {
	private final Clock clock;
	private final CicloVidaRepository repository;
	private final IdempotenciaRepository idempotency;
	private final ListaService lists;
	private final ListaEventService events;
	CicloVidaService(
			Clock clock, CicloVidaRepository repository, IdempotenciaRepository idempotency,
			ListaService lists, ListaEventService events) {
		this.clock = clock;
		this.repository = repository;
		this.idempotency = idempotency;
		this.lists = lists;
		this.events = events;
	}
	@Transactional
	ListDetail change(Conta account, UUID listId, String status, long version, String key) {
		if (!Set.of("ACTIVE", "COMPLETED").contains(status)) {
			throw ApiSupport.validation("status", "Informe um estado válido.");
		}
		var content = listId + "|" + status + "|" + version;
		var replay = idempotency.replay(account.getId(), "LIST_STATUS_" + listId, key, content);
		if (replay.isPresent()) return lists.buscar(account, listId);
		var state = owner(account, listId);
		if (state.version() != version) throw ApiSupport.conflict(state.version());
		if (state.status().equals(status)) {
			throw new ApiException(
					HttpStatus.CONFLICT, "INVALID_LIST_TRANSITION",
					"A lista já está no estado solicitado.");
		}
		idempotency.begin(account.getId(), "LIST_STATUS_" + listId, key, content);
		if (repository.change(listId, status, clock.instant(), version) == 0) {
			throw ApiSupport.conflict(owner(account, listId).version());
		}
		idempotency.finish(account.getId(), "LIST_STATUS_" + listId, key, listId, status);
		var result = lists.buscar(account, listId);
		events.publish(listId, result.version(), listId, "list.status.changed", result);
		return result;
	}
	@Transactional
	void delete(Conta account, UUID listId, long version, String key) {
		var content = listId + "|" + version;
		var replay = idempotency.replay(account.getId(), "LIST_DELETE_" + listId, key, content);
		if (replay.isPresent()) return;
		var state = owner(account, listId);
		if (state.version() != version) throw ApiSupport.conflict(state.version());
		idempotency.begin(account.getId(), "LIST_DELETE_" + listId, key, content);
		if (repository.delete(listId, clock.instant(), version) == 0) {
			throw ApiSupport.conflict(owner(account, listId).version());
		}
		idempotency.finish(account.getId(), "LIST_DELETE_" + listId, key, listId, "DELETED");
		events.publish(listId, version + 1, listId, "list.deleted", java.util.Map.of("listId", listId));
	}
	private CicloVidaRepository.State owner(Conta account, UUID listId) {
		var state = repository.find(listId, account.getId()).orElseThrow(ApiSupport::notFound);
		if (!state.ownerId().equals(account.getId())) {
			throw new ApiException(
					HttpStatus.FORBIDDEN, "FORBIDDEN",
					"Somente o proprietário pode realizar esta operação.");
		}
		return state;
	}
}
