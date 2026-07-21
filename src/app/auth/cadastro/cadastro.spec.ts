import { Provider } from '@angular/core';
import { Router } from '@angular/router';
import { fireEvent, render as renderComponent, screen } from '@testing-library/angular';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Cadastro } from './cadastro';
import { CadastroService, EmailJaCadastradoError } from './cadastro.service';

const NOME = 'Nome';
const EMAIL = 'E-mail';
const SENHA = 'Senha';
const CONFIRMAR_SENHA = 'Confirmar senha';
const CRIAR_CONTA = 'Criar conta';
const TITULO_CADASTRO = 'Crie sua conta';
const MOSTRAR = 'Mostrar';
const OCULTAR = 'Ocultar';
const ERRO_NOME = 'Por favor, informe seu nome';
const ERRO_EMAIL = 'Por favor, informe um e-mail válido';
const ARIA_INVALIDO = 'true';
const ARIA_VALIDO = 'false';
const ATRIBUTO_ARIA_INVALIDO = 'aria-invalid';
const EMAIL_254 = `${'a'.repeat(64)}@${'b'.repeat(63)}.${'c'.repeat(63)}.${'d'.repeat(61)}`;
const EMAIL_255 = `${EMAIL_254}d`;
const SENHA_VALIDA = 'senha123';
const SENHA_SEGURA = 'Senha segura';
const CARACTERE_SENHA = 'a';
const EMAIL_VALIDO = 'maria@example.com';
const NOME_VALIDO = 'Maria';
const EMAIL_JA_CADASTRADO = 'E-mail já foi cadastrado';
const ERRO_GERAL_CADASTRO =
  'Ocorreu um erro ao tentar criar sua conta. Aguarde e tente novamente em alguns instantes.';
const ROTA_LISTAS = '/listas';
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: NOME_VALIDO,
    email: EMAIL_VALIDO,
    status: 'ACTIVE',
    createdAt: '2026-07-21T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-22T00:00:00Z',
};

async function render(_component: typeof Cadastro, options: { providers?: Provider[] } = {}) {
  const cadastroService = { cadastrar: vi.fn().mockReturnValue(NEVER) };
  const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
  return renderComponent(Cadastro, {
    providers: [
      { provide: CadastroService, useValue: cadastroService },
      { provide: Router, useValue: router },
      ...(options.providers ?? []),
    ],
  });
}

function preencherCadastroValido(): void {
  fireEvent.input(screen.getByRole('textbox', { name: NOME }), { target: { value: NOME_VALIDO } });
  fireEvent.input(screen.getByRole('textbox', { name: EMAIL }), {
    target: { value: EMAIL_VALIDO },
  });
  fireEvent.input(screen.getByLabelText(SENHA), { target: { value: SENHA_VALIDA } });
  fireEvent.input(screen.getByLabelText(CONFIRMAR_SENHA), { target: { value: SENHA_VALIDA } });
  fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
}

