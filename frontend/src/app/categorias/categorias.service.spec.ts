import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CategoriasService } from './categorias.service';

describe('CategoriasService - FE-CAT-12', () => {
  let service: CategoriasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CategoriasService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoriasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista categorias com pesquisa e limite', () => {
    service.listar({ search: 'grãos' }).subscribe();
    const request = http.expectOne(
      (req) => req.url === '/api/v1/categories' && req.params.get('search') === 'grãos'
        && req.params.get('limit') === '30',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ items: [], page: {} });
  });

  it('cria com chave idempotente', () => {
    service.criar({ name: 'Padaria', icon: '🍞' }).subscribe();
    const request = http.expectOne('/api/v1/categories');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({ id: '1' });
  });

  it('consulta, atualiza e exclui usando versão', () => {
    service.obter('1').subscribe();
    const get = http.expectOne('/api/v1/categories/1');
    expect(get.request.method).toBe('GET');
    get.flush({ id: '1' });
    service.atualizar('1', { icon: '🐾' }, 2).subscribe();
    const patch = http.expectOne('/api/v1/categories/1');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.headers.get('If-Match')).toBe('"2"');
    patch.flush({ id: '1' });
    service.excluir('1', 3).subscribe();
    const remove = http.expectOne('/api/v1/categories/1');
    expect(remove.request.method).toBe('DELETE');
    expect(remove.request.headers.get('If-Match')).toBe('"3"');
    remove.flush(null);
  });
});
