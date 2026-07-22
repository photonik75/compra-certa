import { Provider } from '@angular/core';
import { fireEvent, render as renderComponent, screen } from '@testing-library/angular';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { RecuperacaoSenha } from './recuperacao-senha';
import { RecuperacaoSenhaService } from './recuperacao-senha.service';

const TITULO = 'Recupere sua senha';
const EMAIL = 'E-mail';
const ENVIAR = 'Enviar instruções';
const SOLICITAR_NOVAMENTE = 'Solicitar novamente';
const EMAIL_EXISTENTE = 'maria@example.com';
const EMAIL_INEXISTENTE = 'ninguem@example.com';
const EMAIL_INVALIDO = 'email-invalido';
const CONFIRMACAO = 'Se houver uma conta para este e-mail, enviaremos as instruções';
const SUCESSO_ENVIO = 'Solicitação de recuperação enviada com sucesso.';
const ERRO_EMAIL = 'Por favor, informe um e-mail válido';
const ERRO_ENVIO = 'Não foi possível enviar as instruções. Tente novamente mais tarde.';
const MENSAGEM_PROCESSAMENTO = 'Enviando instruções';

async function render(_component: typeof RecuperacaoSenha, options: { providers?: Provider[] } = {}) {
  const service = { solicitar: vi.fn().mockReturnValue(NEVER) };
  return renderComponent(RecuperacaoSenha, {
    providers: [
      { provide: RecuperacaoSenhaService, useValue: service },
      ...(options.providers ?? []),
    ],
  });
}

function informarEmail(email: string): void {
  fireEvent.input(screen.getByRole('textbox', { name: EMAIL }), { target: { value: email } });
}

function enviar(email: string): void {
  informarEmail(email);
  fireEvent.click(screen.getByRole('button', { name: ENVIAR }));
}

describe('Testes unitários do componente RecuperacaoSenha', () => {
  it('REC-1 - Apresenta título acessível, campo E-mail e botão de envio.', async () => {
    await render(RecuperacaoSenha);
    expect(screen.getByRole('heading', { name: TITULO })).toBeTruthy();
    expect(screen.getByRole('textbox', { name: EMAIL })).toBeTruthy();
    expect(screen.getByRole('button', { name: ENVIAR })).toBeTruthy();
  });

  it('REC-2 - Impede a solicitação quando o e-mail está vazio ou é inválido.', async () => {
    const service = { solicitar: vi.fn().mockReturnValue(NEVER) };
    await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
    const email = screen.getByRole('textbox', { name: EMAIL }) as HTMLInputElement;
    fireEvent.blur(email);
    expect(screen.getByText(ERRO_EMAIL)).toBeTruthy();
    informarEmail(EMAIL_INVALIDO);
    fireEvent.blur(email);
    expect(screen.getByText(ERRO_EMAIL)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: ENVIAR }));
    expect(service.solicitar).not.toHaveBeenCalled();
  });

  it.each([EMAIL_EXISTENTE, EMAIL_INEXISTENTE])(
    'REC-3 - Apresenta a mesma confirmação para e-mails existentes e inexistentes: %s.',
    async (email) => {
      const service = { solicitar: vi.fn().mockReturnValue(of(undefined)) };
      await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
      enviar(email);
      expect(screen.getByRole('status').textContent).toBe(CONFIRMACAO);
    },
  );

  it('REC-4 - Confirma uma solicitação aceita sem revelar se a conta existe.', async () => {
    const service = { solicitar: vi.fn().mockReturnValue(of(undefined)) };
    await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
    enviar(EMAIL_EXISTENTE);
    const mensagem = screen.getByRole('status').textContent ?? '';
    expect(service.solicitar).toHaveBeenCalledWith(EMAIL_EXISTENTE);
    expect(mensagem).toBe(CONFIRMACAO);
    expect(mensagem).not.toContain(EMAIL_EXISTENTE);
  });

  it('REC-5 - Permite iniciar uma nova solicitação após a anterior.', async () => {
    const service = { solicitar: vi.fn().mockReturnValue(of(undefined)) };
    await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
    enviar(EMAIL_EXISTENTE);
    fireEvent.click(screen.getByRole('button', { name: SOLICITAR_NOVAMENTE }));
    expect(screen.getByRole('textbox', { name: EMAIL })).toBeTruthy();
    enviar(EMAIL_INEXISTENTE);
    expect(service.solicitar).toHaveBeenCalledTimes(2);
  });

  it('REC-6 - Desabilita o envio e impede novas solicitações durante o processamento.', async () => {
    const resposta = new Subject<void>();
    const service = { solicitar: vi.fn().mockReturnValue(resposta) };
    await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
    informarEmail(EMAIL_EXISTENTE);
    const enviarSolicitacao = screen.getByRole('button', { name: ENVIAR }) as HTMLButtonElement;
    fireEvent.click(enviarSolicitacao);
    fireEvent.click(enviarSolicitacao);
    expect(enviarSolicitacao.disabled).toBe(true);
    expect(service.solicitar).toHaveBeenCalledOnce();
  });

  it('REC-7 - Mantém a recuperação aberta e apresenta uma mensagem quando o envio falha.', async () => {
    const service = { solicitar: vi.fn().mockReturnValue(throwError(() => new Error())) };
    await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
    enviar(EMAIL_EXISTENTE);
    expect((await screen.findByRole('alert')).textContent).toBe(ERRO_ENVIO);
    expect((screen.getByRole('textbox', { name: EMAIL }) as HTMLInputElement).value).toBe(EMAIL_EXISTENTE);
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('REC-8 - Apresenta uma mensagem explícita quando a solicitação é enviada com sucesso.', async () => {
    const service = { solicitar: vi.fn().mockReturnValue(of(undefined)) };
    await render(RecuperacaoSenha, { providers: [{ provide: RecuperacaoSenhaService, useValue: service }] });
    enviar(EMAIL_EXISTENTE);
    expect(screen.getByText(SUCESSO_ENVIO)).toBeTruthy();
    expect(screen.getByRole('status').textContent).toBe(CONFIRMACAO);
  });

  it('FOR-1 - Apresenta os estados inicial, erro por campo, processamento, sucesso e erro geral.', async () => {
    const resposta = new Subject<void>();
    const service = { solicitar: vi.fn().mockReturnValue(resposta) };
    const { fixture } = await render(RecuperacaoSenha, {
      providers: [{ provide: RecuperacaoSenhaService, useValue: service }],
    });
    const email = screen.getByRole('textbox', { name: EMAIL }) as HTMLInputElement;
    const botao = screen.getByRole('button', { name: ENVIAR }) as HTMLButtonElement;
    expect.soft(botao.disabled).toBe(true);
    expect.soft(screen.queryByRole('alert')).toBeNull();
    fireEvent.blur(email);
    expect.soft(screen.getByRole('alert').textContent).toBe(ERRO_EMAIL);
    informarEmail(EMAIL_EXISTENTE);
    fireEvent.click(botao);
    expect.soft(botao.disabled).toBe(true);
    expect.soft(screen.queryByRole('status', { name: MENSAGEM_PROCESSAMENTO })).toBeTruthy();
    resposta.next();
    resposta.complete();
    fixture.detectChanges();
    expect.soft(screen.getByRole('status').textContent).toBe(CONFIRMACAO);
    fireEvent.click(screen.getByRole('button', { name: SOLICITAR_NOVAMENTE }));
    service.solicitar.mockReturnValue(throwError(() => new Error()));
    enviar(EMAIL_EXISTENTE);
    expect.soft((await screen.findByRole('alert')).textContent).toBe(ERRO_ENVIO);
  });
});
