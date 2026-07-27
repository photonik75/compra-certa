import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ProdutosService } from '../../produtos/produtos.service';
import { ListaItensService } from '../lista-itens.service';
import { NovoItem } from './novo-item';

describe('NovoItem - EF05', () => {
  let items: any;
  let products: any;
  let router: any;

  beforeEach(async () => {
    items = {
      criar: vi.fn().mockReturnValue(of({ item: { id: 'i1' } })),
      listar: vi.fn().mockReturnValue(of({ items: [], page: {}, listSummary: {} })),
    };
    products = {
      sugerir: vi.fn().mockReturnValue(of([{
        id: 'p1', name: 'Banana', category: { id: 'c1', name: 'Hortifruti', icon: '🥬' },
        defaultUnit: 'UNIT', active: true, version: 1,
      }])),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [NovoItem],
      providers: [
        { provide: ListaItensService, useValue: items },
        { provide: ProdutosService, useValue: products },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['listId', 'l1']]) } } },
      ],
    }).compileComponents();
  });

  it('FE-ITEM-03/05/06 - exige produto selecionado, quantidade válida e limita observação', () => {
    const fixture = TestBed.createComponent(NovoItem);
    fixture.detectChanges();
    fixture.componentInstance.save();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Selecione um produto válido na lista de sugestões.');
    expect(text).toContain('Informe uma quantidade maior que zero.');
    fixture.componentInstance.form.patchValue({ quantity: '1000000', notes: 'x'.repeat(241) });
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('A quantidade deve ser menor ou igual a 999999,99.');
    expect(fixture.nativeElement.textContent).toContain('A observação deve ter no máximo 240 caracteres.');
  });

  it('FE-ITEM-03/07 - sugere desde o primeiro caractere, envia snapshots derivados e bloqueia reenvio', () => {
    const response = new Subject<any>();
    items.criar.mockReturnValue(response);
    const fixture = TestBed.createComponent(NovoItem);
    fixture.detectChanges();
    fixture.componentInstance.searchProducts('b');
    expect(products.sugerir).toHaveBeenCalledWith('b');
    fixture.componentInstance.selectProduct({
      id: 'p1', name: 'Banana', category: { id: 'c1', name: 'Hortifruti', icon: '🥬', available: true },
      defaultUnit: 'UNIT', active: true, version: 1,
    });
    fixture.componentInstance.form.patchValue({ quantity: '1,50', notes: ' madura ' });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    expect(items.criar).toHaveBeenCalledOnce();
    expect(items.criar).toHaveBeenCalledWith('l1', {
      productId: 'p1', quantity: '1.5', unit: 'UNIT', categoryId: 'c1', notes: 'madura',
    });
    response.next({ item: { id: 'i1' } });
    response.complete();
    expect(router.navigate).toHaveBeenCalledWith(['/listas', 'l1'], { fragment: 'item-i1' });
  });

  it('FE-ITEM-07/08 - preserva formulário e apresenta duplicidade no erro', () => {
    items.criar.mockReturnValue(throwError(() => ({ error: { code: 'DUPLICATE_ITEM', meta: { item: { id: 'old' } } } })));
    items.listar.mockReturnValue(of({
      items: [{
        id: 'old',
        product: { id: 'p1', name: 'Banana' },
        category: { id: 'c1', name: 'Hortifruti', icon: '🥬' },
        quantity: '1',
        unit: 'UNIT',
        checked: false,
        version: 1,
      }],
      page: { nextCursor: null, hasMore: false },
      listSummary: { total: 1, checked: 0, pending: 1, percentage: 0 },
    }));
    const fixture = TestBed.createComponent(NovoItem);
    fixture.detectChanges();
    fixture.componentInstance.selectProduct({
      id: 'p1', name: 'Banana', category: { id: 'c1', name: 'Hortifruti', icon: '🥬', available: true },
      defaultUnit: 'UNIT', active: true, version: 1,
    });
    fixture.componentInstance.form.patchValue({ quantity: '2' });
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Produto já está na lista');
    expect(fixture.componentInstance.form.controls.quantity.value).toBe('2');
  });
});
