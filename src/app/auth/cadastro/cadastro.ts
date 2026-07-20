import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-cadastro',
  imports: [],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css',
})
export class Cadastro {
  protected readonly senhaVisivel = signal(false);
  protected readonly confirmacaoVisivel = signal(false);
  protected readonly erroNome = signal(false);

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

  protected validarCadastro(evento: SubmitEvent): void {
    evento.preventDefault();
    const formulario = evento.currentTarget as HTMLFormElement;
    this.atualizarValidadeNome(formulario.elements.namedItem('nome') as HTMLInputElement);
  }

  private atualizarValidadeNome(campo: HTMLInputElement): void {
    const nomeInvalido = campo.value.trim().length === 0;
    campo.setCustomValidity(nomeInvalido ? 'Por favor, informe seu nome' : '');
    this.erroNome.set(nomeInvalido);
  }
}
