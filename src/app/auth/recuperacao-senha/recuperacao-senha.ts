import { Component, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { RecuperacaoSenhaService } from './recuperacao-senha.service';

const CONFIRMACAO = 'Se houver uma conta para este e-mail, enviaremos as instruções';
const SUCESSO_ENVIO = 'Solicitação de recuperação enviada com sucesso.';
const ERRO_EMAIL = 'Por favor, informe um e-mail válido';
const ERRO_ENVIO = 'Não foi possível enviar as instruções. Tente novamente mais tarde.';
const MENSAGEM_PROCESSAMENTO = 'Enviando instruções';

@Component({
  selector: 'app-recuperacao-senha',
  templateUrl: './recuperacao-senha.html',
  styleUrls: ['../auth.css', './recuperacao-senha.css'],
})
export class RecuperacaoSenha {
  private readonly service = inject(RecuperacaoSenhaService);
  protected readonly mensagemProcessamento = MENSAGEM_PROCESSAMENTO;
  protected readonly processando = signal(false);
  protected readonly formularioValido = signal(false);
  protected readonly erroEmail = signal<string | null>(null);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly confirmacao = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected validarEmail(evento: Event): void {
    this.atualizarValidadeEmail(evento.target as HTMLInputElement);
  }

  protected solicitar(evento: SubmitEvent): void {
    evento.preventDefault();
    if (this.processando()) return;
    const formulario = evento.currentTarget as HTMLFormElement;
    const email = formulario.elements.namedItem('email') as HTMLInputElement;
    this.atualizarValidadeEmail(email);
    if (!formulario.checkValidity()) return;
    this.erroGeral.set(null);
    this.processando.set(true);
    this.service.solicitar(email.value).pipe(finalize(() => this.processando.set(false))).subscribe({
      next: () => {
        this.sucesso.set(SUCESSO_ENVIO);
        this.confirmacao.set(CONFIRMACAO);
      },
      error: () => this.erroGeral.set(ERRO_ENVIO),
    });
  }

  protected solicitarNovamente(): void {
    this.confirmacao.set(null);
    this.sucesso.set(null);
    this.formularioValido.set(false);
    this.erroEmail.set(null);
    this.erroGeral.set(null);
  }

  private atualizarValidadeEmail(campo: HTMLInputElement): void {
    const invalido = campo.value.length === 0 || campo.value.length > 254 || campo.validity.typeMismatch;
    campo.setCustomValidity(invalido ? ERRO_EMAIL : '');
    this.erroEmail.set(invalido ? ERRO_EMAIL : null);
    this.formularioValido.set(campo.form?.checkValidity() ?? false);
  }
}
