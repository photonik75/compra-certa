package br.leobarros.compracerta.autenticacao.sessao;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SessaoHttpResponseService {

	private final SessaoCookieService sessaoCookieService;

	public SessaoHttpResponseService(SessaoCookieService sessaoCookieService) {
		this.sessaoCookieService = sessaoCookieService;
	}

	public ResponseEntity<SessionResponse> criar(HttpStatus status, SessaoCriada sessao) {
		var cookie = sessaoCookieService.criar(sessao.token());
		return ResponseEntity.status(status)
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(sessao.response());
	}
}
