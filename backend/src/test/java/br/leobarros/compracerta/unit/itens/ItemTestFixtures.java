package br.leobarros.compracerta.itens;

import java.time.Instant;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.itens.ItemDtos.CategorySnapshot;
import br.leobarros.compracerta.itens.ItemDtos.ListItem;
import br.leobarros.compracerta.itens.ItemDtos.ProductSnapshot;
import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import br.leobarros.compracerta.listas.ListaDtos.UserReference;

final class ItemTestFixtures {
	static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	static final UUID LIST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	static final UUID ITEM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	static final UUID TARGET_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
	static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	static final UUID CATEGORY_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
	static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
	static final Conta ACCOUNT = new Conta(ACCOUNT_ID, "Ana", "ana@example.com", "hash", true);
	static final ItemRepository.ProductData PRODUCT =
			new ItemRepository.ProductData(PRODUCT_ID, "Arroz", CATEGORY_ID, "Mercearia", "🛍️");
	static final ListSummary SUMMARY = new ListSummary(1, 0, 1, 0);

	private ItemTestFixtures() {
	}

	static ListItem item(UUID id, String quantity, String unit, long version) {
		return new ListItem(id, new ProductSnapshot(PRODUCT_ID, "Arroz"),
				new CategorySnapshot(CATEGORY_ID, "Mercearia", "🛍️"), quantity, unit, null,
				false, null, null, new UserReference(ACCOUNT_ID, "Ana"), NOW, NOW, version);
	}

	static ItemDtos.Input input(String quantity, String unit, String resolution) {
		return new ItemDtos.Input(PRODUCT_ID, quantity, unit, CATEGORY_ID, null, resolution, null);
	}
}
