package br.leobarros.compracerta.integration.autenticacao.sessao;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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
import org.springframework.http.MediaType;
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
		SessaoControllerIntegrationTest.Configuracao.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebMvcTest(SessaoController.class)
class SessaoControllerIntegrationTest {

	private static final String ENDPOINT = "/api/v1/auth/session";
	private static final Instant AGORA = Instant.parse("2026-07-23T12:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SessaoService sessaoService;

	private String token;

	@BeforeEach
	void criarSessao() {
		token = sessaoService.criarParaLogin(
				new Conta("Maria Silva", "maria@example.com", "hash-secreto"), false).token();
	}

	@Test
	void beSes06CookieDeSessaoPossuiAtributosDeSeguranca() throws Exception {
		mockMvc.perform(get(ENDPOINT).cookie(new Cookie("cc_session", token)))
				.andExpect(status().isOk())
				.andExpect(cookie().httpOnly("cc_session", true))
				.andExpect(cookie().secure("cc_session", true))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/api/v1")));
	}

	@Test
	void beSes07ConsultaRetornaDadosDaSessaoESemCache() throws Exception {
		mockMvc.perform(get(ENDPOINT).cookie(new Cookie("cc_session", token)))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", containsString("no-store")))
				.andExpect(jsonPath("$.user.email").value("maria@example.com"))
				.andExpect(jsonPath("$.csrfToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").isNotEmpty());
	}

	@Test
	void beSes08ConsultaSemCookieDesconhecidaOuExpiradaRetornaErroPolido() throws Exception {
		consultarSemAutenticacao();
		consultarSemAutenticacao(new Cookie("cc_session", "desconhecida"));
		sessaoService.revogar(token);
		consultarSemAutenticacao(new Cookie("cc_session", token));
	}

	@Test
	void beSes09RespostaNaoExpoeSegredos() throws Exception {
		mockMvc.perform(get(ENDPOINT).cookie(new Cookie("cc_session", token)))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("hash-secreto"))))
				.andExpect(content().string(not(containsString(token))))
				.andExpect(jsonPath("$.user.id").exists())
				.andExpect(jsonPath("$.user.name").exists())
				.andExpect(jsonPath("$.user.email").exists())
				.andExpect(jsonPath("$.user.status").exists())
				.andExpect(jsonPath("$.user.createdAt").exists());
	}

	private void consultarSemAutenticacao(Cookie... cookies) throws Exception {
		var requisicao = get(ENDPOINT);
		if (cookies.length > 0) requisicao.cookie(cookies);
		mockMvc.perform(requisicao)
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("SESSION_INVALID"))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@TestConfiguration
	static class Configuracao {

		@Bean
		MutableClock clock() {
			return new MutableClock(AGORA);
		}
	}
}
