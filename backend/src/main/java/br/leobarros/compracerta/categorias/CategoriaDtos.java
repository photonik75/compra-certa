package br.leobarros.compracerta.categorias;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CategoriaDtos {

	private CategoriaDtos() {
	}

	public record Input(String name, String icon) {
	}

	public record Category(
			UUID id,
			String name,
			String icon,
			int activeProductCount,
			Instant createdAt,
			Instant updatedAt,
			long version) {
	}

	public record PageInfo(String nextCursor, boolean hasMore) {
	}

	public record Collection(List<Category> items, PageInfo page) {
	}
}
