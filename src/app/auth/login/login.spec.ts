import { render, screen } from '@testing-library/angular';
import { Login } from './login';

const TITULO_LOGIN = 'Entre na sua conta';
const EMAIL = 'E-mail';
const SENHA = 'Senha';
const MOSTRAR_OU_OCULTAR = /^(mostrar|ocultar)$/i;
const MANTER_CONECTADO = 'Manter-me conectado';
const ENTRAR = 'Entrar';
const CRIAR_CONTA = 'Criar uma conta';
const ESQUECI_SENHA = 'Esqueci minha senha';

describe('Testes unitários do componente Login', () => {
  it(
    'LOG-1 - Verifica se título, E-mail, Senha, Mostrar/Ocultar, “Manter-me conectado”, “Entrar”, ' +
      '“Criar uma conta” e “Esqueci minha senha” estão presentes na tela de login.',
    async () => {
      await render(Login);

      expect(screen.getByRole('heading', { name: TITULO_LOGIN })).toBeTruthy();
      expect(screen.getByRole('textbox', { name: EMAIL })).toBeTruthy();
      expect((screen.getByLabelText(SENHA) as HTMLInputElement).type).toBe('password');
      expect(screen.getByRole('button', { name: MOSTRAR_OU_OCULTAR })).toBeTruthy();
      expect(screen.getByRole('checkbox', { name: MANTER_CONECTADO })).toBeTruthy();
      expect(screen.getByRole('button', { name: ENTRAR })).toBeTruthy();
      expect(screen.getByRole('link', { name: CRIAR_CONTA })).toBeTruthy();
      expect(screen.getByRole('link', { name: ESQUECI_SENHA })).toBeTruthy();
    },
  );
});
