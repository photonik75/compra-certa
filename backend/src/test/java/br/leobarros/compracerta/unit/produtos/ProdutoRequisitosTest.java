package br.leobarros.compracerta.produtos;

import br.leobarros.compracerta.comum.ApiSupport;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoRequisitosTest {
	@Test
	void beProd12MutacoesSaoAtomicas() throws Exception {
		for (var name : java.util.List.of("create", "update", "deactivate")) {
			assertThat(java.util.Arrays.stream(ProdutoService.class.getDeclaredMethods())
					.filter(method -> method.getName().equals(name)).findFirst().orElseThrow()
					.isAnnotationPresent(Transactional.class)).isTrue();
		}
	}

	@Test
	void beProd14SchemasEErrosNormativosPreservamReferenciaDatasVersao() {
		var product = ProdutoTestFixtures.product("Arroz", false, 4);
		assertThat(product.category().available()).isTrue();
		assertThat(product.createdAt()).isEqualTo(ProdutoTestFixtures.NOW);
		assertThat(product.version()).isEqualTo(4);
		assertThat(ApiSupport.conflict(4).etag()).isEqualTo("\"4\"");
		assertThat(ApiSupport.notFound().code()).isEqualTo("NOT_FOUND");
	}

	@Test
	void beProd15ArquiteturaControllerServiceRepositoryPorFuncionalidade() {
		assertThat(ProdutoController.class.getPackageName()).isEqualTo("br.leobarros.compracerta.produtos");
		assertThat(ProdutoController.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ProdutoService.class));
		assertThat(ProdutoService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ProdutoRepository.class));
	}
}
