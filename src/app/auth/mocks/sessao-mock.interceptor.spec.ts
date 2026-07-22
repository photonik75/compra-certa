import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ENDPOINT_LOGOUT, ENDPOINT_SESSAO, SessaoService } from '../sessao.service';
import { criarSessaoMock } from './criar-sessao-mock';
import { sessaoMockInterceptor } from './sessao-mock.interceptor';
import { SessaoMockStore } from './sessao-mock.store';

const EMAIL = 'maria@example.com';
const SESSAO = criarSessaoMock({ name: 'Maria', email: EMAIL });

describe('Testes de integração do sessaoMockInterceptor', () => {
  let service: SessaoService;
  let store: SessaoMockStore;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessaoMockInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(SessaoService);
    store = TestBed.inject(SessaoMockStore);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('MOCK-SES-1 - Retorna a sessão armazenada sem acessar o backend.', async () => {
    store.salvar(SESSAO);
    const resposta = await firstValueFrom(service.consultar());
    httpTesting.expectNone(ENDPOINT_SESSAO);
    expect(resposta).toEqual(SESSAO);
  });

  it('MOCK-SES-2 - Retorna 401 quando não existe sessão armazenada.', async () => {
    const respostaPendente = firstValueFrom(service.consultar());
    httpTesting.expectNone(ENDPOINT_SESSAO);
    await expect(respostaPendente).rejects.toMatchObject({ status: 401 });
  });

  it('MOCK-SES-3 - Remove a sessão armazenada ao sair sem acessar o backend.', async () => {
    store.salvar(SESSAO);
    await firstValueFrom(service.sair());
    httpTesting.expectNone(ENDPOINT_LOGOUT);
    expect(store.obter()).toBeNull();
  });
});
