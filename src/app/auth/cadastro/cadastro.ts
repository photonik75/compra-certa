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
  protected readonly erroNome = signal<string | null>(null);

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
    const tamanho = campo.value.trim().length;
    const erro = tamanho === 0 ? 'Por favor, informe seu nome' : tamanho < 2 || tamanho > 100 ? 'O nome deve ter entre 2 e 100 caracteres' : null;
    campo.setCustomValidity(erro ?? '');
    this.erroNome.set(erro);
  }
}
