package br.leobarros.compracerta.eventos;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ListaEventService {
	private final Clock clock;
	private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> clients = new ConcurrentHashMap<>();
	public ListaEventService(Clock clock) {
		this.clock = clock;
	}
	public SseEmitter subscribe(UUID listId) {
		var emitter = new SseEmitter(0L);
		var list = clients.computeIfAbsent(listId, ignored -> new CopyOnWriteArrayList<>());
		list.add(emitter);
		emitter.onCompletion(() -> remove(listId, emitter));
		emitter.onTimeout(() -> remove(listId, emitter));
		emitter.onError(error -> remove(listId, emitter));
		send(emitter, UUID.randomUUID().toString(), "connected", Map.of("occurredAt", clock.instant()));
		return emitter;
	}
	public void publish(UUID listId, long listVersion, UUID resourceId, String type, Object payload) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					dispatch(listId, listVersion, resourceId, type, payload);
				}
			});
			return;
		}
		dispatch(listId, listVersion, resourceId, type, payload);
	}
	protected void dispatch(UUID listId, long listVersion, UUID resourceId, String type, Object payload) {
		var id = UUID.randomUUID().toString();
		var event = Map.of(
				"listId", listId,
				"listVersion", listVersion,
				"resourceId", resourceId == null ? listId : resourceId,
				"occurredAt", clock.instant(),
				"type", type,
				"payload", payload);
		clients.getOrDefault(listId, new CopyOnWriteArrayList<>())
				.removeIf(emitter -> !send(emitter, id, type, event));
	}
	private boolean send(SseEmitter emitter, String id, String type, Object data) {
		try {
			emitter.send(SseEmitter.event().id(id).name(type).data(data));
			return true;
		} catch (IOException exception) {
			emitter.complete();
			return false;
		}
	}
	private void remove(UUID listId, SseEmitter emitter) {
		var list = clients.get(listId);
		if (list == null) return;
		list.remove(emitter);
		if (list.isEmpty()) clients.remove(listId, list);
	}
}
