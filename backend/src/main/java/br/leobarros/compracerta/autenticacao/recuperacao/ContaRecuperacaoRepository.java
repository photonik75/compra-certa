package br.leobarros.compracerta.autenticacao.recuperacao;

import java.util.Optional;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;

public interface ContaRecuperacaoRepository {

	Optional<Conta> buscarPorEmail(String email);

	void salvar(Conta conta);
}
