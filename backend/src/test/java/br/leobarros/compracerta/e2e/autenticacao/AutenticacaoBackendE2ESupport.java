package br.leobarros.compracerta.e2e.autenticacao;

import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import br.leobarros.compracerta.CompraCertaBackendApplication;
import br.leobarros.compracerta.TestcontainersConfiguration;
import br.leobarros.compracerta.autenticacao.recuperacao.EntregaRecuperacaoSenha;
import br.leobarros.compracerta.autenticacao.comum.Sha256;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
		TestcontainersConfiguration.class,
		AutenticacaoBackendE2ESupport.EntregaTestConfiguration.class
})
@SpringBootTest
@AutoConfigureMockMvc
abstract class AutenticacaoBackendE2ESupport {

	private static final String CADASTRO = "/api/v1/auth/registrations";
	private static final String LOGIN = "/api/v1/auth/sessions";
	private static final String SESSAO = "/api/v1/auth/session";
	private static final String LOGOUT = "/api/v1/auth/sessions/current";
	private static final String SOLICITAR_RECUPERACAO = "/api/v1/auth/password-reset-requests";
	private static final String REDEFINIR_SENHA = "/api/v1/auth/password-resets";
	private static final String HEADER_IDEMPOTENCIA = "Idempotency-Key";
	private static final String HEADER_CSRF = "X-CSRF-Token";
	private static final String SENHA = "senha forte 123";
	private static final String NOVA_SENHA = "nova senha forte 456";
	private static final String EMAIL = "pessoa@example.com";
	private static final String NOME = "  Pessoa da Silva  ";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntregaCapturadora entrega;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void limparBanco() {
		jdbc.execute("""
				TRUNCATE TABLE idempotencias, tentativas_login, tokens_recuperacao, sessoes, contas
				RESTART IDENTITY CASCADE
				""");
		entrega.limpar();
	}

	void beE2e02CadastroPersisteContaNormalizadaEAtiva() throws Exception {
		cadastrar(EMAIL.toUpperCase(), "cadastro-02").andExpect(status().isCreated());
		var conta = conta(EMAIL);
		assertThat(conta)
				.containsEntry("nome", NOME)
				.containsEntry("email", EMAIL)
				.containsEntry("ativa", true);
		assertThat(quantidade("contas")).isOne();
	}

	void beE2e03CadastroPersisteSomenteHashForteDaSenha() throws Exception {
		cadastrar(EMAIL, "cadastro-03").andExpect(status().isCreated());
		var hash = (String) conta(EMAIL).get("senha_hash");
		assertThat(hash).doesNotContain(SENHA).doesNotContain("confirmation");
		assertThat(passwordEncoder.matches(SENHA, hash)).isTrue();
		assertThat(colunas("contas")).doesNotContain("senha", "password", "confirmacao", "password_confirmation");
	}

	void beE2e04BancoGaranteUnicidadeConcorrenteDoEmailNormalizado() throws Exception {
		try (var executor = Executors.newFixedThreadPool(2)) {
			var inicio = new java.util.concurrent.CountDownLatch(1);
			Future<Integer> primeira = executor.submit(() -> cadastrarConcorrente("Pessoa@Example.com", "conc-1", inicio));
			Future<Integer> segunda = executor.submit(() -> cadastrarConcorrente("pessoa@example.com", "conc-2", inicio));
			inicio.countDown();
			assertThat(List.of(primeira.get(), segunda.get())).containsExactlyInAnyOrder(201, 409);
		}
		assertThat(quantidade("contas")).isOne();
	}

	void beE2e05FalhaAoCriarSessaoReverteCadastro() throws Exception {
		criarFalhaDeSessao();
		try {
			cadastrar(EMAIL, "cadastro-05").andExpect(status().is5xxServerError());
			assertThat(quantidade("contas")).isZero();
			assertThat(quantidade("sessoes")).isZero();
		} finally {
			removerFalhaDeSessao();
		}
	}

