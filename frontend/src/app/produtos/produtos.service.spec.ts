import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProdutosService } from './produtos.service';

describe('ProdutosService - FE-PROD-11/13', () => {
  let service: ProdutosService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ProdutosService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProdutosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta catálogo e seleção com os filtros contratados', () => {
    service.listar({ search: 'arroz', categoryId: 'c1', status: 'ACTIVE', limit: 30 }).subscribe();
    const list = http.expectOne((req) => req.url === '/api/v1/products'
      && req.params.get('search') === 'arroz' && req.params.get('categoryId') === 'c1'
      && req.params.get('status') === 'ACTIVE' && req.params.get('limit') === '30');
    list.flush({ items: [], page: {} });
    service.sugerir('leite').subscribe();
    const suggest = http.expectOne((req) => req.url === '/api/v1/products'
      && req.params.get('status') === 'ACTIVE' && req.params.get('limit') === '10'
      && req.params.get('search') === 'leite');
    suggest.flush({ items: [], page: {} });
  });

  it('cria, consulta, atualiza e desativa com cabeçalhos contratuais', () => {
    service.criar({ name: 'Arroz', categoryId: 'c1', defaultUnit: 'PACKAGE' }).subscribe();
    const create = http.expectOne('/api/v1/products');
    expect(create.request.headers.has('Idempotency-Key')).toBe(true);
    create.flush({ id: '1' });
    service.obter('1').subscribe();
    http.expectOne('/api/v1/products/1').flush({ id: '1' });
    service.atualizar('1', { name: 'Integral' }, 2).subscribe();
    const patch = http.expectOne('/api/v1/products/1');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.headers.get('If-Match')).toBe('"2"');
    patch.flush({ id: '1' });
    service.desativar('1', 3).subscribe();
    const remove = http.expectOne('/api/v1/products/1');
    expect(remove.request.method).toBe('DELETE');
    expect(remove.request.headers.get('If-Match')).toBe('"3"');
    remove.flush(null);
  });
});
