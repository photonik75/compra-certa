import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ListaItensService } from './lista-itens.service';

describe('ListaItensService - FE-ITEM-13/FE-SHOP-10', () => {
  let service: ListaItensService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ListaItensService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ListaItensService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('lista e cria item com decimal string e idempotência', () => {
    service.listar('l1').subscribe();
    http.expectOne('/api/v1/lists/l1/items?limit=30').flush({ items: [] });
    service.criar('l1', {
      productId: 'p1', quantity: '1.5', unit: 'KILOGRAM', categoryId: 'c1', notes: null,
    }).subscribe();
    const request = http.expectOne('/api/v1/lists/l1/items');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.quantity).toBe('1.5');
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({});
  });

  it('edita, remove e marca usando versão e rotas contratuais', () => {
    service.atualizar('l1', 'i1', { quantity: '2' }, 3).subscribe();
    const patch = http.expectOne('/api/v1/lists/l1/items/i1');
    expect(patch.request.headers.get('If-Match')).toBe('"3"');
    expect(patch.request.headers.has('Idempotency-Key')).toBe(true);
    patch.flush({});
    service.remover('l1', 'i1', 4).subscribe();
    const remove = http.expectOne('/api/v1/lists/l1/items/i1');
    expect(remove.request.method).toBe('DELETE');
    remove.flush({});
    service.marcar('l1', 'i1', true, 5).subscribe();
    const check = http.expectOne('/api/v1/lists/l1/items/i1/checked');
    expect(check.request.method).toBe('PUT');
    expect(check.request.body).toEqual({ checked: true });
    expect(check.request.headers.get('If-Match')).toBe('"5"');
    check.flush({});
  });
});
