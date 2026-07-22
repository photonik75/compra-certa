package br.leobarros.compracerta.autenticacao.cadastro;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CadastroServiceTest {

	private static final String NOME = "Maria Silva";
	private static final String EMAIL = "maria@example.com";
	private static final String SENHA = "senha segura";
	private static final String HASH_DA_SENHA = "hash-da-senha";

	@Mock
	private ContaRepository contaRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private CadastroService cadastroService;

	@Test
	void cadastraNomeEmailEHashDaSenhaQuandoDadosSaoValidos() {
		when(passwordEncoder.encode(SENHA)).thenReturn(HASH_DA_SENHA);
		cadastroService.cadastrar(new DadosCadastro(NOME, EMAIL, SENHA, SENHA));
		var contaCaptor = ArgumentCaptor.forClass(Conta.class);
		verify(contaRepository).salvar(contaCaptor.capture());
		var contaSalva = contaCaptor.getValue();
		assertAll(
				() -> assertEquals(NOME, contaSalva.getNome()),
				() -> assertEquals(EMAIL, contaSalva.getEmail()),
				() -> assertEquals(HASH_DA_SENHA, contaSalva.getSenhaHash()));
	}
}
