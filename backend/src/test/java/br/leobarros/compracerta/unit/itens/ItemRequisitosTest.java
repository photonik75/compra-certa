package br.leobarros.compracerta.itens;

import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRequisitosTest {
	@Test
	void beItem12MutacoesItemResumoEMesclagemSaoAtomicas() {
		for (var name : java.util.List.of("create", "update", "delete")) {
			assertThat(java.util.Arrays.stream(ItemService.class.getDeclaredMethods())
					.filter(method -> method.getName().equals(name)).findFirst().orElseThrow()
					.isAnnotationPresent(Transactional.class)).isTrue();
		}
	}

	@Test
	void beItem14MapeiaErrosNormativos() {
		assertThat(ApiSupport.validation("quantity", "inválida").code()).isEqualTo("VALIDATION_ERROR");
		assertThat(ApiSupport.notFound().code()).isEqualTo("NOT_FOUND");
		assertThat(ApiSupport.conflict(2).etag()).isEqualTo("\"2\"");
		assertThat(new ApiException(
				org.springframework.http.HttpStatus.CONFLICT, "DUPLICATE_ITEM", "duplicado").code())
				.isEqualTo("DUPLICATE_ITEM");
	}

	@Test
	void beItem15ArquiteturaControllerServiceRepositoryPorFuncionalidade() {
		assertThat(ItemController.class.getPackageName()).isEqualTo("br.leobarros.compracerta.itens");
		assertThat(ItemController.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ItemService.class));
		assertThat(ItemService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ItemRepository.class));
	}
}
