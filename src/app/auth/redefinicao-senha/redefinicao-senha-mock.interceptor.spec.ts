import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionResponse } from '../models/session-response';
import { SessaoMockStore } from '../mocks/sessao-mock.store';
import {
  redefinicaoSenhaMockInterceptor,
  TOKEN_REDEFINICAO_MOCK,
} from './redefinicao-senha-mock.interceptor';
import {
  ENDPOINT_REDEFINICAO,
  LinkRecuperacaoInvalidoError,
  RedefinicaoSenhaService,
} from './redefinicao-senha.service';

const ENDPOINT_FORA_ESCOPO = '/api/v1/outro-recurso';
const SENHA = 'novaSenha123';
const SESSION_RESPONSE: SessionResponse = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: 'Maria',
    email: 'maria@example.com',
    status: 'ACTIVE',
    createdAt: '2026-07-22T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-23T00:00:00Z',
};

function dados(token: string) {
  return { token, newPassword: SENHA, passwordConfirmation: SENHA };
}

describe('Testes de integração do redefinicaoSenhaMockInterceptor', () => {
  let service: RedefinicaoSenhaService;
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let store: SessaoMockStore;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([redefinicaoSenhaMockInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RedefinicaoSenhaService);
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    store = TestBed.inject(SessaoMockStore);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  it('MOCK-RED-1 - Simula sucesso e encerra a sessão armazenada.', async () => {
    store.salvar(SESSION_RESPONSE);
    const respostaPendente = firstValueFrom(service.redefinir(dados(TOKEN_REDEFINICAO_MOCK)));
    httpTesting.expectNone(ENDPOINT_REDEFINICAO);
    await vi.runAllTimersAsync();
    await expect(respostaPendente).resolves.toBeNull();
    expect(store.obter()).toBeNull();
  });

  it('MOCK-RED-2 - Simula link inválido sem encerrar a sessão armazenada.', async () => {
    store.salvar(SESSION_RESPONSE);
    const respostaPendente = firstValueFrom(service.redefinir(dados('token-invalido')));
    httpTesting.expectNone(ENDPOINT_REDEFINICAO);
    await expect(respostaPendente).rejects.toBeInstanceOf(LinkRecuperacaoInvalidoError);
    expect(store.obter()).toEqual(SESSION_RESPONSE);
  });

  it('MOCK-RED-3 - Encaminha requisições fora do endpoint de redefinição.', async () => {
    const respostaPendente = firstValueFrom(http.get(ENDPOINT_FORA_ESCOPO));
    const requisicao = httpTesting.expectOne(ENDPOINT_FORA_ESCOPO);
    requisicao.flush({ ok: true });
    await expect(respostaPendente).resolves.toEqual({ ok: true });
  });
});
