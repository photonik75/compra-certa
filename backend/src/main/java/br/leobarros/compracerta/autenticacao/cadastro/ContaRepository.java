package br.leobarros.compracerta.autenticacao.cadastro;

public interface ContaRepository {

	boolean existePorEmail(String email);

	void salvar(Conta conta);

	void remover(Conta conta);
}
