import { Provider } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { fireEvent, render as renderComponent, screen } from '@testing-library/angular';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { RedefinicaoSenha } from './redefinicao-senha';
import {
  LinkRecuperacaoInvalidoError,
  RedefinicaoSenhaService,
} from './redefinicao-senha.service';

const TITULO = 'Redefina sua senha';
const NOVA_SENHA = 'Nova senha';
const CONFIRMAR_NOVA_SENHA = 'Confirmar nova senha';
const CONFIRMAR = 'Redefinir senha';
const MOSTRAR = 'Mostrar';
const OCULTAR = 'Ocultar';
const SOLICITAR_NOVAMENTE = 'Solicitar nova recuperação';
const TOKEN = 'token-recuperacao';
const SENHA_VALIDA = 'senha123';
const ROTA_LOGIN = '/entrar';

async function render(options: { providers?: Provider[] } = {}) {
  const service = { redefinir: vi.fn().mockReturnValue(NEVER) };
  const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
  return renderComponent(RedefinicaoSenha, {
    providers: [
      { provide: RedefinicaoSenhaService, useValue: service },
      { provide: Router, useValue: router },
      { provide: ActivatedRoute, useValue: { snapshot: { fragment: `token=${TOKEN}` } } },
      ...(options.providers ?? []),
    ],
  });
}

function preencherSenhas(senha = SENHA_VALIDA, confirmacao = senha): void {
  fireEvent.input(screen.getByLabelText(NOVA_SENHA), { target: { value: senha } });
  fireEvent.input(screen.getByLabelText(CONFIRMAR_NOVA_SENHA), {
    target: { value: confirmacao },
  });
}

describe('Testes unitários do componente RedefinicaoSenha', () => {
  it('RED-1 - Apresenta título, campos, controles de visibilidade e confirmação.', async () => {
    await render();
    expect(screen.getByRole('heading', { name: TITULO })).toBeTruthy();
    expect(screen.getByLabelText(NOVA_SENHA)).toBeTruthy();
    expect(screen.getByLabelText(CONFIRMAR_NOVA_SENHA)).toBeTruthy();
    expect(screen.getAllByRole('button', { name: MOSTRAR })).toHaveLength(2);
    expect(screen.getByRole('button', { name: CONFIRMAR })).toBeTruthy();
  });

  it('RED-2 - Rejeita nova senha vazia, com 7 ou 129 caracteres e aceita de 8 a 128.', async () => {
    await render();
    const senha = screen.getByLabelText(NOVA_SENHA) as HTMLInputElement;
    for (const tamanho of [0, 7, 129]) {
      fireEvent.input(senha, { target: { value: 'a'.repeat(tamanho) } });
      expect.soft(senha.checkValidity()).toBe(false);
    }
    for (const tamanho of [8, 128]) {
      fireEvent.input(senha, { target: { value: 'a'.repeat(tamanho) } });
      expect.soft(senha.checkValidity()).toBe(true);
    }
  });

  it('RED-3 - Rejeita confirmação vazia ou diferente da nova senha.', async () => {
    await render();
    const confirmacao = screen.getByLabelText(CONFIRMAR_NOVA_SENHA) as HTMLInputElement;
    fireEvent.input(screen.getByLabelText(NOVA_SENHA), { target: { value: SENHA_VALIDA } });
    for (const valor of ['', 'senhaDiferente']) {
      fireEvent.input(confirmacao, { target: { value: valor } });
      expect.soft(confirmacao.checkValidity()).toBe(false);
    }
    fireEvent.input(confirmacao, { target: { value: SENHA_VALIDA } });
    expect(confirmacao.checkValidity()).toBe(true);
  });

  it('RED-4 - Alterna cada campo de senha sem alterar seu conteúdo.', async () => {
    await render();
    preencherSenhas();
    const senha = screen.getByLabelText(NOVA_SENHA) as HTMLInputElement;
    const confirmacao = screen.getByLabelText(CONFIRMAR_NOVA_SENHA) as HTMLInputElement;
    const botoes = screen.getAllByRole('button', { name: MOSTRAR });
    fireEvent.click(botoes[0]);
    expect.soft(senha.type).toBe('text');
    expect.soft(confirmacao.type).toBe('password');
    fireEvent.click(botoes[1]);
    fireEvent.click(screen.getAllByRole('button', { name: OCULTAR })[0]);
    expect.soft(senha.type).toBe('password');
    expect.soft(confirmacao.type).toBe('text');
    expect.soft(senha.value).toBe(SENHA_VALIDA);
    expect.soft(confirmacao.value).toBe(SENHA_VALIDA);
  });

  it('RED-5 - Token inválido não altera a senha e oferece nova solicitação.', async () => {
    const service = {
      redefinir: vi.fn().mockReturnValue(throwError(() => new LinkRecuperacaoInvalidoError())),
    };
    await render({ providers: [{ provide: RedefinicaoSenhaService, useValue: service }] });
    preencherSenhas();
    fireEvent.click(screen.getByRole('button', { name: CONFIRMAR }));
    expect(service.redefinir).toHaveBeenCalledWith({
      token: TOKEN,
      newPassword: SENHA_VALIDA,
      passwordConfirmation: SENHA_VALIDA,
    });
    expect(screen.getByRole('link', { name: SOLICITAR_NOVAMENTE })).toBeTruthy();
    expect(screen.queryByText(/senha redefinida com sucesso/i)).toBeNull();
  });

  it('RED-6 - Sucesso encerra a sessão local e abre o login.', async () => {
    const service = { redefinir: vi.fn().mockReturnValue(of(undefined)) };
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    await render({
      providers: [
        { provide: RedefinicaoSenhaService, useValue: service },
        { provide: Router, useValue: router },
      ],
    });
    preencherSenhas();
    fireEvent.click(screen.getByRole('button', { name: CONFIRMAR }));
    expect(service.redefinir).toHaveBeenCalledOnce();
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_LOGIN);
  });

  it('RED-7 - Desabilita a confirmação e impede novo envio durante o processamento.', async () => {
    const service = { redefinir: vi.fn().mockReturnValue(NEVER) };
    await render({ providers: [{ provide: RedefinicaoSenhaService, useValue: service }] });
    preencherSenhas();
    const confirmar = screen.getByRole('button', { name: CONFIRMAR }) as HTMLButtonElement;
    fireEvent.click(confirmar);
    fireEvent.click(confirmar);
    expect.soft(confirmar.disabled).toBe(true);
    expect(service.redefinir).toHaveBeenCalledOnce();
  });
});
