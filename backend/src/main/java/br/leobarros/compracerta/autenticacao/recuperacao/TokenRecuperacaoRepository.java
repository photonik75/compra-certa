package br.leobarros.compracerta.autenticacao.recuperacao;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.leobarros.compracerta.autenticacao.comum.Sha256;

public class TokenRecuperacaoRepository {

	private final Map<String, TokenRecuperacao> tokens = new ConcurrentHashMap<>();

	void salvar(TokenRecuperacao token) {
		tokens.put(token.hash(), token);
	}

	Optional<TokenRecuperacao> buscar(String hash) {
		return Optional.ofNullable(tokens.get(hash));
	}

	void invalidarDaConta(UUID contaId) {
		tokens.values().stream()
				.filter(token -> token.conta().getId().equals(contaId) && !token.usado())
				.forEach(TokenRecuperacao::invalidar);
	}

	public boolean contemValor(String valor) {
		return tokens.containsKey(valor);
	}

	public long quantidade() {
		return tokens.size();
	}

	public Instant expiracaoDoToken(String token) {
		return buscar(Sha256.hex(token))
				.map(TokenRecuperacao::expiraEm)
				.orElseThrow();
	}

	void atualizar(TokenRecuperacao token) {
		salvar(token);
	}
}
