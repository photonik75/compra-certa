import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import {
  LinkRecuperacaoInvalidoError,
  RedefinicaoSenhaService,
} from './redefinicao-senha.service';

const ERRO_CONFIRMACAO = 'As senhas devem ser idênticas';
const ERRO_GERAL = 'Não foi possível redefinir sua senha. Tente novamente mais tarde.';
const ERRO_LINK = 'O link de recuperação é inválido, já foi usado ou expirou.';
const ERRO_SENHA = 'A senha deve ter entre 8 e 128 caracteres';
const PARAMETRO_TOKEN = 'token';
const ROTA_LOGIN = '/entrar';

@Component({
  selector: 'app-redefinicao-senha',
  imports: [],
  templateUrl: './redefinicao-senha.html',
  styleUrls: ['../auth.css', './redefinicao-senha.css'],
})
export class RedefinicaoSenha {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(RedefinicaoSenhaService);
  private readonly token = new URLSearchParams(this.route.snapshot.fragment ?? '').get(PARAMETRO_TOKEN) ?? '';
  protected readonly senhaVisivel = signal(false);
  protected readonly confirmacaoVisivel = signal(false);
  protected readonly erroSenha = signal<string | null>(null);
  protected readonly erroConfirmacao = signal<string | null>(null);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly mensagemLinkInvalido = ERRO_LINK;
  protected readonly linkInvalido = signal(false);
  protected readonly processando = signal(false);
  protected readonly formularioValido = signal(false);

  protected alternarSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }

  protected alternarConfirmacao(): void {
    this.confirmacaoVisivel.update((visivel) => !visivel);
  }

  protected validarSenha(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    this.atualizarValidadeSenha(campo);
    const confirmacao = campo.form?.elements.namedItem('confirmarNovaSenha') as HTMLInputElement;
    if (confirmacao.value.length > 0) this.atualizarValidadeConfirmacao(confirmacao);
    this.atualizarEstadoFormulario(campo);
  }

  protected validarConfirmacao(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    this.atualizarValidadeConfirmacao(campo);
    this.atualizarEstadoFormulario(campo);
  }

  protected redefinir(evento: SubmitEvent): void {
    evento.preventDefault();
    if (this.processando()) return;
    const formulario = evento.currentTarget as HTMLFormElement;
    const senha = formulario.elements.namedItem('novaSenha') as HTMLInputElement;
    const confirmacao = formulario.elements.namedItem('confirmarNovaSenha') as HTMLInputElement;
    this.atualizarValidadeSenha(senha);
    this.atualizarValidadeConfirmacao(confirmacao);
    if (!formulario.checkValidity()) return;
    this.erroGeral.set(null);
    this.linkInvalido.set(false);
    this.processando.set(true);
    this.service
      .redefinir({
        token: this.token,
        newPassword: senha.value,
        passwordConfirmation: confirmacao.value,
      })
      .pipe(finalize(() => this.processando.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl(ROTA_LOGIN),
        error: (erro: unknown) => this.tratarErro(erro),
      });
  }

  private atualizarValidadeSenha(campo: HTMLInputElement): void {
    const invalido = campo.value.length < 8 || campo.value.length > 128;
    campo.setCustomValidity(invalido ? ERRO_SENHA : '');
    this.erroSenha.set(invalido ? ERRO_SENHA : null);
  }

  private atualizarValidadeConfirmacao(campo: HTMLInputElement): void {
    const senha = (campo.form?.elements.namedItem('novaSenha') as HTMLInputElement).value;
    const invalido = campo.value.length === 0 || campo.value !== senha;
    campo.setCustomValidity(invalido ? ERRO_CONFIRMACAO : '');
    this.erroConfirmacao.set(invalido ? ERRO_CONFIRMACAO : null);
  }

  private atualizarEstadoFormulario(campo: HTMLInputElement): void {
    this.formularioValido.set(campo.form?.checkValidity() ?? false);
  }

  private tratarErro(erro: unknown): void {
    if (erro instanceof LinkRecuperacaoInvalidoError) {
      this.linkInvalido.set(true);
      return;
    }
    this.erroGeral.set(ERRO_GERAL);
  }
}
