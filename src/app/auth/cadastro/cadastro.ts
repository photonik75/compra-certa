import { Component, signal } from '@angular/core';

const CAMPO_SENHA = 'senha';

@Component({
  selector: 'app-cadastro',
  imports: [],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css',
})
export class Cadastro {
  protected readonly senhaVisivel = signal(false);
  protected readonly confirmacaoVisivel = signal(false);
  protected readonly erroNome = signal<string | null>(null);
  protected readonly erroEmail = signal(false);
  protected readonly erroSenha = signal(false);
  protected readonly erroConfirmacao = signal(false);

  protected alternarSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }

  protected alternarConfirmacao(): void {
    this.confirmacaoVisivel.update((visivel) => !visivel);
  }

  protected validarNome(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    this.atualizarValidadeNome(campo);
  }

  protected validarEmail(evento: Event): void {
    this.atualizarValidadeEmail(evento.target as HTMLInputElement);
  }

  protected validarSenha(evento: Event): void {
    this.atualizarValidadeSenha(evento.target as HTMLInputElement);
  }

  protected validarConfirmacao(evento: Event): void {
    this.atualizarValidadeConfirmacao(evento.target as HTMLInputElement);
  }

  protected validarCadastro(evento: SubmitEvent): void {
    evento.preventDefault();
    const formulario = evento.currentTarget as HTMLFormElement;
    const nome = formulario.elements.namedItem('nome') as HTMLInputElement;
    nome.value = nome.value.trim().replace(/\s+/g, ' ');
    this.atualizarValidadeNome(nome);
    this.atualizarValidadeEmail(formulario.elements.namedItem('email') as HTMLInputElement);
    this.atualizarValidadeSenha(formulario.elements.namedItem(CAMPO_SENHA) as HTMLInputElement);
    this.atualizarValidadeConfirmacao(formulario.elements.namedItem('confirmarSenha') as HTMLInputElement);
  }

  private atualizarValidadeNome(campo: HTMLInputElement): void {
    const tamanho = campo.value.trim().length;
    const erro = tamanho === 0 ? 'Por favor, informe seu nome' : tamanho < 2 || tamanho > 100 ? 'O nome deve ter entre 2 e 100 caracteres' : null;
    campo.setCustomValidity(erro ?? '');
    this.erroNome.set(erro);
  }

  private atualizarValidadeEmail(campo: HTMLInputElement): void {
    const invalido = campo.value.trim().length === 0 || campo.value.length > 254 || campo.validity.typeMismatch;
    campo.setCustomValidity(invalido ? 'Por favor, informe um e-mail válido' : '');
    this.erroEmail.set(invalido);
  }

  private atualizarValidadeSenha(campo: HTMLInputElement): void {
    const invalido = campo.value.length < 8 || campo.value.length > 128;
    campo.setCustomValidity(invalido ? 'A senha deve ter entre 8 e 128 caracteres' : '');
    this.erroSenha.set(invalido);
  }

  private atualizarValidadeConfirmacao(campo: HTMLInputElement): void {
    const senha = (campo.form?.elements.namedItem(CAMPO_SENHA) as HTMLInputElement).value;
    const invalido = campo.value.length === 0 || campo.value !== senha;
    campo.setCustomValidity(invalido ? 'As senhas devem ser idênticas' : '');
    this.erroConfirmacao.set(invalido);
  }
}
