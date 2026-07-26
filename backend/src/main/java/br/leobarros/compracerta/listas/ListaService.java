package br.leobarros.compracerta.listas;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.comum.Sha256;
import br.leobarros.compracerta.autenticacao.comum.idempotencia.ChaveIdempotenciaReutilizadaException;
import br.leobarros.compracerta.listas.ListaDtos.CreateListRequest;
import br.leobarros.compracerta.listas.ListaDtos.ListCollection;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaDtos.PageInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListaService {

	private static final String NOME_OBRIGATORIO = "Por favor, informe o nome da lista.";
	private static final String NOME_LONGO = "O nome da lista deve ter no máximo 60 caracteres.";
	private static final String DESCRICAO_LONGA = "A descrição deve ter no máximo 240 caracteres.";

	private final Clock clock;
	private final ListaRepository repository;

	public ListaService(Clock clock, ListaRepository repository) {
		this.clock = clock;
		this.repository = repository;
	}

	public ListCollection listar(Conta conta, String status, String search, String cursor, Integer limit) {
		var estado = status == null ? "ACTIVE" : status.toUpperCase(Locale.ROOT);
		if (!estado.equals("ACTIVE") && !estado.equals("COMPLETED") && !estado.equals("ALL")) {
			throw new ListaExceptions.Validacao("status", "Selecione um estado de lista válido.");
		}
		var pesquisa = search == null ? null : normalizarEspacos(search);
		if (pesquisa != null && pesquisa.length() > 60) {
			throw new ListaExceptions.Validacao("search", NOME_LONGO);
		}
		if (pesquisa != null && pesquisa.isEmpty()) pesquisa = null;
		var tamanho = limit == null ? 30 : limit;
		if (tamanho < 1 || tamanho > 30) {
			throw new ListaExceptions.Validacao("limit", "O limite deve estar entre 1 e 30.");
		}
		var cursorDecodificado = decodificarCursor(cursor, estado, pesquisa);
		var resultado = repository.listar(
				conta.getId(),
				estado,
				pesquisa,
				cursorDecodificado.data(),
				cursorDecodificado.id(),
				tamanho);
		if (!resultado.page().hasMore() || resultado.items().isEmpty()) return resultado;
		var ultimo = resultado.items().getLast();
		var proximo = codificarCursor(estado, pesquisa, ultimo.updatedAt(), ultimo.id());
		return new ListCollection(resultado.items(), new PageInfo(proximo, true), resultado.summary());
	}

	@Transactional
	public ListDetail criar(Conta conta, CreateListRequest request, String chave) {
		validarChave(chave);
		var nome = validarNome(request == null ? null : request.name());
		var descricao = validarDescricao(request == null ? null : request.description());
		var fingerprint = Sha256.hex(nome + "\n" + String.valueOf(descricao));
		var anterior = repository.buscarIdempotencia(conta.getId(), chave);
		if (anterior.isPresent()) {
			if (!anterior.orElseThrow().fingerprint().equals(fingerprint)) {
				throw new ChaveIdempotenciaReutilizadaException();
			}
			if (anterior.orElseThrow().listaId() != null) {
				return buscar(conta, anterior.orElseThrow().listaId());
			}
		} else if (!repository.iniciarIdempotencia(conta.getId(), chave, fingerprint)) {
			var concorrente = repository.buscarIdempotencia(conta.getId(), chave).orElseThrow();
			if (!concorrente.fingerprint().equals(fingerprint)) {
				throw new ChaveIdempotenciaReutilizadaException();
			}
			if (concorrente.listaId() != null) return buscar(conta, concorrente.listaId());
		}
		if (repository.nomeEmUso(conta.getId(), nome, null)) throw new ListaExceptions.NomeEmUso();
		var id = UUID.randomUUID();
		try {
			repository.criar(id, conta.getId(), nome, descricao, clock.instant());
		} catch (DataIntegrityViolationException exception) {
			throw new ListaExceptions.NomeEmUso();
		}
		repository.concluirIdempotencia(conta.getId(), chave, id);
		return buscar(conta, id);
	}

	public ListDetail buscar(Conta conta, UUID id) {
		return repository.buscarAcessivel(id, conta.getId()).orElseThrow(ListaExceptions.NaoEncontrada::new);
	}

	@Transactional
	public ListDetail atualizar(
			Conta conta,
			UUID id,
			Long versao,
			boolean temNome,
			String nomeRecebido,
			boolean temDescricao,
			String descricaoRecebida) {
		if (versao == null) throw new ListaExceptions.Validacao("If-Match", "Informe a versão atual da lista.");
		if (!temNome && !temDescricao) {
			throw new ListaExceptions.Validacao("body", "Informe ao menos uma alteração.");
		}
		var atual = buscar(conta, id);
		if (!atual.owner().id().equals(conta.getId())) throw new ListaExceptions.Proibida();
		if ("COMPLETED".equals(atual.status())) throw new ListaExceptions.Concluida();
		if (atual.version() != versao) throw new ListaExceptions.Conflito(atual.version());
		var nome = temNome ? validarNome(nomeRecebido) : atual.name();
		var descricao = temDescricao ? validarDescricao(descricaoRecebida) : atual.description();
		if (nome.equals(atual.name()) && java.util.Objects.equals(descricao, atual.description())) {
			throw new ListaExceptions.Validacao("body", "Informe ao menos uma alteração.");
		}
		if (repository.nomeEmUso(conta.getId(), nome, id)) throw new ListaExceptions.NomeEmUso();
		if (repository.atualizar(id, nome, descricao, clock.instant(), versao) == 0) {
			var recente = buscar(conta, id);
			throw new ListaExceptions.Conflito(recente.version());
		}
		return buscar(conta, id);
	}

	private String validarNome(String valor) {
		var nome = valor == null ? "" : normalizarEspacos(valor);
		if (nome.isEmpty()) throw new ListaExceptions.Validacao("name", NOME_OBRIGATORIO);
		if (nome.length() > 60) throw new ListaExceptions.Validacao("name", NOME_LONGO);
		return nome;
	}

	private String validarDescricao(String valor) {
		if (valor == null) return null;
		if (valor.length() > 240) throw new ListaExceptions.Validacao("description", DESCRICAO_LONGA);
		return valor;
	}

	private String normalizarEspacos(String valor) {
		return valor.trim().replaceAll("\\s+", " ");
	}

	private void validarChave(String chave) {
		if (chave == null || chave.isBlank() || chave.length() > 263) {
			throw new ListaExceptions.Validacao("Idempotency-Key", "Informe uma chave de idempotência válida.");
		}
	}

	private Cursor decodificarCursor(String cursor, String status, String pesquisa) {
		if (cursor == null) return new Cursor(null, null);
		try {
			var texto = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			var partes = texto.split("\\|", -1);
			if (partes.length != 4 || !partes[0].equals(status)
					|| !partes[1].equals(normalizarPesquisa(pesquisa))) throw new IllegalArgumentException();
			return new Cursor(Instant.parse(partes[2]), UUID.fromString(partes[3]));
		} catch (RuntimeException exception) {
			throw new ListaExceptions.Validacao("cursor", "O cursor informado não é válido para esta consulta.");
		}
	}

	private String codificarCursor(String status, String pesquisa, Instant data, UUID id) {
		var texto = status + "|" + normalizarPesquisa(pesquisa) + "|" + data + "|" + id;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	private String normalizarPesquisa(String pesquisa) {
		if (pesquisa == null) return "";
		return Normalizer.normalize(pesquisa, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

	private record Cursor(Instant data, UUID id) {
	}
}
