import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import {
  CredenciaisInvalidasError,
  LoginService,
  MuitasTentativasError,
} from './login.service';

const ENDPOINT_LOGIN = '/api/v1/auth/sessions';
const DADOS_LOGIN = {
  email: 'maria@example.com',
  password: 'senha123',
  manterConectado: true,
};
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: 'Maria',
    email: DADOS_LOGIN.email,
    status: 'ACTIVE',
    createdAt: '2026-07-22T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-23T00:00:00Z',
};

describe('Testes unitários do LoginService', () => {
  let service: LoginService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(LoginService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('SLO-1 - Envia os dados de login por POST e retorna a sessão recebida.', async () => {
    const respostaPendente = firstValueFrom(service.entrar(DADOS_LOGIN));
    const requisicao = httpTesting.expectOne(ENDPOINT_LOGIN);
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual(DADOS_LOGIN);
    requisicao.flush(SESSION_RESPONSE);
    await expect(respostaPendente).resolves.toEqual(SESSION_RESPONSE);
  });

  it('SLO-2 - Traduz 401 Unauthorized para CredenciaisInvalidasError.', async () => {
    const respostaPendente = firstValueFrom(service.entrar(DADOS_LOGIN));
    httpTesting.expectOne(ENDPOINT_LOGIN).flush({}, { status: 401, statusText: 'Unauthorized' });
    await expect(respostaPendente).rejects.toBeInstanceOf(CredenciaisInvalidasError);
  });

  it('SLO-3 - Traduz 429 Too Many Requests para MuitasTentativasError.', async () => {
    const respostaPendente = firstValueFrom(service.entrar(DADOS_LOGIN));
    httpTesting.expectOne(ENDPOINT_LOGIN).flush({}, { status: 429, statusText: 'Too Many Requests' });
    await expect(respostaPendente).rejects.toBeInstanceOf(MuitasTentativasError);
  });

  it('SLO-4 - Preserva erros diferentes de 401 e 429.', async () => {
    const respostaPendente = firstValueFrom(service.entrar(DADOS_LOGIN));
    httpTesting.expectOne(ENDPOINT_LOGIN).flush({}, { status: 500, statusText: 'Server Error' });
    await expect(respostaPendente).rejects.toBeInstanceOf(HttpErrorResponse);
    await expect(respostaPendente).rejects.toMatchObject({ status: 500 });
  });
});
