package br.leobarros.compracerta.produtos;

import java.time.Clock;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.produtos.ProdutoDtos.Collection;
import br.leobarros.compracerta.produtos.ProdutoDtos.Input;
import br.leobarros.compracerta.produtos.ProdutoDtos.PageInfo;
import br.leobarros.compracerta.produtos.ProdutoDtos.Product;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {
	private static final Set<String> UNITS = Set.of(
			"UNIT", "PACKAGE", "BOX", "BOTTLE", "FLASK", "CAN", "BAG", "TRAY", "DOZEN",
			"KILOGRAM", "GRAM", "LITER", "MILLILITER");
	private final Clock clock;
	private final ProdutoRepository repository;
	private final IdempotenciaRepository idempotency;
	public ProdutoService(Clock clock, ProdutoRepository repository, IdempotenciaRepository idempotency) {
		this.clock = clock;
		this.repository = repository;
		this.idempotency = idempotency;
	}
	public Collection list(
			Conta account, String search, UUID categoryId, String status, String cursor, Integer limit) {
		var state = status == null ? "ACTIVE" : status.toUpperCase(Locale.ROOT);
		if (!Set.of("ACTIVE", "INACTIVE", "ALL").contains(state)) {
			throw ApiSupport.validation("status", "Selecione um estado de produto válido.");
		}
		var normalized = search == null ? null : ApiSupport.normalize(search);
		if (search != null && ApiSupport.normalizeSpaces(search).length() > 60) {
			throw ApiSupport.validation("search", "A pesquisa deve ter no máximo 60 caracteres.");
		}
		if (categoryId != null && repository.category(categoryId, account.getId()).isEmpty()) {
			throw ApiSupport.notFound();
		}
		var size = limit == null ? 30 : limit;
		if (size < 1 || size > 30) throw ApiSupport.validation("limit", "O limite deve estar entre 1 e 30.");
		var offset = decode(cursor, normalized, categoryId, state);
		var found = repository.list(account.getId(), normalized, categoryId, state, size, offset);
		var more = found.size() > size;
		var items = found.subList(0, Math.min(size, found.size()));
		var next = more ? encode(normalized, categoryId, state, offset + size) : null;
		return new Collection(items, new PageInfo(next, more));
	}
	@Transactional
	public Product create(Conta account, Input input, String key) {
		var valid = validate(account, input);
		var content = valid.name() + "|" + valid.categoryId() + "|" + valid.defaultUnit();
		var replay = idempotency.replay(account.getId(), "PRODUCT_CREATE", key, content);
		if (replay.isPresent()) return get(account, replay.orElseThrow().resourceId());
		if (repository.nameExists(account.getId(), ApiSupport.normalize(valid.name()), null)) throw duplicate();
		var category = category(account, valid.categoryId());
		var id = UUID.randomUUID();
		idempotency.begin(account.getId(), "PRODUCT_CREATE", key, content);
		repository.create(id, account.getId(), valid, category, clock.instant());
		idempotency.finish(account.getId(), "PRODUCT_CREATE", key, id, "CREATED");
		return get(account, id);
	}
	public Product get(Conta account, UUID id) {
		return repository.find(id, account.getId()).orElseThrow(ApiSupport::notFound);
	}
	@Transactional
	public Product update(Conta account, UUID id, Input input, long version) {
		var current = get(account, id);
		if (!current.active()) {
			throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_INACTIVE", "Este produto está inativo.");
		}
		if (current.version() != version) throw ApiSupport.conflict(current.version());
		if (input == null || input.name() == null && input.categoryId() == null && input.defaultUnit() == null) {
			throw ApiSupport.validation("body", "Informe ao menos uma alteração.");
		}
		var valid = validate(account, new Input(
				input.name() == null ? current.name() : input.name(),
				input.categoryId() == null ? current.category().id() : input.categoryId(),
				input.defaultUnit() == null ? current.defaultUnit() : input.defaultUnit()));
		if (valid.name().equals(current.name()) && valid.categoryId().equals(current.category().id())
				&& valid.defaultUnit().equals(current.defaultUnit())) {
			throw ApiSupport.validation("body", "Informe ao menos uma alteração.");
		}
		if (repository.nameExists(account.getId(), ApiSupport.normalize(valid.name()), id)) throw duplicate();
		if (repository.update(id, account.getId(), valid, category(account, valid.categoryId()),
				clock.instant(), version) == 0) throw ApiSupport.conflict(get(account, id).version());
		return get(account, id);
	}
	@Transactional
	public void deactivate(Conta account, UUID id, long version) {
		var current = get(account, id);
		if (!current.active()) return;
		if (current.version() != version) throw ApiSupport.conflict(current.version());
		if (repository.deactivate(id, account.getId(), version) == 0) {
			throw ApiSupport.conflict(get(account, id).version());
		}
	}
	private Input validate(Conta account, Input input) {
		var name = ApiSupport.normalizeSpaces(input == null ? null : input.name());
		if (name.isEmpty()) throw ApiSupport.validation("name", "Por favor, informe o nome do produto.");
		if (name.length() > 60) {
			throw ApiSupport.validation("name", "O nome do produto deve ter no máximo 60 caracteres.");
		}
		if (input.categoryId() == null) {
			throw ApiSupport.validation("categoryId", "Por favor, escolha uma categoria.");
		}
		if (input.defaultUnit() == null || !UNITS.contains(input.defaultUnit())) {
			throw ApiSupport.validation("defaultUnit", "Por favor, escolha uma unidade disponível.");
		}
		return new Input(name, input.categoryId(), input.defaultUnit());
	}
	private ProdutoDtos.CategoryReference category(Conta account, UUID id) {
		var category = repository.category(id, account.getId()).orElseThrow(ApiSupport::notFound);
		if (!category.available()) {
			throw new ApiException(
					HttpStatus.CONFLICT, "CATEGORY_UNAVAILABLE",
					"A categoria selecionada não está mais disponível. Escolha outra categoria.");
		}
		return category;
	}
	private ApiException duplicate() {
		return new ApiException(
				HttpStatus.CONFLICT, "PRODUCT_NAME_ALREADY_IN_USE",
				"Você já possui um produto ativo com este nome.");
	}
	private int decode(String cursor, String search, UUID category, String status) {
		if (cursor == null) return 0;
		try {
			var text = new String(Base64.getUrlDecoder().decode(cursor));
			var prefix = (search == null ? "" : search) + "|" + (category == null ? "" : category) + "|" + status + "|";
			if (!text.startsWith(prefix)) throw new Exception();
			return Integer.parseInt(text.substring(prefix.length()));
		} catch (Exception exception) {
			throw ApiSupport.validation("cursor", "O cursor informado não é válido para esta consulta.");
		}
	}
	private String encode(String search, UUID category, String status, int offset) {
		var text = (search == null ? "" : search) + "|" + (category == null ? "" : category)
				+ "|" + status + "|" + offset;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes());
	}
}