describe('Testes unitários do componente Cadastro', () => {
  it(
    'CAD-1 - Verifica se os componentes título, Nome, E-mail, Senha, Confirmar senha, dois controles de ' +
      'visibilidade, botão “Criar conta” e link “Entrar” estão presentes na tela de cadastro.',
    async () => {
      await render(Cadastro);

      expect(screen.getByRole('heading', { name: TITULO_CADASTRO })).toBeTruthy();
      expect(screen.getByRole('textbox', { name: NOME })).toBeTruthy();
      expect(screen.getByRole('textbox', { name: EMAIL })).toBeTruthy();
      expect((screen.getByLabelText(SENHA) as HTMLInputElement).type).toBe('password');
      expect((screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement).type).toBe('password');
      expect(screen.getAllByRole('button', { name: /^(mostrar|ocultar)$/i })).toHaveLength(2);
      expect(screen.getByRole('button', { name: CRIAR_CONTA })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Entrar' })).toBeTruthy();
    },
  );

  it('CAD-2 - Testa o impedimento do cadastro quando o nome está vazio ou contém somente espaços.', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: NOME }) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(screen.getByText(ERRO_NOME)).toBeTruthy();
    fireEvent.input(nome, { target: { value: NOME_VALIDO } });
    expect(screen.queryByText(ERRO_NOME)).toBeNull();
    fireEvent.input(nome, { target: { value: '   ' } });
    fireEvent.click(criarConta);
    expect(screen.getByText(ERRO_NOME)).toBeTruthy();
  });

  it(
    'CAD-3 - Confirma que nomes com 1 ou 101 caracteres são rejeitados e nomes com 2 ou 100 caracteres são ' +
      'aceitos.',
    async () => {
      await render(Cadastro);
      const nome = screen.getByRole('textbox', { name: NOME }) as HTMLInputElement;
      fireEvent.input(nome, { target: { value: 'A' } });
      expect(nome.checkValidity()).toBe(false);
      fireEvent.input(nome, { target: { value: 'Al' } });
      expect(nome.checkValidity()).toBe(true);
      fireEvent.input(nome, { target: { value: 'A'.repeat(100) } });
      expect(nome.checkValidity()).toBe(true);
      fireEvent.input(nome, { target: { value: 'A'.repeat(101) } });
      expect(nome.checkValidity()).toBe(false);
    },
  );

  it('CAD-4 - Verifica a remoção de espaços excedentes do nome sem alterar letras maiúsculas ou minúsculas.', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: NOME }) as HTMLInputElement;
    fireEvent.input(nome, { target: { value: '  mARIA   sILVA  ' } });
    fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
    expect(nome.value).toBe('mARIA sILVA');
  });

  it(
    'CAD-5 - Garante que o cadastro é bloqueado quando o e-mail está vazio, contém somente espaços ou é ' +
      'inválido e que a mensagem “Por favor, informe um e-mail válido” é exibida.',
    async () => {
      await render(Cadastro);
      const email = screen.getByRole('textbox', { name: EMAIL }) as HTMLInputElement;
      const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
      fireEvent.click(criarConta);
      expect(email.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
      expect(screen.getByText(ERRO_EMAIL)).toBeTruthy();
      fireEvent.input(email, { target: { value: 'maria@example.com' } });
      expect(email.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
      expect(screen.queryByText(ERRO_EMAIL)).toBeNull();
      fireEvent.input(email, { target: { value: '   ' } });
      fireEvent.click(criarConta);
      expect(email.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
      expect(screen.getByText(ERRO_EMAIL)).toBeTruthy();
      fireEvent.input(email, { target: { value: 'email-invalido' } });
      fireEvent.click(criarConta);
      expect(email.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
      expect(screen.getByText(ERRO_EMAIL)).toBeTruthy();
    },
  );

  it(
    'CAD-6 - Confirma que formatos inválidos e endereços com 255 caracteres são rejeitados e que um endereço ' +
      'válido com 254 caracteres é aceito.',
    async () => {
      await render(Cadastro);
      const email = screen.getByRole('textbox', { name: EMAIL }) as HTMLInputElement;
      for (const valor of ['email-invalido', 'maria@', '@example.com']) {
        fireEvent.input(email, { target: { value: valor } });
        expect(email.checkValidity()).toBe(false);
      }
      fireEvent.input(email, { target: { value: EMAIL_254 } });
      expect(email.checkValidity()).toBe(true);
      fireEvent.input(email, { target: { value: EMAIL_255 } });
      expect(email.checkValidity()).toBe(false);
    },
  );

  it('CAD-7 - Testa o impedimento do cadastro quando a senha está vazia.', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    fireEvent.input(senha, { target: { value: SENHA_VALIDA } });
    expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
  });

  it(
    'CAD-8 - Confirma que senhas com 7 ou 129 caracteres são rejeitadas e senhas com 8 ou 128 caracteres são ' +
      'aceitas.',
    async () => {
      await render(Cadastro);
      const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
      fireEvent.input(senha, { target: { value: CARACTERE_SENHA.repeat(7) } });
      expect(senha.checkValidity()).toBe(false);
      fireEvent.input(senha, { target: { value: CARACTERE_SENHA.repeat(8) } });
      expect(senha.checkValidity()).toBe(true);
      fireEvent.input(senha, { target: { value: CARACTERE_SENHA.repeat(128) } });
      expect(senha.checkValidity()).toBe(true);
      fireEvent.input(senha, { target: { value: CARACTERE_SENHA.repeat(129) } });
      expect(senha.checkValidity()).toBe(false);
    },
  );

  it('CAD-9 - Verifica a preservação dos espaços digitados no início, no meio e no fim da senha.', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const valor = '  senha  com espaços  ';
    fireEvent.input(senha, { target: { value: valor } });
    fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
    expect(senha.value).toBe(valor);
  });

  it('CAD-10 - Testa o impedimento do cadastro quando a confirmação da senha está vazia.', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const confirmacao = screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(confirmacao.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    fireEvent.input(senha, { target: { value: SENHA_VALIDA } });
    fireEvent.input(confirmacao, { target: { value: SENHA_VALIDA } });
    expect(confirmacao.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
  });

  it(
    'CAD-11 - Confirma a rejeição de uma confirmação que difere da senha por caractere, capitalização ou ' +
      'espaço.',
    async () => {
      await render(Cadastro);
      const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
      const confirmacao = screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement;
      fireEvent.input(senha, { target: { value: SENHA_SEGURA } });
      for (const valor of ['Senha segurA', 'Senha segura!', 'Senha  segura']) {
        fireEvent.input(confirmacao, { target: { value: valor } });
        expect(confirmacao.checkValidity()).toBe(false);
      }
      fireEvent.input(confirmacao, { target: { value: SENHA_SEGURA } });
      expect(confirmacao.checkValidity()).toBe(true);
    },
  );

  it('CAD-12 - Verifica se cada controle alterna somente a visibilidade de seu campo e preserva o valor digitado.', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const confirmacao = screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement;
    fireEvent.input(senha, { target: { value: SENHA_VALIDA } });
    fireEvent.input(confirmacao, { target: { value: SENHA_VALIDA } });
    const botoesMostrar = screen.getAllByRole('button', { name: MOSTRAR });
    fireEvent.click(botoesMostrar[0]);
    expect(senha.type).toBe('text');
    expect(confirmacao.type).toBe('password');
    fireEvent.click(botoesMostrar[1]);
    expect(senha.type).toBe('text');
    expect(confirmacao.type).toBe('text');
    fireEvent.click(screen.getAllByRole('button', { name: OCULTAR })[0]);
    expect(senha.type).toBe('password');
    expect(confirmacao.type).toBe('text');
    expect(senha.value).toBe(SENHA_VALIDA);
    expect(confirmacao.value).toBe(SENHA_VALIDA);
  });

  it('CAD-13 - Confirma que um envio inválido apresenta todos os erros e não solicita a criação da conta.', async () => {
    await render(Cadastro);
    const campos = [
      screen.getByRole('textbox', { name: NOME }),
      screen.getByRole('textbox', { name: EMAIL }),
      screen.getByLabelText(SENHA),
      screen.getByLabelText(CONFIRMAR_SENHA),
    ];
    fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
    for (const campo of campos)
      expect(campo.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    expect(screen.getAllByRole('alert')).toHaveLength(4);
  });

  it(
    'CAD-14 - Verifica se uma resposta de e-mail duplicado é reconhecida pelo serviço e faz a tela informar ' +
      '“E-mail já foi cadastrado”, sem autenticar ou sair do cadastro.',
    async () => {
      const cadastroService = {
        cadastrar: vi.fn().mockReturnValue(throwError(() => new EmailJaCadastradoError())),
      };
      await render(Cadastro, {
        providers: [{ provide: CadastroService, useValue: cadastroService }],
      });
      preencherCadastroValido();
      expect((await screen.findByRole('dialog')).textContent).toContain(EMAIL_JA_CADASTRADO);
      expect(screen.getByRole('heading', { name: TITULO_CADASTRO })).toBeTruthy();
      expect(cadastroService.cadastrar).toHaveBeenCalledOnce();
    },
  );

  it(
    'CAD-15 - Confirma que uma falha no cadastro apresenta a mensagem geral de erro, não exibe sucesso e não ' +
      'autentica o usuário.',
    async () => {
      const cadastroService = { cadastrar: vi.fn().mockReturnValue(throwError(() => new Error())) };
      await render(Cadastro, {
        providers: [{ provide: CadastroService, useValue: cadastroService }],
      });
      preencherCadastroValido();
      expect(await screen.findByText(ERRO_GERAL_CADASTRO)).toBeTruthy();
      expect(screen.getByRole('heading', { name: TITULO_CADASTRO })).toBeTruthy();
      expect(screen.queryByRole('heading', { name: 'Minhas Listas' })).toBeNull();
      expect(cadastroService.cadastrar).toHaveBeenCalledOnce();
    },
  );

  it('CAD-16 - Confirma que uma criação bem-sucedida autentica o usuário e abre “Minhas Listas”.', async () => {
    const cadastroService = { cadastrar: vi.fn().mockReturnValue(of(SESSION_RESPONSE)) };
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    await render(Cadastro, {
      providers: [
        { provide: CadastroService, useValue: cadastroService },
        { provide: Router, useValue: router },
      ],
    });
    preencherCadastroValido();
    expect(cadastroService.cadastrar).toHaveBeenCalledOnce();
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_LISTAS);
  });
});
