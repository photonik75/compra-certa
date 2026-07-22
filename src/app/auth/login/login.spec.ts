import { Provider } from '@angular/core';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { fireEvent, render as renderComponent, screen } from '@testing-library/angular';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Login } from './login';
import { CredenciaisInvalidasError, LoginService, MuitasTentativasError } from './login.service';

const TITULO_LOGIN = 'Entre na sua conta';
const EMAIL = 'E-mail';
const SENHA = 'Senha';
const MOSTRAR_OU_OCULTAR = /^(mostrar|ocultar)$/i;
const MOSTRAR = 'Mostrar';
const OCULTAR = 'Ocultar';
const MANTER_CONECTADO = 'Manter-me conectado';
const ENTRAR = 'Entrar';
const CRIAR_CONTA = 'Criar uma conta';
const ESQUECI_SENHA = 'Esqueci minha senha';
const PLACEHOLDER_SENHA = 'Mínimo de 8 caracteres';
const ERRO_EMAIL = 'Por favor, informe um e-mail válido';
const ERRO_SENHA = 'Por favor, informe sua senha';
const ATRIBUTO_ARIA_INVALIDO = 'aria-invalid';
const ARIA_INVALIDO = 'true';
const ARIA_VALIDO = 'false';
const EMAIL_254 = `${'a'.repeat(64)}@${'b'.repeat(63)}.${'c'.repeat(63)}.${'d'.repeat(61)}`;
const EMAIL_255 = `${EMAIL_254}d`;
const EMAIL_VALIDO = 'maria@example.com';
const SENHA_VALIDA = 'senha123';
const ERRO_CREDENCIAIS = 'E-mail ou senha inválidos';
const ERRO_BLOQUEIO = 'Muitas tentativas de acesso. Tente novamente em 15 minutos';
const ROTA_CADASTRO = '/cadastro';
const ROTA_RECUPERAR_SENHA = '/recuperar-senha';
const ROTA_LISTAS = '/listas';
const ROTA_SOLICITADA = '/categorias';
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: 'Maria',
    email: EMAIL_VALIDO,
    status: 'ACTIVE',
    createdAt: '2026-07-21T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-22T00:00:00Z',
};

async function render(_component: typeof Login, options: { providers?: Provider[] } = {}) {
  const loginService = { entrar: vi.fn().mockReturnValue(NEVER) };
  const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
  return renderComponent(Login, {
    providers: [
      { provide: LoginService, useValue: loginService },
      { provide: Router, useValue: router },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
      },
      ...(options.providers ?? []),
    ],
  });
}

