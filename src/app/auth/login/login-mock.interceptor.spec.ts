import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { loginMockInterceptor } from './login-mock.interceptor';
import { LoginService } from './login.service';

const ENDPOINT_LOGIN = '/api/v1/auth/sessions';
const ENDPOINT_FORA_ESCOPO = '/api/v1/outro-recurso';
const DADOS_LOGIN = {
  email: 'maria@example.com',
  password: 'senha123',
  manterConectado: false,
};

describe('Testes de integração do loginMockInterceptor', () => {
  let loginService: LoginService;
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loginMockInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    loginService = TestBed.inject(LoginService);
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  it('MOCK-LOG-1 - Intercepta o login e retorna uma sessão simulada.', async () => {
    const respostaPendente = firstValueFrom(loginService.entrar(DADOS_LOGIN));
    httpTesting.expectNone(ENDPOINT_LOGIN);
    await vi.runAllTimersAsync();
    const resposta = await respostaPendente;
    expect(resposta.user.email).toBe(DADOS_LOGIN.email);
    expect(resposta.csrfToken).toBeTruthy();
    expect(resposta.expiresAt).toBeTruthy();
  });

  it('MOCK-LOG-2 - Encaminha requisições fora do endpoint de login.', async () => {
    const respostaPendente = firstValueFrom(http.get(ENDPOINT_FORA_ESCOPO));
    const requisicao = httpTesting.expectOne(ENDPOINT_FORA_ESCOPO);
    requisicao.flush({ ok: true });
    await expect(respostaPendente).resolves.toEqual({ ok: true });
  });
});
