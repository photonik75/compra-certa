package br.leobarros.compracerta.integration.autenticacao.recuperacao;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.leobarros.compracerta.autenticacao.comum.idempotencia.IdempotenciaService;
import br.leobarros.compracerta.autenticacao.configuracao.AutenticacaoSecurityConfiguration;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.recuperacao.RecuperacaoSenhaController;
import br.leobarros.compracerta.autenticacao.recuperacao.RecuperacaoSenhaException;
import br.leobarros.compracerta.autenticacao.recuperacao.RecuperacaoSenhaExceptionHandler;
import br.leobarros.compracerta.autenticacao.recuperacao.RedefinicaoSenhaService;
import br.leobarros.compracerta.autenticacao.recuperacao.SolicitacaoRecuperacaoService;
import br.leobarros.compracerta.autenticacao.recuperacao.TokenRecuperacaoInvalidoException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoController;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCookieService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoExceptionHandler;
import br.leobarros.compracerta.autenticacao.sessao.SessaoInvalidaException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({
		AutenticacaoSecurityConfiguration.class,
		ApiErrorResponseService.class,
		IdempotenciaService.class,
		RecuperacaoSenhaExceptionHandler.class
		,SessaoController.class
		,SessaoExceptionHandler.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebMvcTest(RecuperacaoSenhaController.class)
class RecuperacaoSenhaControllerIntegrationTest {

	private static final String SOLICITACAO = "/api/v1/auth/password-reset-requests";
	private static final String REDEFINICAO = "/api/v1/auth/password-resets";
	private static final String CHAVE = "chave-idempotente";
	private static final String EMAIL_EXISTENTE = "maria@example.com";
	private static final String EMAIL_INEXISTENTE = "ninguem@example.com";
	private static final String TOKEN = "token-recuperacao-secreto";
	private static final String SENHA = "nova senha segura";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private SolicitacaoRecuperacaoService solicitacaoService;
	@MockitoBean
	private RedefinicaoSenhaService redefinicaoService;
	@MockitoBean
	private SessaoService sessaoService;
	@MockitoBean
	private SessaoCookieService sessaoCookieService;

	@Test
	void beRec03EmailExistenteEInexistenteRecebemRespostaIndistinguivel() throws Exception {
		var existente = solicitar(EMAIL_EXISTENTE, "chave-1").andExpect(status().isAccepted()).andReturn();
		var inexistente = solicitar(EMAIL_INEXISTENTE, "chave-2").andExpect(status().isAccepted()).andReturn();
		org.junit.jupiter.api.Assertions.assertEquals(
				existente.getResponse().getContentAsString(),
				inexistente.getResponse().getContentAsString());
	}

	@Test
	void beRec07RespostaNaoExpoeTokenEmTextoPuro() throws Exception {
		solicitar(EMAIL_EXISTENTE, CHAVE)
				.andExpect(status().isAccepted())
				.andExpect(content().string(not(containsString(TOKEN))));
	}

	@Test
	void beRec09FalhaNoEnvioRetorna500Polido() throws Exception {
		doThrow(new RecuperacaoSenhaException(
				"Não foi possível enviar as instruções. Tente novamente mais tarde."))
				.when(solicitacaoService).solicitar(EMAIL_EXISTENTE);
		solicitar(EMAIL_EXISTENTE, CHAVE)
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("PASSWORD_RESET_DELIVERY_FAILED"))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void beRec10MesmaChaveNaoCriaNemEnviaSegundoToken() throws Exception {
		solicitar(EMAIL_EXISTENTE, CHAVE).andExpect(status().isAccepted());
		solicitar(EMAIL_EXISTENTE, CHAVE).andExpect(status().isAccepted());
		verify(solicitacaoService).solicitar(EMAIL_EXISTENTE);
	}

	@Test
	void beRec11ChaveAusenteInvalidaOuReutilizadaComOutroConteudoEhRejeitada() throws Exception {
		solicitar(EMAIL_EXISTENTE, null).andExpect(status().isBadRequest());
		solicitar(EMAIL_EXISTENTE, "a".repeat(256)).andExpect(status().isBadRequest());
		solicitar(EMAIL_EXISTENTE, CHAVE).andExpect(status().isAccepted());
		solicitar(EMAIL_INEXISTENTE, CHAVE).andExpect(status().isConflict());
	}

	@Test
	void beRed08ReutilizarTokenRetorna400ENaoAlteraNovamente() throws Exception {
		doThrow(new TokenRecuperacaoInvalidoException()).when(redefinicaoService)
				.redefinir(TOKEN, SENHA, SENHA);
		redefinir(TOKEN, SENHA, SENHA, CHAVE)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));
	}

	@Test
	void beRed09SomenteNovaSenhaEhEncaminhadaParaPersistencia() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, CHAVE).andExpect(status().isNoContent());
		verify(redefinicaoService).redefinir(TOKEN, SENHA, SENHA);
	}

	@Test
	void beRed10RedefinicaoAcionaInvalidacaoDasSessoesNoServico() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, CHAVE).andExpect(status().isNoContent());
		verify(redefinicaoService).redefinir(TOKEN, SENHA, SENHA);
	}

	@Test
	void beRed11SucessoRetorna204SemCriarCookieOuAutenticar() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, CHAVE)
				.andExpect(status().isNoContent())
				.andExpect(cookie().doesNotExist("cc_session"))
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	void beRed12FalhaAtomicaRetornaErroGenericoSemDetalhesInternos() throws Exception {
		doThrow(new IllegalStateException("detalhe interno")).when(redefinicaoService)
				.redefinir(TOKEN, SENHA, SENHA);
		redefinir(TOKEN, SENHA, SENHA, CHAVE)
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(not(containsString("detalhe interno"))));
	}

	@Test
	void beRed13MesmaChaveRetornaResultadoSemNovoProcessamento() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, CHAVE).andExpect(status().isNoContent());
		redefinir(TOKEN, SENHA, SENHA, CHAVE).andExpect(status().isNoContent());
		verify(redefinicaoService).redefinir(TOKEN, SENHA, SENHA);
	}

	@Test
	void beRed14ChaveAusenteInvalidaOuReutilizadaComOutroConteudoEhRejeitada() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, null).andExpect(status().isBadRequest());
		redefinir(TOKEN, SENHA, SENHA, "a".repeat(256)).andExpect(status().isBadRequest());
		redefinir(TOKEN, SENHA, SENHA, CHAVE).andExpect(status().isNoContent());
		redefinir("outro-token", SENHA, SENHA, CHAVE).andExpect(status().isConflict());
	}

	@Test
	void beCtr01EndpointsAceitamSomenteMetodosETiposDocumentados() throws Exception {
		mockMvc.perform(get(SOLICITACAO)).andExpect(status().is4xxClientError());
		mockMvc.perform(post(SOLICITACAO).header("Idempotency-Key", CHAVE)
						.contentType(MediaType.TEXT_PLAIN).content(EMAIL_EXISTENTE))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void beCtr02CamposAusentesETiposIncorretosRetornam400() throws Exception {
		mockMvc.perform(post(SOLICITACAO).header("Idempotency-Key", CHAVE)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(REDEFINICAO).header("Idempotency-Key", CHAVE)
						.contentType(MediaType.APPLICATION_JSON).content("{\"token\":42}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void beCtr03ErrosUsamProblemJsonCodigoDetalheEFieldErrors() throws Exception {
		mockMvc.perform(post(SOLICITACAO).header("Idempotency-Key", CHAVE)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void beCtr04ErroInesperadoNaoExpoeStackTraceOuDetalheInterno() throws Exception {
		doThrow(new IllegalStateException("senha-do-banco")).when(solicitacaoService).solicitar(EMAIL_EXISTENTE);
		solicitar(EMAIL_EXISTENTE, CHAVE)
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(not(containsString("senha-do-banco"))))
				.andExpect(content().string(not(containsString("stackTrace"))));
	}

	@Test
	void beSeg01SegredosNaoAparecemNasRespostas() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, CHAVE)
				.andExpect(content().string(not(containsString(TOKEN))))
				.andExpect(content().string(not(containsString(SENHA))));
	}

	@Test
	void beSeg02RecuperacaoNaoPermiteInferirExistenciaDaConta() throws Exception {
		beRec03EmailExistenteEInexistenteRecebemRespostaIndistinguivel();
	}

	@Test
	void beSeg03EndpointProtegidoRejeitaSessaoInvalida() throws Exception {
		doThrow(new SessaoInvalidaException()).when(sessaoService).consultar(null);
		mockMvc.perform(get("/api/v1/auth/session")).andExpect(status().isUnauthorized());
	}

	@Test
	void beSeg04RespostasDeRecuperacaoNaoSaoArmazenaveisQuandoContemSessao() throws Exception {
		redefinir(TOKEN, SENHA, SENHA, CHAVE).andExpect(header().doesNotExist("Set-Cookie"));
	}

	private org.springframework.test.web.servlet.ResultActions solicitar(String email, String chave)
			throws Exception {
		var request = post(SOLICITACAO).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\"}");
		if (chave != null) request.header("Idempotency-Key", chave);
		return mockMvc.perform(request);
	}

	private org.springframework.test.web.servlet.ResultActions redefinir(
			String token,
			String senha,
			String confirmacao,
			String chave) throws Exception {
		var corpo = """
				{"token":"%s","newPassword":"%s","passwordConfirmation":"%s"}
				""".formatted(token, senha, confirmacao);
		var request = post(REDEFINICAO).contentType(MediaType.APPLICATION_JSON).content(corpo);
		if (chave != null) request.header("Idempotency-Key", chave);
		return mockMvc.perform(request);
	}
}