function preencherLoginValido(manterConectado = false): void {
  fireEvent.input(screen.getByRole('textbox', { name: EMAIL }), { target: { value: EMAIL_VALIDO } });
  fireEvent.input(screen.getByLabelText(SENHA), { target: { value: SENHA_VALIDA } });
  if (manterConectado) fireEvent.click(screen.getByRole('checkbox', { name: MANTER_CONECTADO }));
  fireEvent.click(screen.getByRole('button', { name: ENTRAR }));
}

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

  it(
    'LOG-2 - Confirma que e-mails vazios, inválidos ou com 255 caracteres são rejeitados com a mensagem ' +
      'normativa e que um endereço válido com 254 caracteres é aceito.',
    async () => {
      await render(Login);
      const email = screen.getByRole('textbox', { name: EMAIL }) as HTMLInputElement;
      const entrar = screen.getByRole('button', { name: ENTRAR });
      for (const valor of ['', 'email-invalido', EMAIL_255]) {
        fireEvent.input(email, { target: { value: valor } });
        fireEvent.click(entrar);
        expect(email.checkValidity()).toBe(false);
        expect(email.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
        expect(screen.getByText(ERRO_EMAIL)).toBeTruthy();
      }
      fireEvent.input(email, { target: { value: EMAIL_254 } });
      expect(email.checkValidity()).toBe(true);
      expect(email.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
      expect(screen.queryByText(ERRO_EMAIL)).toBeNull();
    },
  );

  it(
    'LOG-3 - Testa o impedimento do login com senha vazia, exibe a mensagem normativa e confirma que as regras ' +
      'de criação de senha não são aplicadas à senha informada.',
    async () => {
      await render(Login);
      const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
      fireEvent.click(screen.getByRole('button', { name: ENTRAR }));
      expect(senha.checkValidity()).toBe(false);
      expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
      expect(screen.getByText(ERRO_SENHA)).toBeTruthy();
      fireEvent.input(senha, { target: { value: 'a' } });
      expect(senha.checkValidity()).toBe(true);
      expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
      expect(screen.queryByText(ERRO_SENHA)).toBeNull();
    },
  );

  it('LOG-4 - Verifica se “Mínimo de 8 caracteres” é exibido como orientação no campo Senha.', async () => {
    await render(Login);
    expect(screen.getByPlaceholderText(PLACEHOLDER_SENHA)).toBe(screen.getByLabelText(SENHA));
  });

  it('LOG-5 - Confirma que Mostrar/Ocultar alterna a visibilidade da senha sem alterar seu valor.', async () => {
    await render(Login);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    fireEvent.input(senha, { target: { value: SENHA_VALIDA } });
    fireEvent.click(screen.getByRole('button', { name: MOSTRAR }));
    expect(senha.type).toBe('text');
    fireEvent.click(screen.getByRole('button', { name: OCULTAR }));
    expect(senha.type).toBe('password');
    expect(senha.value).toBe(SENHA_VALIDA);
  });

  it('LOG-6 - Verifica se “Manter-me conectado” é exibido inicialmente desmarcado.', async () => {
    await render(Login);
    expect((screen.getByRole('checkbox', { name: MANTER_CONECTADO }) as HTMLInputElement).checked).toBe(false);
  });

  it('LOG-7 - Confirma que a solicitação de login informa se “Manter-me conectado” foi marcado.', async () => {
    const loginService = { entrar: vi.fn().mockReturnValue(NEVER) };
    await render(Login, { providers: [{ provide: LoginService, useValue: loginService }] });
    preencherLoginValido(true);
    expect(loginService.entrar).toHaveBeenCalledWith({
      email: EMAIL_VALIDO,
      password: SENHA_VALIDA,
      manterConectado: true,
    });
  });

  it(
    'LOG-8 - Confirma que e-mail inexistente e senha incorreta apresentam “E-mail ou senha inválidos”.',
    async () => {
      const loginService = {
        entrar: vi.fn().mockReturnValue(throwError(() => new CredenciaisInvalidasError())),
      };
      await render(Login, { providers: [{ provide: LoginService, useValue: loginService }] });
      preencherLoginValido();
      expect(await screen.findByText(ERRO_CREDENCIAIS)).toBeTruthy();
      expect(screen.queryByText(/e-mail inexistente|senha incorreta/i)).toBeNull();
    },
  );

  it('LOG-9 - Exibe a mensagem normativa quando o serviço reconhece o bloqueio temporário.', async () => {
    const loginService = {
      entrar: vi.fn().mockReturnValue(throwError(() => new MuitasTentativasError())),
    };
    await render(Login, { providers: [{ provide: LoginService, useValue: loginService }] });
    preencherLoginValido();
    expect(await screen.findByText(ERRO_BLOQUEIO)).toBeTruthy();
  });

  it('LOG-10 - Após o login, abre a rota solicitada anteriormente.', async () => {
    const loginService = { entrar: vi.fn().mockReturnValue(of(SESSION_RESPONSE)) };
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    const rota = { snapshot: { queryParamMap: convertToParamMap({ returnUrl: ROTA_SOLICITADA }) } };
    await render(Login, {
      providers: [
        { provide: LoginService, useValue: loginService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: rota },
      ],
    });
    preencherLoginValido();
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_SOLICITADA);
  });

  it('LOG-10 - Após o login, abre Minhas Listas quando não existe uma rota solicitada.', async () => {
    const loginService = { entrar: vi.fn().mockReturnValue(of(SESSION_RESPONSE)) };
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    await render(Login, {
      providers: [
        { provide: LoginService, useValue: loginService },
        { provide: Router, useValue: router },
      ],
    });
    preencherLoginValido();
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_LISTAS);
  });

  it('LOG-11 - Durante o login, desabilita o envio e ignora cliques adicionais.', async () => {
    const resposta = new Subject<typeof SESSION_RESPONSE>();
    const loginService = { entrar: vi.fn().mockReturnValue(resposta) };
    await render(Login, { providers: [{ provide: LoginService, useValue: loginService }] });
    preencherLoginValido();
    const entrar = screen.getByRole('button', { name: ENTRAR }) as HTMLButtonElement;
    fireEvent.click(entrar);
    fireEvent.click(entrar);
    expect.soft(entrar.disabled).toBe(true);
    expect(loginService.entrar).toHaveBeenCalledOnce();
  });

  it('LOG-12 - Confirma que “Criar uma conta” abre a tela “Crie sua conta”.', async () => {
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    await render(Login, { providers: [{ provide: Router, useValue: router }] });
    fireEvent.click(screen.getByRole('link', { name: CRIAR_CONTA }));
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_CADASTRO);
  });

  it('LOG-13 - Confirma que “Esqueci minha senha” abre a tela de recuperação de senha.', async () => {
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    await render(Login, { providers: [{ provide: Router, useValue: router }] });
    fireEvent.click(screen.getByRole('link', { name: ESQUECI_SENHA }));
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_RECUPERAR_SENHA);
  });
});
