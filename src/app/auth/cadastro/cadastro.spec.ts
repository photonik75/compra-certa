import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { fireEvent, render, screen } from '@testing-library/angular';
import { routes } from '../../app.routes';
import { Cadastro } from './cadastro';

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
const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';
const EMAIL_JA_CADASTRADO = 'E-mail já foi cadastrado';
const ERRO_GERAL_CADASTRO = 'Ocorreu um erro ao tentar criar sua conta. Aguarde e tente novamente em alguns instantes.';
const MINHAS_LISTAS = 'Minhas Listas';
const ROTA_CADASTRO = '/cadastro';
const SESSION_RESPONSE = { user: { id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9', name: NOME_VALIDO, email: EMAIL_VALIDO, status: 'ACTIVE', createdAt: '2026-07-21T12:00:00Z' }, csrfToken: 'csrf-token', expiresAt: '2026-07-22T00:00:00Z' };

async function renderizarCadastroComHttp(): Promise<HttpTestingController> {
  await render(Cadastro, { providers: [provideHttpClient(), provideHttpClientTesting()] });
  return TestBed.inject(HttpTestingController);
}

function preencherCadastroValido(): void {
  fireEvent.input(screen.getByRole('textbox', { name: NOME }), { target: { value: NOME_VALIDO } });
  fireEvent.input(screen.getByRole('textbox', { name: EMAIL }), { target: { value: EMAIL_VALIDO } });
  fireEvent.input(screen.getByLabelText(SENHA), { target: { value: SENHA_VALIDA } });
  fireEvent.input(screen.getByLabelText(CONFIRMAR_SENHA), { target: { value: SENHA_VALIDA } });
  fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
}

describe('Testes unitários do componente Cadastro', () => {
  it('CAD-1 - renderiza todos os campos e controles do cadastro', async () => {
    await render(Cadastro);

    expect(
      screen.getByRole('heading', { name: TITULO_CADASTRO }),
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
    fireEvent.input(nome, { target: { value: NOME_VALIDO } });
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
    fireEvent.input(senha, { target: { value: SENHA_VALIDA } });
    expect(senha.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
  });

  it('CAD-8 - aceita senha de 8 a 128 caracteres', async () => {
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
    const senha = screen.getByLabelText(SENHA) as HTMLInputElement;
    const confirmacao = screen.getByLabelText(CONFIRMAR_SENHA) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: CRIAR_CONTA });
    fireEvent.click(criarConta);
    expect(confirmacao.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    fireEvent.input(senha, { target: { value: SENHA_VALIDA } });
    fireEvent.input(confirmacao, { target: { value: SENHA_VALIDA } });
    expect(confirmacao.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_VALIDO);
  });

  it('CAD-11 - exige confirmação idêntica à senha', async () => {
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
  });

  it('CAD-12 - alterna cada senha independentemente e preserva o conteúdo', async () => {
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

  it('CAD-13 - envio inválido mostra erros em todos os campos', async () => {
    await render(Cadastro);
    const campos = [
      screen.getByRole('textbox', { name: NOME }),
      screen.getByRole('textbox', { name: EMAIL }),
      screen.getByLabelText(SENHA),
      screen.getByLabelText(CONFIRMAR_SENHA),
    ];
    fireEvent.click(screen.getByRole('button', { name: CRIAR_CONTA }));
    for (const campo of campos) expect(campo.getAttribute(ATRIBUTO_ARIA_INVALIDO)).toBe(ARIA_INVALIDO);
    expect(screen.getAllByRole('alert')).toHaveLength(4);
  });

  it('CAD-14 - exibe pop-up quando o e-mail já está cadastrado', async () => {
    const http = await renderizarCadastroComHttp();
    preencherCadastroValido();
    const requisicao = http.expectOne(ENDPOINT_CADASTRO);
    expect(requisicao.request.method).toBe('POST');
    requisicao.flush({ status: 409, code: 'CONFLICT', fieldErrors: [{ field: 'email', message: EMAIL_JA_CADASTRADO }] }, { status: 409, statusText: 'Conflict' });
    expect((await screen.findByRole('dialog')).textContent).toContain(EMAIL_JA_CADASTRADO);
    expect(screen.getByRole('heading', { name: TITULO_CADASTRO })).toBeTruthy();
    http.verify();
  });

  it('CAD-15 - falha não cria conta parcialmente', async () => {
    const http = await renderizarCadastroComHttp();
    preencherCadastroValido();
    const requisicao = http.expectOne(ENDPOINT_CADASTRO);
    requisicao.flush({ status: 500, code: 'INTERNAL_ERROR' }, { status: 500, statusText: 'Internal Server Error' });
    expect(await screen.findByText(ERRO_GERAL_CADASTRO)).toBeTruthy();
    expect(screen.getByRole('heading', { name: TITULO_CADASTRO })).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Minhas Listas' })).toBeNull();
    http.verify();
  });

  it('CAD-16 - sucesso autentica e abre Minhas Listas', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()] });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(ROTA_CADASTRO, Cadastro);
    const http = TestBed.inject(HttpTestingController);
    preencherCadastroValido();
    const requisicao = http.expectOne(ENDPOINT_CADASTRO);
    requisicao.flush(SESSION_RESPONSE, { status: 201, statusText: 'Created' });
    await harness.fixture.whenStable();
    expect(screen.getByRole('heading', { name: MINHAS_LISTAS })).toBeTruthy();
    http.verify();
  });
});
