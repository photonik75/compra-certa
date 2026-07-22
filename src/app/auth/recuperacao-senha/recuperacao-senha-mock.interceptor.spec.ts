import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  EMAIL_RECUPERACAO_MOCK,
  recuperacaoSenhaMockInterceptor,
} from './recuperacao-senha-mock.interceptor';
import { ENDPOINT_RECUPERACAO, RecuperacaoSenhaService } from './recuperacao-senha.service';

const EMAIL_FALHA = 'falha@example.com';

describe('Testes de integração do recuperacaoSenhaMockInterceptor', () => {
  let service: RecuperacaoSenhaService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([recuperacaoSenhaMockInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RecuperacaoSenhaService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.useRealTimers();
  });

  it('MOCK-REC-1 - Simula sucesso para o e-mail configurado.', async () => {
    const respostaPendente = firstValueFrom(service.solicitar(EMAIL_RECUPERACAO_MOCK));
    httpTesting.expectNone(ENDPOINT_RECUPERACAO);
    await vi.runAllTimersAsync();
    await expect(respostaPendente).resolves.toBeNull();
  });

  it('MOCK-REC-2 - Simula falha para outro e-mail.', async () => {
    const respostaPendente = firstValueFrom(service.solicitar(EMAIL_FALHA));
    httpTesting.expectNone(ENDPOINT_RECUPERACAO);
    await expect(respostaPendente).rejects.toMatchObject({ status: 503 });
  });
});
