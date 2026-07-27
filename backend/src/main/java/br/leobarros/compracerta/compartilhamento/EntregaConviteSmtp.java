package br.leobarros.compracerta.compartilhamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "compra-certa.email.smtp.enabled", havingValue = "true")
class EntregaConviteSmtp implements EntregaConvite {
	private final JavaMailSender mailSender;
	private final String remetente;
	private final String frontendBaseUrl;
	EntregaConviteSmtp(
			JavaMailSender mailSender,
			@Value("${compra-certa.email.from}") String remetente,
			@Value("${compra-certa.frontend.base-url}") String frontendBaseUrl) {
		this.mailSender = mailSender;
		this.remetente = remetente;
		this.frontendBaseUrl = frontendBaseUrl;
	}
	@Override
	public void send(String email, String token) {
		var mensagem = new SimpleMailMessage();
		mensagem.setFrom(remetente);
		mensagem.setTo(email);
		mensagem.setSubject("Convite para uma lista — CompraCerta");
		mensagem.setText("""
				Você recebeu um convite para colaborar em uma lista.

				Abra o link para consultar e aceitar:
				%s/convites/aceitar?token=%s
				""".formatted(frontendBaseUrl, token));
		mailSender.send(mensagem);
	}
}
