package br.leobarros.compracerta.categorias;

import java.nio.file.Files;
import java.nio.file.Path;

import br.leobarros.compracerta.comum.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaRequisitosTest {

	@Test
	void beCat01MigracaoCriaExatamenteQuatroCategoriasIniciaisUmaVez() throws Exception {
		var sql = Files.readString(Path.of("src/main/resources/db/migration/V5__catalogo_itens_colaboracao.sql"));
		var triggerValues = sql.substring(sql.indexOf("CREATE OR REPLACE FUNCTION criar_categorias_iniciais"),
				sql.indexOf("CREATE TRIGGER contas_categorias_iniciais"));
		assertThat(triggerValues).containsOnlyOnce("'Hortifruti'").containsOnlyOnce("'Mercearia'")
				.containsOnlyOnce("'Bebidas'").containsOnlyOnce("'Limpeza'");
		assertThat(triggerValues).doesNotContain("produtos");
		assertThat(sql).contains("AFTER INSERT ON contas FOR EACH ROW EXECUTE FUNCTION criar_categorias_iniciais()");
	}

	@Test
	void beCat10AtualizacaoEPropagacaoSaoTransacionais() throws Exception {
		assertThat(CategoriaService.class
				.getDeclaredMethod("update", br.leobarros.compracerta.autenticacao.cadastro.Conta.class,
						java.util.UUID.class, CategoriaDtos.Input.class, long.class)
				.isAnnotationPresent(Transactional.class)).isTrue();
	}

	@Test
	void beCat14ErrosPossuemStatusCodigoCampoMensagemEEtagNormativos() {
		var validation = br.leobarros.compracerta.comum.ApiSupport.validation("name", "Nome inválido.");
		var conflict = br.leobarros.compracerta.comum.ApiSupport.conflict(4);
		assertThat(validation.status().value()).isEqualTo(400);
		assertThat(validation.code()).isEqualTo("VALIDATION_ERROR");
		assertThat(validation.fieldErrors()).singleElement().extracting("field").isEqualTo("name");
		assertThat(validation.getMessage()).isEqualTo("Nome inválido.");
		assertThat(conflict.code()).isEqualTo("CONFLICT");
		assertThat(conflict.etag()).isEqualTo("\"4\"");
	}

	@Test
	void beCat15ArquiteturaMantemControllerServiceRepositoryNoPacoteDaFuncionalidade() {
		assertThat(CategoriaController.class.getPackageName()).isEqualTo("br.leobarros.compracerta.categorias");
		assertThat(CategoriaService.class.getPackageName()).isEqualTo("br.leobarros.compracerta.categorias");
		assertThat(CategoriaRepository.class.getPackageName()).isEqualTo("br.leobarros.compracerta.categorias");
		assertThat(CategoriaController.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(CategoriaService.class));
		assertThat(CategoriaService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(CategoriaRepository.class));
	}
}
