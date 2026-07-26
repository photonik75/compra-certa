import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CicloVidaListaService } from './ciclo-vida-lista.service';

describe('CicloVidaListaService - FE-LIFE-10', () => {
  let service: CicloVidaListaService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CicloVidaListaService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CicloVidaListaService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('altera status e exclui com versão e idempotência', () => {
    service.alterarStatus('l1', 'COMPLETED', 2).subscribe();
    const status = http.expectOne('/api/v1/lists/l1/status');
    expect(status.request.method).toBe('PUT');
    expect(status.request.body).toEqual({ status: 'COMPLETED' });
    expect(status.request.headers.get('If-Match')).toBe('"2"');
    expect(status.request.headers.has('Idempotency-Key')).toBe(true);
    status.flush({});
    service.excluir('l1', 3).subscribe();
    const remove = http.expectOne('/api/v1/lists/l1');
    expect(remove.request.method).toBe('DELETE');
    expect(remove.request.headers.get('If-Match')).toBe('"3"');
    remove.flush(null);
  });
});
