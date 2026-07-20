import { Component } from '@angular/core';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  template: `
    <main>
      <h1>Crie sua conta</h1>

      <form>
        <label for="nome">Nome</label>
        <input id="nome" name="nome" type="text" />

        <label for="email">E-mail</label>
        <input id="email" name="email" type="email" />

        <label for="senha">Senha</label>
        <input id="senha" name="senha" type="password" />
        <button type="button" aria-label="Mostrar senha">Mostrar</button>

        <label for="confirmacao-senha">Confirmar senha</label>
        <input
          id="confirmacao-senha"
          name="confirmacaoSenha"
          type="password"
        />
        <button type="button" aria-label="Mostrar confirmação de senha">
          Mostrar
        </button>

        <button type="submit">Criar conta</button>
      </form>

      <a href="/login">Entrar</a>
    </main>
  `,
})
export class CadastroComponent {}
