package br.leobarros.compracerta.autenticacao.sessao;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class SessaoRepository {

	private final Map<String, SessaoRegistro> sessoes = new ConcurrentHashMap<>();

	Optional<SessaoRegistro> buscarPorTokenHash(String tokenHash) {
		return Optional.ofNullable(sessoes.get(tokenHash));
	}

	void salvar(SessaoRegistro sessao) {
		sessoes.put(sessao.tokenHash(), sessao);
	}

	void revogarDaConta(UUID contaId) {
		sessoes.values().stream()
				.filter(sessao -> sessao.conta().getId().equals(contaId))
				.forEach(SessaoRegistro::revogar);
	}
}
