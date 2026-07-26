import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ListasService } from './listas.service';

describe('ListasService - FE-LIS-13', () => {
  let service: ListasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ListasService, provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ListasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta com status, pesquisa, cursor e limite contratados', () => {
    service.listar({ status: 'ALL', search: 'feira', cursor: 'c2' }).subscribe();
    const request = http.expectOne(
      (req) => req.url === '/api/v1/lists' && req.params.get('status') === 'ALL'
        && req.params.get('search') === 'feira' && req.params.get('cursor') === 'c2'
        && req.params.get('limit') === '30',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ items: [], page: {}, summary: {} });
  });

  it('cria com chave idempotente e atualiza com versão', () => {
    service.criar({ name: 'Feira', description: null }).subscribe();
    const create = http.expectOne('/api/v1/lists');
    expect(create.request.method).toBe('POST');
    expect(create.request.headers.has('Idempotency-Key')).toBe(true);
    create.flush({ id: '1' });
    service.atualizar('1', { name: 'Nova feira' }, 4).subscribe();
    const update = http.expectOne('/api/v1/lists/1');
    expect(update.request.method).toBe('PATCH');
    expect(update.request.headers.get('If-Match')).toBe('"4"');
    update.flush({ id: '1' });
  });
});
