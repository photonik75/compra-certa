import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cadastroMockInterceptor } from './cadastro-mock.interceptor';
import { CadastroService } from './cadastro.service';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';
const ENDPOINT_FORA_ESCOPO = '/api/v1/outro-recurso';
const DADOS_CADASTRO = {
  name: 'Maria',
  email: 'maria@example.com',
  password: 'senha123',
  passwordConfirmation: 'senha123',
};

describe('Testes de integração do cadastroMockInterceptor', () => {
  let cadastroService: CadastroService;
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([cadastroMockInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    cadastroService = TestBed.inject(CadastroService);
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  it('MOCK-CAD-1 - Intercepta o cadastro e retorna uma sessão simulada.', async () => {
    const respostaPendente = firstValueFrom(cadastroService.cadastrar(DADOS_CADASTRO));
    httpTesting.expectNone(ENDPOINT_CADASTRO);
    await vi.runAllTimersAsync();
    const resposta = await respostaPendente;
    expect(resposta.user.name).toBe(DADOS_CADASTRO.name);
    expect(resposta.user.email).toBe(DADOS_CADASTRO.email);
    expect(resposta.csrfToken).toBeTruthy();
    expect(resposta.expiresAt).toBeTruthy();
  });

  it('MOCK-CAD-2 - Encaminha requisições fora do endpoint de cadastro.', async () => {
    const respostaPendente = firstValueFrom(http.get(ENDPOINT_FORA_ESCOPO));
    const requisicao = httpTesting.expectOne(ENDPOINT_FORA_ESCOPO);
    requisicao.flush({ ok: true });
    await expect(respostaPendente).resolves.toEqual({ ok: true });
  });
});
