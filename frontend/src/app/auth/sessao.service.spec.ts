import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ENDPOINT_LOGOUT, ENDPOINT_SESSAO, SessaoService } from './sessao.service';

const CSRF_TOKEN = 'csrf-token';
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: 'Maria',
    email: 'maria@example.com',
    status: 'ACTIVE',
    createdAt: '2026-07-22T12:00:00Z',
  },
  csrfToken: CSRF_TOKEN,
  expiresAt: '2026-07-23T00:00:00Z',
};

describe('Testes unitários do SessaoService', () => {
  let service: SessaoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SessaoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('consulta a sessão e envia o token CSRF ao sair.', async () => {
    const respostaPendente = firstValueFrom(service.sair());
    httpTesting.expectOne(ENDPOINT_SESSAO).flush(SESSION_RESPONSE);
    const requisicao = httpTesting.expectOne(ENDPOINT_LOGOUT);
    expect(requisicao.request.method).toBe('DELETE');
    expect(requisicao.request.headers.get('X-CSRF-Token')).toBe(CSRF_TOKEN);
    requisicao.flush(null, { status: 204, statusText: 'No Content' });
    await expect(respostaPendente).resolves.toBeNull();
  });
});