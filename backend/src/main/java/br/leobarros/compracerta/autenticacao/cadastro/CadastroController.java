package br.leobarros.compracerta.autenticacao.cadastro;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CadastroController.ENDPOINT_CADASTRO)
public class CadastroController {

	static final String ENDPOINT_CADASTRO = "/api/v1/auth/registrations";

	private final CadastroService cadastroService;

	public CadastroController(CadastroService cadastroService) {
		this.cadastroService = cadastroService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void cadastrar(@Valid @RequestBody CadastroRequest request) {
		cadastroService.cadastrar(request.toDadosCadastro());
	}
}
