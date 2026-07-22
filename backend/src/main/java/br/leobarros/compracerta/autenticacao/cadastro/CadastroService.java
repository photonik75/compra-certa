package br.leobarros.compracerta.autenticacao.cadastro;

import org.springframework.security.crypto.password.PasswordEncoder;

public class CadastroService {

	private final ContaRepository contaRepository;
	private final PasswordEncoder passwordEncoder;

	public CadastroService(ContaRepository contaRepository, PasswordEncoder passwordEncoder) {
		this.contaRepository = contaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void cadastrar(DadosCadastro dadosCadastro) {
		var senhaHash = passwordEncoder.encode(dadosCadastro.senha());
		contaRepository.salvar(new Conta(dadosCadastro.nome(), dadosCadastro.email(), senhaHash));
	}
}
