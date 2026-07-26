package br.leobarros.compracerta.e2e;

import java.util.UUID;

import br.leobarros.compracerta.TestcontainersConfiguration;
import br.leobarros.compracerta.autenticacao.comum.Sha256;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class CompraCertaBackendE2ESupport {

	private static final String TOKEN = "sessao-de-integracao";
	private static final String CSRF = "csrf-de-integracao";
	private static final String COOKIE = "cc_session";
	private static final String HEADER_CSRF = "X-CSRF-Token";
	private static final String HEADER_IDEMPOTENCY = "Idempotency-Key";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper mapper;

	private UUID accountId;

	@BeforeEach
	void prepare() {
		jdbc.execute("""
				TRUNCATE TABLE operacoes_idempotentes, eventos_lista, convites_lista, itens_lista,
				participantes_lista, criacoes_lista_idempotentes, listas, produtos, categorias,
				idempotencias, tentativas_login, tokens_recuperacao, sessoes, contas CASCADE
				""");
		accountId = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO contas(id,nome,email,senha_hash,ativa) VALUES (?,?,?,?,TRUE)",
				accountId, "Ana", "ana@example.com", "hash");
		jdbc.update(
				"INSERT INTO sessoes(token_hash,csrf_token_hash,conta_id,criada_em,"
						+ "duracao_inatividade_segundos,expira_por_inatividade,expira_em_definitivo,revogada) "
						+ "VALUES (?,?,?,now(),43200,now()+interval '12 hours',now()+interval '24 hours',FALSE)",
				Sha256.base64(TOKEN), Sha256.base64(CSRF), accountId);
	}

	protected void beLis01A16GerenciaListasComIdempotenciaPaginacaoEConcorrencia() throws Exception {
		var created = mvc.perform(post("/api/v1/lists")
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "lista-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Compras do mês\",\"description\":\"Casa\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("ETag", "\"1\""))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.role").value("OWNER"))
				.andReturn();
		var id = json(created).get("id").asText();
		mvc.perform(get("/api/v1/lists").cookie(cookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(id))
				.andExpect(jsonPath("$.summary.activeLists").value(1));
		mvc.perform(patch("/api/v1/lists/{id}", id)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header("If-Match", "\"1\"")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Mercado\"}"))
				.andExpect(status().isOk())
				.andExpect(header().string("ETag", "\"2\""));
	}

	protected void beCat01A16CriaCategoriasIniciaisEProtegeUnicidadeEExclusao() throws Exception {
		mvc.perform(get("/api/v1/categories").cookie(cookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(4));
		var created = mvc.perform(post("/api/v1/categories")
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "categoria-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Padaria\",\"icon\":\"🍞\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("ETag", "\"1\""))
				.andReturn();
		var id = json(created).get("id").asText();
		mvc.perform(delete("/api/v1/categories/{id}", id)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header("If-Match", "\"1\""))
				.andExpect(status().isNoContent());
	}

	protected void beProd01A16MantemCatalogoIsoladoEHistoricoAoDesativar() throws Exception {
		var categoryId = categoryId();
		var created = createProduct(categoryId);
		var id = json(created).get("id").asText();
		mvc.perform(get("/api/v1/products?status=ACTIVE").cookie(cookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].name").value("Arroz"));
		mvc.perform(delete("/api/v1/products/{id}", id)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header("If-Match", "\"1\""))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/products?status=ACTIVE").cookie(cookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isEmpty());
	}

	protected void beItem01A16CriaEditaMarcaEMantemResumoAtomico() throws Exception {
		var listId = listId();
		var categoryId = categoryId();
		var productId = json(createProduct(categoryId)).get("id").asText();
		var created = createItem(listId, productId, categoryId);
		var itemId = json(created).at("/item/id").asText();
		mvc.perform(get("/api/v1/lists/{listId}/items", listId).cookie(cookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.listSummary.total").value(1))
				.andExpect(jsonPath("$.listSummary.pending").value(1));
		mvc.perform(patch("/api/v1/lists/{listId}/items/{itemId}", listId, itemId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "item-update-1")
						.header("If-Match", "\"1\"")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"quantity\":\"2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.item.quantity").value("2"));
	}

	protected void beShop01A13MarcaItemIdempotentementeEAtualizaAutoria() throws Exception {
		var listId = listId();
		var categoryId = categoryId();
		var productId = json(createProduct(categoryId)).get("id").asText();
		var itemId = json(createItem(listId, productId, categoryId)).at("/item/id").asText();
		mvc.perform(put("/api/v1/lists/{listId}/items/{itemId}/checked", listId, itemId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header("If-Match", "\"1\"")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"checked\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.item.checked").value(true))
				.andExpect(jsonPath("$.item.checkedBy.id").value(accountId.toString()))
				.andExpect(jsonPath("$.listSummary.checked").value(1));
	}

	protected void beLife01A13ConcluiReabreEExcluiComControleOtimista() throws Exception {
		var listId = listId();
		mvc.perform(put("/api/v1/lists/{listId}/status", listId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "life-complete")
						.header("If-Match", "\"1\"")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isNotEmpty());
		mvc.perform(put("/api/v1/lists/{listId}/status", listId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "life-reopen")
						.header("If-Match", "\"2\"")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"ACTIVE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	protected void beShare01A16AdicionaContaExistenteERevogaAcesso() throws Exception {
		var listId = listId();
		var memberId = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO contas(id,nome,email,senha_hash,ativa) VALUES (?,?,?,?,TRUE)",
				memberId, "Bia", "bia@example.com", "hash");
		var shared = mvc.perform(post("/api/v1/lists/{listId}/invitations", listId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "share-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"BIA@example.com\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.outcome").value("MEMBER_ADDED"))
				.andReturn();
		var membershipVersion = json(shared).at("/membership/version").asLong();
		mvc.perform(get("/api/v1/lists/{listId}/access", listId).cookie(cookie()))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.members[0].user.id").value(memberId.toString()));
		mvc.perform(delete("/api/v1/lists/{listId}/members/{userId}", listId, memberId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "share-remove-1")
						.header("If-Match", "\"" + membershipVersion + "\""))
				.andExpect(status().isNoContent());
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM participantes_lista WHERE lista_id=?",
				Integer.class, UUID.fromString(listId))).isZero();
	}

	private org.springframework.test.web.servlet.MvcResult createProduct(String categoryId) throws Exception {
		return mvc.perform(post("/api/v1/products")
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "produto-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Arroz","categoryId":"%s","defaultUnit":"PACKAGE"}
								""".formatted(categoryId)))
				.andExpect(status().isCreated()).andReturn();
	}

	private org.springframework.test.web.servlet.MvcResult createItem(
			String listId, String productId, String categoryId) throws Exception {
		return mvc.perform(post("/api/v1/lists/{listId}/items", listId)
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "item-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"productId":"%s","quantity":"1","unit":"PACKAGE","categoryId":"%s"}
								""".formatted(productId, categoryId)))
				.andExpect(status().isCreated()).andReturn();
	}

	private String listId() throws Exception {
		var result = mvc.perform(post("/api/v1/lists")
						.cookie(cookie())
						.header(HEADER_CSRF, CSRF)
						.header(HEADER_IDEMPOTENCY, "lista-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Lista de teste\"}"))
				.andExpect(status().isCreated()).andReturn();
		return json(result).get("id").asText();
	}

	private String categoryId() {
		return jdbc.queryForObject(
				"SELECT id FROM categorias WHERE conta_id=? ORDER BY nome LIMIT 1",
				UUID.class, accountId).toString();
	}

	private JsonNode json(org.springframework.test.web.servlet.MvcResult result) throws Exception {
		return mapper.readTree(result.getResponse().getContentAsString());
	}

	private jakarta.servlet.http.Cookie cookie() {
		return new jakarta.servlet.http.Cookie(COOKIE, TOKEN);
	}
}
