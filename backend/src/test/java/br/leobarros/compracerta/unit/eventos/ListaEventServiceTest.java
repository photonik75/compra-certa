package br.leobarros.compracerta.eventos;

import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ListaEventServiceTest {
	@AfterEach
	void cleanSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void eventoEhPublicadoSomenteDepoisDoCommit() {
		var service = spy(new ListaEventService(Clock.systemUTC()));
		var listId = UUID.randomUUID();
		TransactionSynchronizationManager.initSynchronization();
		service.publish(listId, 2, listId, "list.changed", java.util.Map.of("status", "ACTIVE"));
		verify(service, never()).dispatch(any(), anyLong(), any(), any(), any());
		TransactionSynchronizationManager.getSynchronizations().forEach(
				org.springframework.transaction.support.TransactionSynchronization::afterCommit);
		verify(service).dispatch(listId, 2, listId, "list.changed", java.util.Map.of("status", "ACTIVE"));
	}

	@Test
	void eventoNaoEhPublicadoQuandoTransacaoFalha() {
		var service = spy(new ListaEventService(Clock.systemUTC()));
		var listId = UUID.randomUUID();
		TransactionSynchronizationManager.initSynchronization();
		service.publish(listId, 2, listId, "list.changed", java.util.Map.of());
		TransactionSynchronizationManager.getSynchronizations().forEach(
				synchronization -> synchronization.afterCompletion(
						org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
		verify(service, never()).dispatch(any(), anyLong(), any(), any(), any());
	}
}
