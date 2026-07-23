package br.leobarros.compracerta.integration.autenticacao.cadastro;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.leobarros.compracerta.autenticacao.cadastro.CadastroSecurityConfiguration;
import br.leobarros.compracerta.autenticacao.cadastro.CadastroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(CadastroSecurityConfiguration.class)
@WebMvcTest
class CadastroControllerIntegrationTest {

	private static final String ENDPOINT_CADASTRO = "/api/v1/auth/registrations";
	private static final String CHAVE_IDEMPOTENCIA = "cadastro-entrada-invalida";
	private static final String CORPO_INVALIDO = """
			{
			  "name": "",
			  "email": "email-invalido",
			  "password": "curta",
			  "passwordConfirmation": ""
			}
			""";
	private static final String CODIGO_ERRO_VALIDACAO = "VALIDATION_ERROR";
	private static final String DETALHE_ERRO_VALIDACAO = "Verifique os campos informados e tente novamente.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CadastroService cadastroService;

	@Test
	void beCad10RetornaErrosPorCampoQuandoEntradaEhInvalida() throws Exception {
		mockMvc.perform(post(ENDPOINT_CADASTRO)
						.header("Idempotency-Key", CHAVE_IDEMPOTENCIA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(CORPO_INVALIDO))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(CODIGO_ERRO_VALIDACAO))
				.andExpect(jsonPath("$.detail").value(DETALHE_ERRO_VALIDACAO))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'passwordConfirmation')]").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors", hasSize(4)))
				.andExpect(jsonPath("$.fieldErrors[*].message", everyItem(not(blankOrNullString()))));
	}
}
