package br.leobarros.compracerta.itens;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.itens.ItemDtos.CheckResult;
import br.leobarros.compracerta.itens.ItemDtos.Collection;
import br.leobarros.compracerta.itens.ItemDtos.Deletion;
import br.leobarros.compracerta.itens.ItemDtos.Input;
import br.leobarros.compracerta.itens.ItemDtos.ListItem;
import br.leobarros.compracerta.itens.ItemDtos.Mutation;
import br.leobarros.compracerta.itens.ItemDtos.PageInfo;
import br.leobarros.compracerta.itens.ItemRepository.InputData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
	private static final BigDecimal MAX = new BigDecimal("999999.99");
	private static final Set<String> UNITS = Set.of(
			"UNIT", "PACKAGE", "BOX", "BOTTLE", "FLASK", "CAN", "BAG", "TRAY", "DOZEN",
			"KILOGRAM", "GRAM", "LITER", "MILLILITER");
	private final Clock clock;
	private final ItemRepository repository;
	private final IdempotenciaRepository idempotency;
	private final ListaEventService events;
	public ItemService(
			Clock clock, ItemRepository repository, IdempotenciaRepository idempotency,
			ListaEventService events) {
		this.clock = clock;
		this.repository = repository;
		this.idempotency = idempotency;
		this.events = events;
	}
	public Collection list(Conta account, UUID listId, String cursor, Integer limit) {
		var state = access(account, listId);
		var size = limit == null ? 30 : limit;
		if (size < 1 || size > 30) throw ApiSupport.validation("limit", "O limite deve estar entre 1 e 30.");
		var offset = decode(cursor, listId);
		var found = repository.listItems(listId, size, offset);
		var more = found.size() > size;
		var items = found.subList(0, Math.min(size, found.size()));
		var next = more ? Base64.getUrlEncoder().withoutPadding()
				.encodeToString((listId + "|" + (offset + size)).getBytes()) : null;
		return new Collection(items, new PageInfo(next, more), repository.summary(listId), state.version());
	}
	public ListItem get(Conta account, UUID listId, UUID itemId) {
		access(account, listId);
		return repository.find(listId, itemId).orElseThrow(ApiSupport::notFound);
	}
	@Transactional
	public Mutation create(Conta account, UUID listId, Input input, String key) {
		active(account, listId);
		var data = validate(account, input);
		var content = listId + "|" + data;
		var replay = idempotency.replay(account.getId(), "ITEM_CREATE_" + listId, key, content);
		if (replay.isPresent()) {
			var item = get(account, listId, replay.orElseThrow().resourceId());
			return mutation(replay.orElseThrow().result(), item, null, listId);
		}
		var duplicate = repository.duplicate(listId, ApiSupport.normalize(data.product().name()), null);
		if (duplicate.isPresent()) {
			if (!"MERGE".equals(input.resolution())) throw duplicate(duplicate.orElseThrow());
			if (!duplicate.orElseThrow().unit().equals(data.unit())) {
				throw new ApiException(
						HttpStatus.CONFLICT, "INCOMPATIBLE_UNITS",
						"Não é possível somar itens com unidades diferentes.");
			}
			var total = new BigDecimal(duplicate.orElseThrow().quantity()).add(data.quantity());
			if (total.compareTo(MAX) > 0) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST, "QUANTITY_LIMIT_EXCEEDED",
						"A quantidade total deve ser menor ou igual a 999999,99.");
			}
			idempotency.begin(account.getId(), "ITEM_CREATE_" + listId, key, content);
			repository.merge(
					duplicate.orElseThrow().id(), data.quantity(), clock.instant(),
					duplicate.orElseThrow().version());
			var version = repository.touchList(listId, clock.instant());
			idempotency.finish(
					account.getId(), "ITEM_CREATE_" + listId, key, duplicate.orElseThrow().id(), "MERGED");
			var merged = get(account, listId, duplicate.orElseThrow().id());
			events.publish(listId, version, merged.id(), "list.item.updated", merged);
			return mutation("MERGED", merged, null, listId);
		}
		var id = UUID.randomUUID();
		idempotency.begin(account.getId(), "ITEM_CREATE_" + listId, key, content);
		repository.create(id, listId, account.getId(), data, clock.instant());
		var version = repository.touchList(listId, clock.instant());
		idempotency.finish(account.getId(), "ITEM_CREATE_" + listId, key, id, "CREATED");
		var created = get(account, listId, id);
		events.publish(listId, version, id, "list.item.created", created);
		return mutation("CREATED", created, null, listId);
	}
	@Transactional
	public Mutation update(Conta account, UUID listId, UUID itemId, Input input, long version, String key) {
		active(account, listId);
		var current = get(account, listId, itemId);
		if (current.version() != version) throw ApiSupport.conflict(current.version());
		if (input == null) throw ApiSupport.validation("body", "Informe ao menos uma alteração.");
		var effective = new Input(
				input.productId() == null ? current.product().id() : input.productId(),
				input.quantity() == null ? current.quantity() : input.quantity(),
				input.unit() == null ? current.unit() : input.unit(),
				input.categoryId() == null ? current.category().id() : input.categoryId(),
				input.notes() == null ? current.notes() : input.notes(),
				input.resolution(),
				input.targetVersion());
		var data = validate(account, effective);
		var content = listId + "|" + itemId + "|" + data + "|" + version;
		var replay = idempotency.replay(account.getId(), "ITEM_UPDATE_" + itemId, key, content);
		if (replay.isPresent()) {
			var item = get(account, listId, replay.orElseThrow().resourceId());
			return mutation(replay.orElseThrow().result(), item, null, listId);
		}
		var duplicate = repository.duplicate(listId, ApiSupport.normalize(data.product().name()), itemId);
		if (duplicate.isPresent()) {
			if (!"MERGE".equals(effective.resolution())) throw duplicate(duplicate.orElseThrow());
			if (!duplicate.orElseThrow().unit().equals(data.unit())) {
				throw new ApiException(
						HttpStatus.CONFLICT, "INCOMPATIBLE_UNITS",
						"Não é possível mesclar itens com unidades diferentes.");
			}
			idempotency.begin(account.getId(), "ITEM_UPDATE_" + itemId, key, content);
			repository.merge(
					duplicate.orElseThrow().id(), data.quantity(), clock.instant(),
					effective.targetVersion() == null
							? duplicate.orElseThrow().version() : effective.targetVersion());
			repository.delete(itemId, version);
			var listVersion = repository.touchList(listId, clock.instant());
			idempotency.finish(
					account.getId(), "ITEM_UPDATE_" + itemId, key, duplicate.orElseThrow().id(), "MERGED");
			var merged = get(account, listId, duplicate.orElseThrow().id());
			events.publish(listId, listVersion, itemId, "list.item.updated", merged);
			return mutation("MERGED", merged, itemId, listId);
		}
		idempotency.begin(account.getId(), "ITEM_UPDATE_" + itemId, key, content);
		if (repository.update(itemId, data, clock.instant(), version) == 0) {
			throw ApiSupport.conflict(get(account, listId, itemId).version());
		}
		var listVersion = repository.touchList(listId, clock.instant());
		idempotency.finish(account.getId(), "ITEM_UPDATE_" + itemId, key, itemId, "UPDATED");
		var updated = get(account, listId, itemId);
		events.publish(listId, listVersion, itemId, "list.item.updated", updated);
		return mutation("UPDATED", updated, null, listId);
	}
	@Transactional
	public Deletion delete(Conta account, UUID listId, UUID itemId, long version, String key) {
		active(account, listId);
		var content = listId + "|" + itemId + "|" + version;
		var replay = idempotency.replay(account.getId(), "ITEM_DELETE_" + itemId, key, content);
		if (replay.isPresent()) return deletion(itemId, listId);
		var item = get(account, listId, itemId);
		if (item.version() != version) throw ApiSupport.conflict(item.version());
		idempotency.begin(account.getId(), "ITEM_DELETE_" + itemId, key, content);
		repository.delete(itemId, version);
		var listVersion = repository.touchList(listId, clock.instant());
		idempotency.finish(account.getId(), "ITEM_DELETE_" + itemId, key, itemId, "DELETED");
		events.publish(listId, listVersion, itemId, "list.item.deleted", java.util.Map.of("itemId", itemId));
		return deletion(itemId, listId);
	}
	@Transactional
	public CheckResult check(
			Conta account, UUID listId, UUID itemId, Boolean checked, long version, String key) {
		active(account, listId);
		if (checked == null) throw ApiSupport.validation("checked", "Informe o estado do item.");
		var content = listId + "|" + itemId + "|" + checked + "|" + version;
		var scope = "ITEM_CHECK_" + itemId;
		if (idempotency.replay(account.getId(), scope, key, content).isPresent()) {
			return checkResult(get(account, listId, itemId), listId);
		}
		var item = get(account, listId, itemId);
		if (item.version() != version) throw ApiSupport.conflict(item.version());
		if (item.checked() == checked) return checkResult(item, listId);
		idempotency.begin(account.getId(), scope, key, content);
		repository.check(itemId, checked, account.getId(), clock.instant(), version);
		var listVersion = repository.touchList(listId, clock.instant());
		idempotency.finish(account.getId(), scope, key, itemId, checked ? "CHECKED" : "UNCHECKED");
		var updated = get(account, listId, itemId);
		events.publish(listId, listVersion, itemId, "list.item.checked", updated);
		return checkResult(updated, listId);
	}
	public ItemRepository.ListState access(Conta account, UUID listId) {
		return repository.list(listId, account.getId()).orElseThrow(ApiSupport::notFound);
	}
	private ItemRepository.ListState active(Conta account, UUID listId) {
		var state = access(account, listId);
		if ("COMPLETED".equals(state.status())) {
			throw new ApiException(
					HttpStatus.CONFLICT, "LIST_COMPLETED",
					"Esta lista está concluída e não pode ser alterada.");
		}
		return state;
	}
	private InputData validate(Conta account, Input input) {
		if (input == null || input.productId() == null) {
			throw ApiSupport.validation("productId", "Por favor, escolha um produto.");
		}
		if (input.categoryId() == null) {
			throw ApiSupport.validation("categoryId", "Por favor, escolha uma categoria.");
		}
		BigDecimal quantity;
		try {
			quantity = new BigDecimal(input.quantity());
		} catch (RuntimeException exception) {
			throw ApiSupport.validation("quantity", "Informe uma quantidade maior que zero.");
		}
		if (quantity.signum() <= 0) {
			throw ApiSupport.validation("quantity", "Informe uma quantidade maior que zero.");
		}
		if (quantity.compareTo(MAX) > 0) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST, "QUANTITY_LIMIT_EXCEEDED",
					"A quantidade deve ser menor ou igual a 999999,99.");
		}
		if (!UNITS.contains(input.unit())) {
			throw ApiSupport.validation("unit", "Por favor, escolha uma unidade disponível.");
		}
		if (input.notes() != null && input.notes().length() > 240) {
			throw ApiSupport.validation("notes", "A observação deve ter no máximo 240 caracteres.");
		}
		var product = repository.product(input.productId(), input.categoryId(), account.getId())
				.orElseThrow(ApiSupport::notFound);
		return new InputData(product, quantity, input.unit(), input.notes());
	}
	private ApiException duplicate(ListItem item) {
		return new ApiException(
				HttpStatus.CONFLICT, "DUPLICATE_ITEM",
				"Este produto já está na lista. Escolha editar o item existente ou somar a quantidade.");
	}
	private Mutation mutation(String outcome, ListItem item, UUID removed, UUID listId) {
		return new Mutation(outcome, item, removed, repository.summary(listId), repository.listVersion(listId));
	}
	private Deletion deletion(UUID itemId, UUID listId) {
		return new Deletion(itemId, repository.summary(listId), repositoryVersionDirect(listId));
	}
	private CheckResult checkResult(ListItem item, UUID listId) {
		return new CheckResult(item, repository.summary(listId), repositoryVersionDirect(listId));
	}
	private long repositoryVersionDirect(UUID listId) {
		return repository.listVersion(listId);
	}
	private int decode(String cursor, UUID listId) {
		if (cursor == null) return 0;
		try {
			var text = new String(Base64.getUrlDecoder().decode(cursor));
			var prefix = listId + "|";
			if (!text.startsWith(prefix)) throw new Exception();
			return Integer.parseInt(text.substring(prefix.length()));
		} catch (Exception exception) {
			throw ApiSupport.validation("cursor", "O cursor informado não é válido para esta lista.");
		}
	}
}
