package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class SessaoCookieService {

	private static final String NOME_COOKIE_SESSAO = "cc_session";
	private static final String PATH_API = "/api/v1";
	private static final String SAME_SITE = "Lax";

	public ResponseCookie criar(String token) {
		return ResponseCookie.from(NOME_COOKIE_SESSAO, token)
				.httpOnly(true)
				.secure(true)
				.sameSite(SAME_SITE)
				.path(PATH_API)
				.build();
	}

	public ResponseCookie expirar() {
		return ResponseCookie.from(NOME_COOKIE_SESSAO, "")
				.httpOnly(true)
				.secure(true)
				.sameSite(SAME_SITE)
				.path(PATH_API)
				.maxAge(Duration.ZERO)
				.build();
	}
}