	void beE2e06IdempotenciaDeCadastroSobreviveANovaRequisicao() throws Exception {
		var primeira = cadastrar(EMAIL, "cadastro-06").andExpect(status().isCreated()).andReturn();
		var segunda = cadastrar(EMAIL, "cadastro-06").andExpect(status().isCreated()).andReturn();
		assertThat(segunda.getResponse().getContentAsString())
				.isEqualTo(primeira.getResponse().getContentAsString());
		assertThat(cookie(primeira)).isEqualTo(cookie(segunda));
		assertThat(quantidade("contas")).isOne();
		assertThat(quantidade("sessoes")).isOne();
		assertThat(quantidade("idempotencias")).isOne();
	}

	void beE2e07LoginUsaContaPersistidaESemEnumeracaoDeEmail() throws Exception {
		cadastrar(EMAIL, "cadastro-07");
		login(EMAIL, SENHA).andExpect(status().isOk());
		var senhaIncorreta = login(EMAIL, "senha incorreta").andExpect(status().isUnauthorized()).andReturn();
		var emailInexistente = login("ninguem@example.com", "senha incorreta")
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertThat(emailInexistente.getResponse().getContentAsString())
				.isEqualTo(senhaIncorreta.getResponse().getContentAsString());
	}

	void beE2e08BloqueioDeLoginPersisteEntreRequisicoes() throws Exception {
		cadastrar(EMAIL, "cadastro-08");
		for (int tentativa = 0; tentativa < 5; tentativa++) {
			login(EMAIL, "senha incorreta").andExpect(status().isUnauthorized());
		}
		login(EMAIL, SENHA)
				.andExpect(status().isTooManyRequests())
				.andExpect(result -> assertThat(result.getResponse().getHeader("Retry-After")).isNotBlank());
		assertThat(quantidade("tentativas_login")).isEqualTo(5);
	}

	void beE2e09SessaoPersistidaPodeSerConsultadaEmOutraRequisicao() throws Exception {
		var cadastro = cadastrar(EMAIL, "cadastro-09").andExpect(status().isCreated()).andReturn();
		var consulta = consultar(cookie(cadastro)).andExpect(status().isOk()).andReturn();
		assertThat(json(consulta).at("/user/email").asText()).isEqualTo(EMAIL);
		assertThat(quantidade("sessoes")).isOne();
		var expiraEm = jdbc.queryForObject("SELECT expira_em_definitivo FROM sessoes", Instant.class);
		var criadaEm = jdbc.queryForObject("SELECT criada_em FROM sessoes", Instant.class);
		assertThat(Duration.between(criadaEm, expiraEm)).isEqualTo(Duration.ofHours(24));
	}

	void beE2e10LogoutRevogaSomenteSessaoAtual() throws Exception {
		cadastrar(EMAIL, "cadastro-10");
		var primeira = login(EMAIL, SENHA).andReturn();
		var segunda = login(EMAIL, SENHA).andReturn();
		var consulta = consultar(cookie(primeira)).andReturn();
		mockMvc.perform(delete(LOGOUT)
						.cookie(new jakarta.servlet.http.Cookie("cc_session", cookie(primeira)))
						.header(HEADER_CSRF, json(consulta).get("csrfToken").asText()))
				.andExpect(status().isNoContent());
		consultar(cookie(primeira)).andExpect(status().isUnauthorized());
		consultar(cookie(segunda)).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM sessoes WHERE revogada", Long.class)).isOne();
	}

