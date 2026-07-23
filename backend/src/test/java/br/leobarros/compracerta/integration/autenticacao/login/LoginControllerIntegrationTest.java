package br.leobarros.compracerta.integration.autenticacao.login;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.configuracao.AutenticacaoSecurityConfiguration;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.login.LoginContaRepository;
import br.leobarros.compracerta.autenticacao.login.LoginController;
import br.leobarros.compracerta.autenticacao.login.LoginService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCookieService;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoHttpResponseService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import({
		AutenticacaoSecurityConfiguration.class,
		ApiErrorResponseService.class,
		LoginService.class,
		SessaoCookieService.class,
		GeradorIdentificadorService.class,
		SessaoHttpResponseService.class,
		SessaoService.class,
		LoginControllerIntegrationTest.Configuracao.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebMvcTest(LoginController.class)
class LoginControllerIntegrationTest {

	private static final String ENDPOINT_LOGIN = "/api/v1/auth/sessions";
	private static final String EMAIL = "maria@example.com";
	private static final String SENHA_CORRETA = "senha segura";
	private static final String SENHA_INCORRETA = "senha errada";
	private static final String CORPO_LOGIN = """
			{
			  "email": "%s",
			  "password": "%s",
			  "manterConectado": false
			}
			""";
	private static final Instant AGORA = Instant.parse("2026-07-23T12:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoginContaRepositoryEmMemoria contaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void prepararConta() {
		contaRepository.salvar(new Conta("Maria Silva", EMAIL, passwordEncoder.encode(SENHA_CORRETA)));
	}

	@Test
	void beLog05EmailInexistenteESenhaIncorretaRetornamMesmoErro() throws Exception {
		var emailInexistente = autenticar("inexistente@example.com", SENHA_INCORRETA);
		var senhaIncorreta = autenticar(EMAIL, SENHA_INCORRETA);
		assertEquals(401, emailInexistente.getResponse().getStatus());
		assertEquals(401, senhaIncorreta.getResponse().getStatus());
		assertEquals(emailInexistente.getResponse().getContentAsString(),
				senhaIncorreta.getResponse().getContentAsString());
		assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, emailInexistente.getResponse().getContentType());
	}

	@Test
	void beLog10DuranteBloqueioRetornaRetryAfterEMensagemNormativa() throws Exception {
		for (var tentativa = 0; tentativa < 5; tentativa++) {
			assertEquals(401, autenticar(EMAIL, SENHA_INCORRETA).getResponse().getStatus());
		}
		mockMvc.perform(requisicaoLogin(EMAIL, SENHA_INCORRETA))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "900"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"))
				.andExpect(jsonPath("$.detail")
						.value("Muitas tentativas de acesso. Tente novamente em 15 minutos"));
	}

	@Test
	void beLog14LoginValidoRetornaSessaoCookieSeguroENaoPermiteCache() throws Exception {
		mockMvc.perform(requisicaoLogin(EMAIL, SENHA_CORRETA))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", containsString("no-store")))
				.andExpect(cookie().exists("cc_session"))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", containsString("Secure")))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/api/v1")))
				.andExpect(jsonPath("$.user.email").value(EMAIL))
				.andExpect(jsonPath("$.csrfToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").isNotEmpty());
	}

	private MvcResult autenticar(String email, String senha) throws Exception {
		return mockMvc.perform(requisicaoLogin(email, senha)).andReturn();
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requisicaoLogin(
			String email,
			String senha) {
		return post(ENDPOINT_LOGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CORPO_LOGIN.formatted(email, senha));
	}

	@TestConfiguration
	static class Configuracao {

		@Bean
		LoginContaRepositoryEmMemoria loginContaRepository() {
			return new LoginContaRepositoryEmMemoria();
		}

		@Bean
		PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		@Bean
		MutableClock clock() {
			return new MutableClock(AGORA);
		}
	}

	static class LoginContaRepositoryEmMemoria implements LoginContaRepository {

		private final Map<String, Conta> contas = new ConcurrentHashMap<>();

		@Override
		public Optional<Conta> buscarPorEmail(String email) {
			return Optional.ofNullable(contas.get(email));
		}

		void salvar(Conta conta) {
			contas.put(conta.getEmail(), conta);
		}

	}

}
