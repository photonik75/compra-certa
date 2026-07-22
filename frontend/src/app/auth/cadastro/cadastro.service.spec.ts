import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CadastroService, EmailJaCadastradoError } from './cadastro.service';

const ENDPOINT_CADASTRO = '/api/v1/auth/registrations';
const DADOS_CADASTRO = {
  name: 'Maria',
  email: 'maria@example.com',
  password: 'senha123',
  passwordConfirmation: 'senha123',
};
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: DADOS_CADASTRO.name,
    email: DADOS_CADASTRO.email,
    status: 'ACTIVE',
    createdAt: '2026-07-22T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-23T00:00:00Z',
};

describe('Testes unitários do CadastroService', () => {
  let service: CadastroService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CadastroService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('SCA-1 - Envia os dados de cadastro por POST e retorna a sessão recebida.', async () => {
    const respostaPendente = firstValueFrom(service.cadastrar(DADOS_CADASTRO));
    const requisicao = httpTesting.expectOne(ENDPOINT_CADASTRO);
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual(DADOS_CADASTRO);
    requisicao.flush(SESSION_RESPONSE);
    await expect(respostaPendente).resolves.toEqual(SESSION_RESPONSE);
  });

  it('SCA-2 - Traduz 409 Conflict para EmailJaCadastradoError.', async () => {
    const respostaPendente = firstValueFrom(service.cadastrar(DADOS_CADASTRO));
    httpTesting.expectOne(ENDPOINT_CADASTRO).flush({}, { status: 409, statusText: 'Conflict' });
    await expect(respostaPendente).rejects.toBeInstanceOf(EmailJaCadastradoError);
  });

  it('SCA-3 - Preserva erros diferentes de 409 Conflict.', async () => {
    const respostaPendente = firstValueFrom(service.cadastrar(DADOS_CADASTRO));
    httpTesting.expectOne(ENDPOINT_CADASTRO).flush({}, { status: 500, statusText: 'Server Error' });
    await expect(respostaPendente).rejects.toBeInstanceOf(HttpErrorResponse);
    await expect(respostaPendente).rejects.toMatchObject({ status: 500 });
  });
});
