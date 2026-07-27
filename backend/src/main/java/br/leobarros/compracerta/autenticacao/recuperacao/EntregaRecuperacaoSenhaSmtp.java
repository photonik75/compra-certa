package br.leobarros.compracerta.autenticacao.recuperacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "compra-certa.email.smtp.enabled", havingValue = "true")
public class EntregaRecuperacaoSenhaSmtp implements EntregaRecuperacaoSenha {

	private static final String ASSUNTO = "Redefinição de senha — CompraCerta";
	private static final String MENSAGEM = """
			Recebemos uma solicitação para redefinir sua senha.

			Use o link abaixo em até 30 minutos:
			%s

			Se você não fez essa solicitação, ignore esta mensagem.
			""";

	private final JavaMailSender mailSender;
	private final String remetente;

	public EntregaRecuperacaoSenhaSmtp(
			JavaMailSender mailSender,
			@Value("${compra-certa.email.from}") String remetente) {
		this.mailSender = mailSender;
		this.remetente = remetente;
	}

	@Override
	public void enviar(String email, String link) {
		var mensagem = new SimpleMailMessage();
		mensagem.setFrom(remetente);
		mensagem.setTo(email);
		mensagem.setSubject(ASSUNTO);
		mensagem.setText(MENSAGEM.formatted(link));
		mailSender.send(mensagem);
	}
}
