package br.leobarros.compracerta.integration.autenticacao.sessao;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.configuracao.AutenticacaoSecurityConfiguration;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoController;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCookieService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCriada;
import br.leobarros.compracerta.autenticacao.sessao.SessaoExceptionHandler;
import br.leobarros.compracerta.autenticacao.sessao.SessaoRepository;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.support.MutableClock;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@Import({
		AutenticacaoSecurityConfiguration.class,
		ApiErrorResponseService.class,
		GeradorIdentificadorService.class,
		SessaoCookieService.class,
		SessaoExceptionHandler.class,
		SessaoRepository.class,
		SessaoService.class,
		LogoutCsrfControllerIntegrationTest.Configuracao.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebMvcTest(SessaoController.class)
class LogoutCsrfControllerIntegrationTest {

	private static final String ENDPOINT_CONSULTA = "/api/v1/auth/session";
	private static final String ENDPOINT_LOGOUT = "/api/v1/auth/sessions/current";
	private static final String HEADER_CSRF = "X-CSRF-Token";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SessaoService sessaoService;

	private SessaoCriada primeira;
	private SessaoCriada segunda;

	@BeforeEach
	void criarSessoes() {
		var conta = new Conta("Maria Silva", "maria@example.com", "hash");
		primeira = sessaoService.criarParaLogin(conta, false);
		segunda = sessaoService.criarParaLogin(conta, false);
	}

	@Test
	void beSai01LogoutInvalidaSessaoAtualERetorna204() throws Exception {
		logout(primeira, primeira.response().csrfToken()).andExpect(status().isNoContent());
		mockMvc.perform(get(ENDPOINT_CONSULTA).cookie(cookie(primeira))).andExpect(status().isUnauthorized());
	}

	@Test
	void beSai02LogoutExpiraCookieComMesmosAtributosDeSeguranca() throws Exception {
		logout(primeira, primeira.response().csrfToken())
				.andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", containsString("cc_session=")))
				.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", containsString("Secure")))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/api/v1")));
	}

	@Test
	void beSai03CookieAnteriorNaoAutenticaDepoisDoLogout() throws Exception {
		logout(primeira, primeira.response().csrfToken()).andExpect(status().isNoContent());
		mockMvc.perform(get(ENDPOINT_CONSULTA).cookie(cookie(primeira))).andExpect(status().isUnauthorized());
	}

	@Test
	void beSai04LogoutSemSessaoValidaRetorna401() throws Exception {
		mockMvc.perform(delete(ENDPOINT_LOGOUT).header(HEADER_CSRF, "csrf"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").isNotEmpty());
		mockMvc.perform(delete(ENDPOINT_LOGOUT)
						.cookie(new Cookie("cc_session", "desconhecida"))
						.header(HEADER_CSRF, "csrf"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void beSai05LogoutRejeitaCsrfAusenteIncorretoOuDeOutraSessao() throws Exception {
		logout(primeira, null).andExpect(status().isForbidden());
		logout(primeira, "incorreto").andExpect(status().isForbidden());
		logout(primeira, segunda.response().csrfToken()).andExpect(status().isForbidden());
	}

	@Test
	void beSai06LogoutEncerraSomenteSessaoAtual() throws Exception {
		logout(primeira, primeira.response().csrfToken()).andExpect(status().isNoContent());
		mockMvc.perform(get(ENDPOINT_CONSULTA).cookie(cookie(primeira))).andExpect(status().isUnauthorized());
		mockMvc.perform(get(ENDPOINT_CONSULTA).cookie(cookie(segunda))).andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions logout(SessaoCriada sessao, String csrf)
			throws Exception {
		var requisicao = delete(ENDPOINT_LOGOUT).cookie(cookie(sessao));
		if (csrf != null) requisicao.header(HEADER_CSRF, csrf);
		return mockMvc.perform(requisicao);
	}

	private Cookie cookie(SessaoCriada sessao) {
		return new Cookie("cc_session", sessao.token());
	}

	@TestConfiguration
	static class Configuracao {

		@Bean
		MutableClock clock() {
			return new MutableClock(Instant.parse("2026-07-23T12:00:00Z"));
		}
	}
}
