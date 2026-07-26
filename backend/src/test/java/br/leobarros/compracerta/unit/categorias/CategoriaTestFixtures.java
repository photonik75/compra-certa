package br.leobarros.compracerta.categorias;

import java.time.Instant;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.categorias.CategoriaDtos.Category;

final class CategoriaTestFixtures {

	static final UUID CONTA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	static final UUID OUTRA_CONTA_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
	static final UUID CATEGORIA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	static final Instant AGORA = Instant.parse("2026-01-01T12:00:00Z");
	static final Conta CONTA = new Conta(CONTA_ID, "Ana", "ana@example.com", "hash", true);

	private CategoriaTestFixtures() {
	}

	static Category categoria(String name, String icon, int products, long version) {
		return new Category(CATEGORIA_ID, name, icon, products, AGORA, AGORA, version);
	}
}
