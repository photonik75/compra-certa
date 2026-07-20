import { fireEvent, render, screen } from '@testing-library/angular';
import { Cadastro } from './cadastro';

const NOME = 'Nome';
const EMAIL = 'E-mail';
const SENHA = 'Senha';
const CONFIRMAR_SENHA = 'Confirmar senha';
const CRIAR_CONTA = 'Criar conta';
const ERRO_NOME = 'Por favor, informe seu nome';
const ERRO_EMAIL = 'Por favor, informe um e-mail válido';
const ARIA_INVALIDO = 'true';
const ARIA_VALIDO = 'false';
const ATRIBUTO_ARIA_INVALIDO = 'aria-invalid';
const EMAIL_254 = `${'a'.repeat(64)}@${'b'.repeat(63)}.${'c'.repeat(63)}.${'d'.repeat(61)}`;
const EMAIL_255 = `${EMAIL_254}d`;

describe('Testes unitários do componente Cadastro', () => {
  it('CAD-1 - renderiza todos os campos e controles do cadastro', async () => {
    await render(Cadastro);

    expect(
      screen.getByRole('heading', { name: 'Crie sua conta' }),
    ).toBeTruthy();
    expect(screen.getByRole('textbox', { name: NOME })).toBeTruthy();
    expect(screen.getByRole('textbox', { name: EMAIL })).toBeTruthy();
    expect((screen.getByLabelText(SENHA) as HTMLInputElement).type).toBe(
      'password',
    );
    expect(
      (screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement).type,
    ).toBe('password');
    expect(
      screen.getAllByRole('button', { name: /^(mostrar|ocultar)$/i }),
    ).toHaveLength(2);
    expect(
      screen.getByRole('button', { name: CRIAR_CONTA }),
    ).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Entrar' })).toBeTruthy();
  });

  it('CAD-2 - rejeita nome vazio ou composto somente por espaços', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: NOME }) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(screen.getByText(ERRO_NOME)).toBeTruthy();
    fireEvent.input(nome, { target: { value: 'Maria' } });
    expect(screen.queryByText(ERRO_NOME)).toBeNull();
    fireEvent.input(nome, { target: { value: '   ' } });
    fireEvent.click(criarConta);
    expect(screen.getByText(ERRO_NOME)).toBeTruthy();
  });

  it('CAD-3 - aceita nome de 2 a 100 caracteres', async () => {
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
  });

  it('CAD-4 - normaliza espaços sem modificar a capitalização do nome', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: NOME }) as HTMLInputElement;
    fireEvent.input(nome, { target: { value: '  mARIA   sILVA  ' } });
    fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
    expect(nome.value).toBe('mARIA sILVA');
  });

  it('CAD-5 - rejeita e-mail não informado corretamente', async () => {
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
  });

  it('CAD-6 - aceita e-mail válido com até 254 caracteres', async () => {
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
  });

  it('CAD-7 - rejeita senha vazia', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    fireEvent.input(senha, { target: { value: 'senha123' } });
    expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
  });

  it('CAD-8 - aceita senha de 8 a 128 caracteres', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    fireEvent.input(senha, { target: { value: 'a'.repeat(7) } });
    expect(senha.checkValidity()).toBe(false);
    fireEvent.input(senha, { target: { value: 'a'.repeat(8) } });
    expect(senha.checkValidity()).toBe(true);
    fireEvent.input(senha, { target: { value: 'a'.repeat(128) } });
    expect(senha.checkValidity()).toBe(true);
    fireEvent.input(senha, { target: { value: 'a'.repeat(129) } });
    expect(senha.checkValidity()).toBe(false);
  });

  it('CAD-9 - preserva espaços no início, meio e fim da senha', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const valor = '  senha  com espaços  ';
    fireEvent.input(senha, { target: { value: valor } });
    fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
    expect(senha.value).toBe(valor);
  });

  it('CAD-10 - rejeita confirmação de senha vazia', async () => {
    await render(Cadastro);
    const confirmacao = screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(confirmacao.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    fireEvent.input(confirmacao, { target: { value: 'senha123' } });
    expect(confirmacao.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
  });

  it('CAD-11 - exige confirmação idêntica à senha', async () => {
    await render(Cadastro);
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const confirmacao = screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement;
    fireEvent.input(senha, { target: { value: 'Senha segura' } });
    for (const valor of ['Senha segurA', 'Senha segura!', 'Senha  segura']) {
      fireEvent.input(confirmacao, { target: { value: valor } });
      expect(confirmacao.checkValidity()).toBe(false);
    }
    fireEvent.input(confirmacao, { target: { value: 'Senha segura' } });
    expect(confirmacao.checkValidity()).toBe(true);
  });
});
