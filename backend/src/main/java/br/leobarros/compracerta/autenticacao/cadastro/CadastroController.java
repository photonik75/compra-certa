package br.leobarros.compracerta.autenticacao.cadastro;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public ResponseEntity<SessionResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
		var conta = cadastroService.cadastrar(request.toDadosCadastro());
		var agora = Instant.now();
		var expiracao = agora.plus(Duration.ofHours(24));
		var tokenSessao = UUID.randomUUID().toString();
		var cookie = ResponseCookie.from("cc_session", tokenSessao)
				.httpOnly(true)
				.secure(true)
				.sameSite("Lax")
				.path("/api/v1")
				.build();
		var usuario = new SessionResponse.UserSummary(
				UUID.randomUUID(), conta.getNome(), conta.getEmail(), "ACTIVE", agora);
		var resposta = new SessionResponse(usuario, UUID.randomUUID().toString(), expiracao);
		return ResponseEntity.status(HttpStatus.CREATED)
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(resposta);
	}
}
