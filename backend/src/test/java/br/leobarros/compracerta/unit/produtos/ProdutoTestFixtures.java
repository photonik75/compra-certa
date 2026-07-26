package br.leobarros.compracerta.produtos;

import java.time.Instant;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.produtos.ProdutoDtos.CategoryReference;
import br.leobarros.compracerta.produtos.ProdutoDtos.Product;

final class ProdutoTestFixtures {
	static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
	static final Conta ACCOUNT = new Conta(ACCOUNT_ID, "Ana", "ana@example.com", "hash", true);
	static final CategoryReference CATEGORY = new CategoryReference(CATEGORY_ID, "Mercearia", "🛍️", true);

	private ProdutoTestFixtures() {
	}

	static Product product(String name, boolean active, long version) {
		return new Product(PRODUCT_ID, name, CATEGORY, "UNIT", active, NOW, NOW, version);
	}
}
