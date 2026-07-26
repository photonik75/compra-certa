package br.leobarros.compracerta.itens;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import br.leobarros.compracerta.listas.ListaDtos.UserReference;

public final class ItemDtos {
	private ItemDtos() {
	}
	public record Input(
			UUID productId, String quantity, String unit, UUID categoryId, String notes,
			String resolution, Long targetVersion) {
	}
	public record CheckInput(Boolean checked) {
	}
	public record ProductSnapshot(UUID id, String name) {
	}
	public record CategorySnapshot(UUID id, String name, String icon) {
	}
	public record ListItem(
			UUID id, ProductSnapshot product, CategorySnapshot category, String quantity, String unit,
			String notes, boolean checked, Instant checkedAt, UserReference checkedBy,
			UserReference createdBy, Instant createdAt, Instant updatedAt, long version) {
	}
	public record PageInfo(String nextCursor, boolean hasMore) {
	}
	public record Collection(List<ListItem> items, PageInfo page, ListSummary listSummary, long listVersion) {
	}
	public record Mutation(
			String outcome, ListItem item, UUID removedItemId, ListSummary listSummary, long listVersion) {
	}
	public record Deletion(UUID deletedItemId, ListSummary listSummary, long listVersion) {
	}
	public record CheckResult(ListItem item, ListSummary listSummary, long listVersion) {
	}
}
