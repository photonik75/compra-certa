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

  protected alternarSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }

  protected alternarConfirmacao(): void {
    this.confirmacaoVisivel.update((visivel) => !visivel);
  }
}
