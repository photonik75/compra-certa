package br.leobarros.compracerta.listas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ListaDtos {

	private ListaDtos() {
	}

	public record CreateListRequest(String name, String description) {
	}

	public record UpdateListRequest(String name, String description) {
	}

	public record UserReference(UUID id, String name) {
	}

	public record ListSummary(int total, int checked, int pending, int percentage) {
	}

	public record ListCard(
			UUID id,
			String name,
			String status,
			String role,
			UserReference owner,
			boolean shared,
			ListSummary summary,
			Instant updatedAt,
			Instant completedAt,
			long version) {
	}

	public record PageInfo(String nextCursor, boolean hasMore) {
	}

	public record CollectionSummary(int activeLists, int pendingItems) {
	}

	public record ListCollection(List<ListCard> items, PageInfo page, CollectionSummary summary) {
	}

	public record ListDetail(
			UUID id,
			String name,
			String description,
			String status,
			UserReference owner,
			String role,
			boolean shared,
			ListSummary summary,
			Instant createdAt,
			Instant updatedAt,
			Instant completedAt,
			long version) {
	}
}
