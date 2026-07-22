import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CredenciaisInvalidasError, LoginService, MuitasTentativasError } from './login.service';

const ERRO_CREDENCIAIS = 'E-mail ou senha inválidos';
const ERRO_BLOQUEIO = 'Muitas tentativas de acesso. Tente novamente em 15 minutos';
const ERRO_EMAIL = 'Por favor, informe um e-mail válido';
const ERRO_SENHA = 'Por favor, informe sua senha';
const ROTA_CADASTRO = '/cadastro';
const ROTA_RECUPERAR_SENHA = '/recuperar-senha';
const ROTA_LISTAS = '/listas';

@Component({
  selector: 'app-login',
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly senhaVisivel = signal(false);
  protected readonly processando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly erroEmail = signal(false);
  protected readonly erroSenha = signal(false);
  protected readonly formularioValido = signal(false);

  protected alternarSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }

  protected validarEmail(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    this.atualizarValidadeEmail(campo);
    this.atualizarEstadoFormulario(campo);
  }

  protected validarSenha(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    this.atualizarValidadeSenha(campo);
    this.atualizarEstadoFormulario(campo);
  }

  protected entrar(evento: SubmitEvent): void {
    evento.preventDefault();
    if (this.processando()) return;
    const formulario = evento.currentTarget as HTMLFormElement;
    const email = formulario.elements.namedItem('email') as HTMLInputElement;
    const senha = formulario.elements.namedItem('senha') as HTMLInputElement;
    const manterConectado = formulario.elements.namedItem('manterConectado') as HTMLInputElement;
    this.atualizarValidadeEmail(email);
    this.atualizarValidadeSenha(senha);
    if (!formulario.checkValidity()) return;
    this.erro.set(null);
    this.processando.set(true);
    this.loginService
      .entrar({ email: email.value, password: senha.value, manterConectado: manterConectado.checked })
      .pipe(finalize(() => this.processando.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('returnUrl') ?? ROTA_LISTAS),
        error: (erro: unknown) => this.tratarErro(erro),
      });
  }

  protected criarConta(evento: MouseEvent): void {
    evento.preventDefault();
    this.router.navigateByUrl(ROTA_CADASTRO);
  }

  protected recuperarSenha(evento: MouseEvent): void {
    evento.preventDefault();
    this.router.navigateByUrl(ROTA_RECUPERAR_SENHA);
  }

  private tratarErro(erro: unknown): void {
    if (erro instanceof CredenciaisInvalidasError) this.erro.set(ERRO_CREDENCIAIS);
    if (erro instanceof MuitasTentativasError) this.erro.set(ERRO_BLOQUEIO);
  }

  private atualizarValidadeEmail(campo: HTMLInputElement): void {
    const invalido = campo.value.length === 0 || campo.value.length > 254 || campo.validity.typeMismatch;
    campo.setCustomValidity(invalido ? ERRO_EMAIL : '');
    this.erroEmail.set(invalido);
  }

  private atualizarValidadeSenha(campo: HTMLInputElement): void {
    const invalido = campo.value.length === 0;
    campo.setCustomValidity(invalido ? ERRO_SENHA : '');
    this.erroSenha.set(invalido);
  }

  private atualizarEstadoFormulario(campo: HTMLInputElement): void {
    this.formularioValido.set(campo.form?.checkValidity() ?? false);
  }
}