	void beE2e11RecuperacaoPersisteHashInvalidaAnteriorEEntregaUmaVez() throws Exception {
		cadastrar(EMAIL, "cadastro-11");
		solicitarRecuperacao(EMAIL, "rec-11-a").andExpect(status().isAccepted());
		var primeiroHash = jdbc.queryForObject(
				"SELECT token_hash FROM tokens_recuperacao WHERE invalidado = false",
				String.class);
		solicitarRecuperacao(EMAIL, "rec-11-b").andExpect(status().isAccepted());
		assertThat(entrega.links()).hasSize(2);
		var tokenAtual = tokenDaUltimaEntrega();
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM tokens_recuperacao WHERE invalidado",
				Long.class)).isOne();
		assertThat(jdbc.queryForObject(
				"SELECT token_hash FROM tokens_recuperacao WHERE invalidado = false",
				String.class)).isEqualTo(sha256(tokenAtual)).isNotEqualTo(primeiroHash);
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM tokens_recuperacao WHERE token_hash = ?",
				Long.class,
				tokenAtual)).isZero();
	}

	void beE2e12RecuperacaoDeEmailInexistenteEIndistinguivelESemEfeito() throws Exception {
		cadastrar(EMAIL, "cadastro-12");
		var existente = solicitarRecuperacao(EMAIL, "rec-12-a").andReturn().getResponse();
		entrega.limpar();
		jdbc.update("DELETE FROM tokens_recuperacao");
		var inexistente = solicitarRecuperacao("ninguem@example.com", "rec-12-b").andReturn().getResponse();
		assertThat(inexistente.getStatus()).isEqualTo(existente.getStatus()).isEqualTo(202);
		assertThat(inexistente.getContentAsString()).isEqualTo(existente.getContentAsString());
		assertThat(quantidade("tokens_recuperacao")).isZero();
		assertThat(entrega.links()).isEmpty();
	}

	void beE2e13RedefinicaoAlteraSenhaConsomeTokenERevogaSessoesAtomicamente() throws Exception {
		var sessao = prepararRedefinicao("13");
		redefinir(tokenDaUltimaEntrega(), NOVA_SENHA, "reset-13").andExpect(status().isNoContent());
		assertThat(passwordEncoder.matches(NOVA_SENHA, (String) conta(EMAIL).get("senha_hash"))).isTrue();
		assertThat(jdbc.queryForObject("SELECT usado FROM tokens_recuperacao", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM sessoes WHERE conta_id = ? AND revogada",
				Long.class,
				UUID.fromString(json(sessao).at("/user/id").asText()))).isGreaterThan(0L);
	}

	void beE2e14FalhaNaRedefinicaoReverteSenhaTokenESessoes() throws Exception {
		prepararRedefinicao("14");
		var hashAnterior = (String) conta(EMAIL).get("senha_hash");
		criarFalhaDeConsumoDoToken();
		try {
			redefinir(tokenDaUltimaEntrega(), NOVA_SENHA, "reset-14")
					.andExpect(status().is5xxServerError());
		} finally {
			removerFalhaDeConsumoDoToken();
		}
		assertThat(conta(EMAIL).get("senha_hash")).isEqualTo(hashAnterior);
		assertThat(jdbc.queryForObject("SELECT usado FROM tokens_recuperacao", Boolean.class)).isFalse();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM sessoes WHERE revogada", Long.class)).isZero();
	}

	void beE2e15AposRedefinicaoSomenteNovaSenhaCriaSessao() throws Exception {
		var sessaoAntiga = prepararRedefinicao("15");
		redefinir(tokenDaUltimaEntrega(), NOVA_SENHA, "reset-15").andExpect(status().isNoContent());
		consultar(cookie(sessaoAntiga)).andExpect(status().isUnauthorized());
		login(EMAIL, SENHA).andExpect(status().isUnauthorized());
		login(EMAIL, NOVA_SENHA).andExpect(status().isOk());
	}

	void beE2e16SchemaImpoeRestricoesCompativeisComODominio() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO contas (id, nome, email, senha_hash, ativa) VALUES (?, NULL, ?, ?, true)",
				UUID.randomUUID(),
				"restricao@example.com",
				"hash")).isInstanceOf(RuntimeException.class);
		jdbc.update(
				"INSERT INTO contas (id, nome, email, senha_hash, ativa) VALUES (?, ?, ?, ?, true)",
				UUID.randomUUID(),
				"Pessoa",
				"unico@example.com",
				"hash");
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO contas (id, nome, email, senha_hash, ativa) VALUES (?, ?, ?, ?, true)",
				UUID.randomUUID(),
				"Outra",
				"unico@example.com",
				"hash")).isInstanceOf(RuntimeException.class);
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO sessoes "
						+ "(token_hash, csrf_token_hash, conta_id, criada_em, duracao_inatividade_segundos, "
						+ "expira_por_inatividade, expira_em_definitivo, revogada) "
						+ "VALUES (?, ?, ?, now(), 1, now(), now(), false)",
				"token",
				"csrf",
				UUID.randomUUID())).isInstanceOf(RuntimeException.class);
	}

	void beE2e17DadosPermanecemDisponiveisEmNovoContexto() throws Exception {
		cadastrar(EMAIL, "cadastro-17");
		solicitarRecuperacao(EMAIL, "rec-17");
		var propriedades = propriedadesDoBanco();
		try (ConfigurableApplicationContext novoContexto = new SpringApplication(CompraCertaBackendApplication.class)
				.run(propriedades)) {
			var novoJdbc = novoContexto.getBean(JdbcTemplate.class);
			assertThat(novoJdbc.queryForObject("SELECT count(*) FROM contas", Long.class)).isOne();
			assertThat(novoJdbc.queryForObject("SELECT count(*) FROM sessoes", Long.class)).isOne();
			assertThat(novoJdbc.queryForObject("SELECT count(*) FROM idempotencias", Long.class)).isEqualTo(2);
			assertThat(novoJdbc.queryForObject("SELECT count(*) FROM tokens_recuperacao", Long.class)).isOne();
		}
	}

	private org.springframework.test.web.servlet.ResultActions cadastrar(String email, String chave) throws Exception {
		return mockMvc.perform(post(CADASTRO)
				.header(HEADER_IDEMPOTENCIA, chave)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"%s","email":"%s","password":"%s","passwordConfirmation":"%s"}
						""".formatted(NOME, email, SENHA, SENHA)));
	}

	private int cadastrarConcorrente(
			String email,
			String chave,
			java.util.concurrent.CountDownLatch inicio) throws Exception {
		inicio.await();
		return cadastrar(email, chave).andReturn().getResponse().getStatus();
	}

	private org.springframework.test.web.servlet.ResultActions login(String email, String senha) throws Exception {
		return mockMvc.perform(post(LOGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s","manterConectado":false}
						""".formatted(email, senha)));
	}

	private org.springframework.test.web.servlet.ResultActions consultar(String token) throws Exception {
		return mockMvc.perform(get(SESSAO).cookie(new jakarta.servlet.http.Cookie("cc_session", token)));
	}

	private org.springframework.test.web.servlet.ResultActions solicitarRecuperacao(String email, String chave)
			throws Exception {
		return mockMvc.perform(post(SOLICITAR_RECUPERACAO)
				.header(HEADER_IDEMPOTENCIA, chave)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s"}
						""".formatted(email)));
	}

	private org.springframework.test.web.servlet.ResultActions redefinir(String token, String senha, String chave)
			throws Exception {
		return mockMvc.perform(post(REDEFINIR_SENHA)
				.header(HEADER_IDEMPOTENCIA, chave)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s","newPassword":"%s","passwordConfirmation":"%s"}
						""".formatted(token, senha, senha)));
	}

	private MvcResult prepararRedefinicao(String sufixo) throws Exception {
		cadastrar(EMAIL, "cadastro-" + sufixo);
		var sessao = login(EMAIL, SENHA).andReturn();
		solicitarRecuperacao(EMAIL, "rec-" + sufixo).andExpect(status().isAccepted());
		return sessao;
	}

	private Map<String, Object> conta(String email) {
		return jdbc.queryForMap(
				"SELECT nome, email, senha_hash, ativa FROM contas WHERE email = ?",
				email);
	}

	private long quantidade(String tabela) {
		var tabelas = Map.of(
				"contas", "SELECT count(*) FROM contas",
				"sessoes", "SELECT count(*) FROM sessoes",
				"idempotencias", "SELECT count(*) FROM idempotencias",
				"tentativas_login", "SELECT count(*) FROM tentativas_login",
				"tokens_recuperacao", "SELECT count(*) FROM tokens_recuperacao");
		return jdbc.queryForObject(tabelas.get(tabela), Long.class);
	}

	private List<String> colunas(String tabela) throws Exception {
		var resultado = new ArrayList<String>();
		DatabaseMetaData metadata = dataSource.getConnection().getMetaData();
		try (var colunas = metadata.getColumns(null, null, tabela, null)) {
			while (colunas.next()) {
				resultado.add(colunas.getString("COLUMN_NAME"));
			}
		}
		return resultado;
	}

	private String cookie(MvcResult result) {
		MockHttpServletResponse response = result.getResponse();
		return response.getCookie("cc_session").getValue();
	}

	private JsonNode json(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String tokenDaUltimaEntrega() {
		var link = entrega.links().getLast();
		return link.substring(link.indexOf("#token=") + 7);
	}

	private String sha256(String valor) throws Exception {
		return Sha256.hex(valor);
	}

	private void criarFalhaDeSessao() {
		jdbc.execute("""
				CREATE OR REPLACE FUNCTION falhar_insercao_sessao() RETURNS trigger AS $$
				BEGIN RAISE EXCEPTION 'falha controlada'; END;
				$$ LANGUAGE plpgsql
				""");
		jdbc.execute("""
				CREATE TRIGGER falha_sessao BEFORE INSERT ON sessoes
				FOR EACH ROW EXECUTE FUNCTION falhar_insercao_sessao()
				""");
	}

	private void removerFalhaDeSessao() {
		jdbc.execute("DROP TRIGGER IF EXISTS falha_sessao ON sessoes");
		jdbc.execute("DROP FUNCTION IF EXISTS falhar_insercao_sessao()");
	}

	private void criarFalhaDeConsumoDoToken() {
		jdbc.execute("""
				CREATE OR REPLACE FUNCTION falhar_consumo_token() RETURNS trigger AS $$
				BEGIN
				    IF NEW.usado THEN RAISE EXCEPTION 'falha controlada'; END IF;
				    RETURN NEW;
				END;
				$$ LANGUAGE plpgsql
				""");
		jdbc.execute("""
				CREATE TRIGGER falha_token BEFORE UPDATE ON tokens_recuperacao
				FOR EACH ROW EXECUTE FUNCTION falhar_consumo_token()
				""");
	}

	private void removerFalhaDeConsumoDoToken() {
		jdbc.execute("DROP TRIGGER IF EXISTS falha_token ON tokens_recuperacao");
		jdbc.execute("DROP FUNCTION IF EXISTS falhar_consumo_token()");
	}

	private String[] propriedadesDoBanco() throws Exception {
		try (var conexao = dataSource.getConnection()) {
			return new String[] {
					"--spring.datasource.url=" + conexao.getMetaData().getURL(),
					"--spring.datasource.username=" + conexao.getMetaData().getUserName(),
					"--spring.datasource.password=test",
					"--server.port=0"
			};
		}
	}

	@TestConfiguration
	static class EntregaTestConfiguration {

		@Bean
		@Primary
		EntregaCapturadora entregaCapturadora() {
			return new EntregaCapturadora();
		}
	}

	static class EntregaCapturadora implements EntregaRecuperacaoSenha {

		private final List<String> links = new ArrayList<>();

		@Override
		public void enviar(String email, String link) {
			links.add(link);
		}

		List<String> links() {
			return List.copyOf(links);
		}

		void limpar() {
			links.clear();
		}
	}
}
