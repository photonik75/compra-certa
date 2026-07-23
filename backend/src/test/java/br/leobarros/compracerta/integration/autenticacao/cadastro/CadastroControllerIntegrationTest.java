package br.leobarros.compracerta.integration.autenticacao.cadastro;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import br.leobarros.compracerta.autenticacao.cadastro.CadastroController;
import br.leobarros.compracerta.autenticacao.cadastro.CadastroService;
import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.cadastro.ContaRepository;
import br.leobarros.compracerta.autenticacao.comum.idempotencia.IdempotenciaService;
import br.leobarros.compracerta.autenticacao.configuracao.AutenticacaoSecurityConfiguration;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCookieService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoHttpResponseService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoRepository;
import jakarta.servlet.ServletException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import({
		AutenticacaoSecurityConfiguration.class,
		ApiErrorResponseService.class,
		CadastroService.class,
		IdempotenciaService.class,
		SessaoCookieService.class,
		GeradorIdentificadorService.class,
		SessaoHttpResponseService.class,
		SessaoService.class,
		SessaoRepository.class,
		CadastroControllerIntegrationTest.Configuracao.class
})
@WebMvcTest(CadastroController.class)
class CadastroControllerIntegrationTest {

	private static final String ENDPOINT_CADASTRO = "/api/v1/auth/registrations";
	private static final String HEADER_CHAVE_IDEMPOTENCIA = "Idempotency-Key";
	private static final String CHAVE_IDEMPOTENCIA = "cadastro-entrada-invalida";
	private static final String EMAIL = "maria@example.com";
	private static final String EMAIL_COM_MAIUSCULAS = "Maria@EXAMPLE.COM";
	private static final String SENHA = "senha segura";
	private static final String CORPO_VALIDO = """
			{
			  "name": "Maria Silva",
			  "email": "%s",
			  "password": "%s",
			  "passwordConfirmation": "%s"
			}
			""";
	private static final String CORPO_INVALIDO = """
			{
			  "name": "",
			  "email": "email-invalido",
			  "password": "curta",
			  "passwordConfirmation": ""
			}
			""";
	private static final String CODIGO_CONFLITO = "CONFLICT";
	private static final String MENSAGEM_EMAIL_CADASTRADO = "E-mail já foi cadastrado";
	private static final String CODIGO_ERRO_VALIDACAO = "VALIDATION_ERROR";
	private static final String DETALHE_ERRO_VALIDACAO = "Verifique os campos informados e tente novamente.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContaRepositoryObservavel contaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void limparRepositorio() {
		contaRepository.limpar();
	}

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

	@Test
	void beCad11RetornaConflitoQuandoEmailJaFoiCadastradoComOutraCaixa() throws Exception {
		cadastrar(EMAIL_COM_MAIUSCULAS, "primeiro-cadastro").andExpect(status().isCreated());
		cadastrar(EMAIL, "segundo-cadastro")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(CODIGO_CONFLITO))
				.andExpect(jsonPath("$.detail").value(MENSAGEM_EMAIL_CADASTRADO))
				.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value(MENSAGEM_EMAIL_CADASTRADO));
	}

	@Test
	void beCad12DisputaDeCadastrosComMesmoEmailCriaSomenteUmaConta() throws Exception {
		try (var executor = Executors.newFixedThreadPool(2)) {
			var primeiraResposta = executor.submit(() -> cadastrarConcorrentemente("cadastro-concorrente-1"));
			var segundaResposta = executor.submit(() -> cadastrarConcorrentemente("cadastro-concorrente-2"));
			var statuses = List.of(
					primeiraResposta.get().getResponse().getStatus(),
					segundaResposta.get().getResponse().getStatus());
			assertEquals(1, statuses.stream().filter(status -> status == 201).count());
			assertEquals(1, statuses.stream().filter(status -> status == 409).count());
			assertEquals(1, contaRepository.quantidadeDeContas());
		}
	}

	@Test
	void beCad13FalhaDuranteCadastroNaoDeixaContaParcialmentePersistida() throws Exception {
		contaRepository.falharDepoisDePersistir();
		try {
			cadastrar(EMAIL, "cadastro-com-falha").andExpect(status().isInternalServerError());
		} catch (ServletException exception) {
			assertInstanceOf(IllegalStateException.class, exception.getCause());
		}
		assertEquals(0, contaRepository.quantidadeDeContas());
	}

	@Test
	void beCad14PersisteSomenteHashForteENaoReversivelDaSenha() throws Exception {
		cadastrar(EMAIL, "cadastro-seguro").andExpect(status().isCreated());
		var conta = contaRepository.unicaConta();
		assertFalse(conta.getSenhaHash().contains(SENHA));
		assertTrue(passwordEncoder.matches(SENHA, conta.getSenhaHash()));
		assertFalse(conta.getSenhaHash().equals(SENHA));
	}

	@Test
	void beCad15CadastroValidoRetornaSessaoCookieSeguroENaoPermiteCache() throws Exception {
		cadastrar(EMAIL, "cadastro-valido")
				.andExpect(status().isCreated())
				.andExpect(header().string("Cache-Control", containsString("no-store")))
				.andExpect(cookie().exists("cc_session"))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", containsString("Secure")))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/api/v1")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.user").isMap())
				.andExpect(jsonPath("$.csrfToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").isNotEmpty());
	}

	@Test
	void beCad16UsuarioDaSessaoPossuiSomenteOsCamposDocumentados() throws Exception {
		cadastrar(EMAIL, "cadastro-resposta-do-usuario")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user", aMapWithSize(5)))
				.andExpect(jsonPath("$.user", hasKey("id")))
				.andExpect(jsonPath("$.user", hasKey("name")))
				.andExpect(jsonPath("$.user", hasKey("email")))
				.andExpect(jsonPath("$.user", hasKey("status")))
				.andExpect(jsonPath("$.user", hasKey("createdAt")));
	}

	@Test
	void beIdc01RejeitaChaveIdempotenteAusenteVaziaOuMaiorQue255Caracteres() throws Exception {
		mockMvc.perform(post(ENDPOINT_CADASTRO)
						.contentType(MediaType.APPLICATION_JSON)
						.content(CORPO_VALIDO.formatted(EMAIL, SENHA, SENHA)))
				.andExpect(status().isBadRequest());
		cadastrar(EMAIL, "").andExpect(status().isBadRequest());
		cadastrar(EMAIL, "a".repeat(256)).andExpect(status().isBadRequest());
	}

	@Test
	void beIdc02RepeteResultadoOriginalSemNovaContaOuSessao() throws Exception {
		var primeiraResposta = cadastrar(EMAIL, "mesma-chave").andExpect(status().isCreated()).andReturn();
		var segundaResposta = cadastrar(EMAIL, "mesma-chave").andExpect(status().isCreated()).andReturn();
		assertEquals(primeiraResposta.getResponse().getContentAsString(),
				segundaResposta.getResponse().getContentAsString());
		assertEquals(primeiraResposta.getResponse().getHeader("Set-Cookie"),
				segundaResposta.getResponse().getHeader("Set-Cookie"));
		assertEquals(1, contaRepository.quantidadeDeContas());
	}

	@Test
	void beIdc03RejeitaMesmaChaveComConteudoDiferente() throws Exception {
		cadastrar(EMAIL, "chave-reutilizada").andExpect(status().isCreated());
		cadastrar("outra@example.com", "chave-reutilizada")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").isNotEmpty())
				.andExpect(jsonPath("$.detail").value(not(blankOrNullString())));
	}

	@Test
	void beIdc04ProcessaChavesDiferentesIndependentemente() throws Exception {
		cadastrar(EMAIL, "chave-independente-1").andExpect(status().isCreated());
		cadastrar("outra@example.com", "chave-independente-2").andExpect(status().isCreated());
		assertEquals(2, contaRepository.quantidadeDeContas());
	}

	private org.springframework.test.web.servlet.ResultActions cadastrar(String email, String chave) throws Exception {
		return mockMvc.perform(post(ENDPOINT_CADASTRO)
				.header(HEADER_CHAVE_IDEMPOTENCIA, chave)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CORPO_VALIDO.formatted(email, SENHA, SENHA)));
	}

	private MvcResult cadastrarConcorrentemente(String chave) throws Exception {
		return cadastrar(EMAIL, chave).andReturn();
	}

	@TestConfiguration
	static class Configuracao {

		@Bean
		ContaRepositoryObservavel contaRepository() {
			return new ContaRepositoryObservavel();
		}

		@Bean
		PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		@Bean
		Clock clock() {
			return Clock.systemUTC();
		}
	}

	static class ContaRepositoryObservavel implements ContaRepository {

		private final List<Conta> contas = new CopyOnWriteArrayList<>();
		private volatile boolean falharDepoisDePersistir;

		@Override
		public boolean existePorEmail(String email) {
			return contas.stream().anyMatch(conta -> conta.getEmail().equals(email));
		}

		@Override
		public void salvar(Conta conta) {
			contas.add(conta);
			if (falharDepoisDePersistir) {
				throw new IllegalStateException("Falha simulada após persistir a conta");
			}
		}

		@Override
		public void remover(Conta conta) {
			contas.remove(conta);
		}

		void limpar() {
			contas.clear();
			falharDepoisDePersistir = false;
		}

		void falharDepoisDePersistir() {
			falharDepoisDePersistir = true;
		}

		int quantidadeDeContas() {
			return contas.size();
		}

		Conta unicaConta() {
			assertEquals(1, contas.size());
			return contas.getFirst();
		}
	}
}
