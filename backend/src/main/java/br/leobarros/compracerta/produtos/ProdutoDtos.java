package br.leobarros.compracerta.produtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProdutoDtos {
	private ProdutoDtos() {
	}
	public record Input(String name, UUID categoryId, String defaultUnit) {
	}
	public record CategoryReference(UUID id, String name, String icon, boolean available) {
	}
	public record Product(
			UUID id, String name, CategoryReference category, String defaultUnit, boolean active,
			Instant createdAt, Instant updatedAt, long version) {
	}
	public record PageInfo(String nextCursor, boolean hasMore) {
	}
	public record Collection(List<Product> items, PageInfo page) {
	}
}
