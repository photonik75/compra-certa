package br.leobarros.compracerta.categorias;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.categorias.CategoriaDtos.Category;
import br.leobarros.compracerta.categorias.CategoriaDtos.Collection;
import br.leobarros.compracerta.categorias.CategoriaDtos.Input;
import br.leobarros.compracerta.categorias.CategoriaDtos.PageInfo;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

	private static final Set<String> ICONS = Set.of("🥬", "🛍️", "🧃", "🧴", "🍞", "❄️", "🐾", "🛒");
	private final Clock clock;
	private final CategoriaRepository repository;
	private final IdempotenciaRepository idempotency;

	public CategoriaService(Clock clock, CategoriaRepository repository, IdempotenciaRepository idempotency) {
		this.clock = clock;
		this.repository = repository;
		this.idempotency = idempotency;
	}

	public Collection list(Conta account, String search, String cursor, Integer limit) {
		var normalizedSearch = search == null ? null : ApiSupport.normalize(search);
		if (normalizedSearch != null && ApiSupport.normalizeSpaces(search).length() > 40) {
			throw ApiSupport.validation("search", "A pesquisa deve ter no máximo 40 caracteres.");
		}
		var size = limit == null ? 30 : limit;
		if (size < 1 || size > 30) throw ApiSupport.validation("limit", "O limite deve estar entre 1 e 30.");
		var after = decode(cursor, normalizedSearch);
		var found = repository.list(account.getId(), normalizedSearch, after.name(), after.id(), size);
		var hasMore = found.size() > size;
		var items = found.subList(0, Math.min(size, found.size()));
		var next = hasMore ? encode(normalizedSearch, items.getLast()) : null;
		return new Collection(items, new PageInfo(next, hasMore));
	}

	@Transactional
	public Category create(Conta account, Input input, String key) {
		var values = validate(input);
		var content = values.name() + "|" + values.icon();
		var replay = idempotency.replay(account.getId(), "CATEGORY_CREATE", key, content);
		if (replay.isPresent()) return get(account, replay.orElseThrow().resourceId());
		if (repository.nameExists(account.getId(), ApiSupport.normalize(values.name()), null)) throw duplicate();
		var id = UUID.randomUUID();
		idempotency.begin(account.getId(), "CATEGORY_CREATE", key, content);
		repository.create(id, account.getId(), values.name(), values.icon(), clock.instant());
		idempotency.finish(account.getId(), "CATEGORY_CREATE", key, id, "CREATED");
		return get(account, id);
	}

	public Category get(Conta account, UUID id) {
		return repository.find(id, account.getId()).orElseThrow(ApiSupport::notFound);
	}

	@Transactional
	public Category update(Conta account, UUID id, Input input, long version) {
		var current = get(account, id);
		if (current.version() != version) throw ApiSupport.conflict(current.version());
		if (input == null || input.name() == null && input.icon() == null) {
			throw ApiSupport.validation("body", "Informe ao menos uma alteração.");
		}
		var values = validate(new Input(
				input.name() == null ? current.name() : input.name(),
				input.icon() == null ? current.icon() : input.icon()));
		if (values.name().equals(current.name()) && values.icon().equals(current.icon())) {
			throw ApiSupport.validation("body", "Informe ao menos uma alteração.");
		}
		if (repository.nameExists(account.getId(), ApiSupport.normalize(values.name()), id)) throw duplicate();
		if (repository.update(id, account.getId(), values.name(), values.icon(), clock.instant(), version) == 0) {
			throw ApiSupport.conflict(get(account, id).version());
		}
		return get(account, id);
	}

	@Transactional
	public void delete(Conta account, UUID id, long version) {
		var current = get(account, id);
		if (current.activeProductCount() > 0) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"CATEGORY_IN_USE",
					"Esta categoria possui " + current.activeProductCount()
							+ " produto(s) ativo(s). Mova ou desative esses produtos antes de excluí-la.");
		}
		if (current.version() != version) throw ApiSupport.conflict(current.version());
		if (repository.delete(id, account.getId(), version) == 0) throw ApiSupport.conflict(get(account, id).version());
	}

	private Input validate(Input input) {
		var name = ApiSupport.normalizeSpaces(input == null ? null : input.name());
		if (name.isEmpty()) throw ApiSupport.validation("name", "Por favor, informe o nome da categoria.");
		if (name.length() > 40) {
			throw ApiSupport.validation("name", "O nome da categoria deve ter no máximo 40 caracteres.");
		}
		var icon = input == null ? null : input.icon();
		if (!ICONS.contains(icon)) {
			throw ApiSupport.validation("icon", "Por favor, escolha um ícone disponível.");
		}
		return new Input(name, icon);
	}

	private ApiException duplicate() {
		return new ApiException(
				HttpStatus.CONFLICT,
				"CATEGORY_NAME_ALREADY_IN_USE",
				"Você já possui uma categoria com este nome.");
	}

	private Cursor decode(String cursor, String search) {
		if (cursor == null) return new Cursor(null, null);
		try {
			var text = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			var parts = text.split("\\|", -1);
			if (parts.length != 3 || !parts[0].equals(search == null ? "" : search)) throw new Exception();
			return new Cursor(parts[1], UUID.fromString(parts[2]));
		} catch (Exception exception) {
			throw ApiSupport.validation("cursor", "O cursor informado não é válido para esta consulta.");
		}
	}

	private String encode(String search, Category category) {
		var value = (search == null ? "" : search) + "|" + category.name().toLowerCase() + "|" + category.id();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private record Cursor(String name, UUID id) {
	}
}
