package br.leobarros.compracerta.listas;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListaArquiteturaTest {

	@Test
	void beLis15MantemDependenciasControllerServiceRepository() {
		assertThat(Arrays.stream(ListaController.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ListaService.class))).isTrue();
		assertThat(Arrays.stream(ListaService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ListaRepository.class))).isTrue();
		assertThat(ListaRepository.class.isInterface()).isTrue();
		assertThat(Modifier.isPublic(ListaPostgresqlRepository.class.getModifiers())).isFalse();
	}
}
