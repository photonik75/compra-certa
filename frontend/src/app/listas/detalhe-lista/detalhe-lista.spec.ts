import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ListaItensService } from '../lista-itens.service';
import { SincronizacaoListaService } from '../sincronizacao-lista.service';
import { DetalheLista } from './detalhe-lista';

const ITEMS = [
  {
    id: '2', product: { id: 'p2', name: 'Banana' }, category: { id: 'c1', name: 'Hortifruti', icon: '🥬' },
    quantity: '1.5', unit: 'KILOGRAM', notes: 'madura', checked: false, version: 1,
  },
  {
    id: '1', product: { id: 'p1', name: 'Arroz' }, category: { id: 'c2', name: 'Mercearia', icon: '🛍️' },
    quantity: '2', unit: 'PACKAGE', notes: null, checked: true, checkedBy: { name: 'Ana' },
    checkedAt: '2026-07-26T10:00:00Z', version: 2,
  },
];
const COLLECTION = {
  items: ITEMS, page: { nextCursor: null, hasMore: false },
  listSummary: { total: 2, checked: 1, pending: 1, percentage: 50 }, listVersion: 3,
};

describe('DetalheLista - EF05/EF06', () => {
  let itemsService: any;
  let sync: any;
  let connection: BehaviorSubject<boolean>;
  let events: Subject<any>;

  beforeEach(async () => {
    connection = new BehaviorSubject(true);
    events = new Subject();
    itemsService = {
      listar: vi.fn().mockReturnValue(of(COLLECTION)),
      marcar: vi.fn().mockReturnValue(of({
        item: { ...ITEMS[0], checked: true, checkedBy: { name: 'Bia' }, checkedAt: '2026-07-26T11:00:00Z' },
        listSummary: { total: 2, checked: 2, pending: 0, percentage: 100 }, listVersion: 4,
      })),
      remover: vi.fn().mockReturnValue(of({
        deletedItemId: '2', listSummary: { total: 1, checked: 1, pending: 0, percentage: 100 }, listVersion: 4,
      })),
    };
    sync = { connection$: connection, events$: events, connect: vi.fn(), disconnect: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [DetalheLista],
      providers: [
        { provide: ListaItensService, useValue: itemsService },
        { provide: SincronizacaoListaService, useValue: sync },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['listId', 'l1']]) } } },
      ],
    }).compileComponents();
  });

  it('FE-ITEM-01/02 e FE-SHOP-01 - agrupa snapshots e mostra resumo e gestão', () => {
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Hortifruti');
    expect(text).toContain('Mercearia');
    expect(text).toContain('Banana');
    expect(text).toContain('1,5 quilograma');
    expect(text).toContain('madura');
    expect(text).toContain('Total 2');
    expect(text).toContain('Comprados 1');
    expect(text).toContain('Pendentes 1');
    expect(text).toContain('50%');
    expect(fixture.nativeElement.querySelectorAll('input[type="checkbox"]')).toHaveLength(2);
  });

  it('FE-SHOP-03/04/07 - marca uma vez, mantém ordem e aplica confirmação do servidor', () => {
    const response = new Subject<any>();
    itemsService.marcar.mockReturnValue(response);
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    const order = fixture.componentInstance.items.map((item: any) => item.id);
    fixture.componentInstance.toggle(ITEMS[0]);
    fixture.componentInstance.toggle(ITEMS[0]);
    fixture.detectChanges();
    expect(itemsService.marcar).toHaveBeenCalledOnce();
    expect(itemsService.marcar).toHaveBeenCalledWith('l1', '2', true, 1);
    response.next({
      item: { ...ITEMS[0], checked: true, checkedBy: { name: 'Bia' }, checkedAt: '2026-07-26T11:00:00Z' },
      listSummary: { total: 2, checked: 2, pending: 0, percentage: 100 },
    });
    response.complete();
    fixture.detectChanges();
    expect(fixture.componentInstance.items.map((item: any) => item.id)).toEqual(order);
    expect(fixture.nativeElement.textContent).toContain('Bia');
    expect(fixture.nativeElement.textContent).toContain('Comprados 2');
  });

  it('FE-SHOP-04/05 - reverte falha e aplica estado atual em conflito', () => {
    itemsService.marcar.mockReturnValue(throwError(() => ({
      error: { code: 'CONFLICT', meta: {
        item: { ...ITEMS[0], checked: true, version: 4 },
        listSummary: { total: 2, checked: 2, pending: 0, percentage: 100 },
      } },
    })));
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    fixture.componentInstance.toggle(ITEMS[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('A lista foi atualizada em outro lugar.');
    expect(fixture.componentInstance.items[0].checked).toBe(true);
  });

  it('FE-SHOP-06/08/09/11 - recebe evento da lista, ignora outra e ressincroniza ao reconectar', () => {
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    connection.next(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Sem conexão');
    fixture.componentInstance.toggle(ITEMS[0]);
    expect(itemsService.marcar).not.toHaveBeenCalled();
    events.next({ listId: 'outra', payload: { item: { ...ITEMS[0], checked: true } } });
    events.next({ listId: 'l1', payload: {
      item: { ...ITEMS[0], checked: true },
      listSummary: { total: 2, checked: 2, pending: 0, percentage: 100 },
    } });
    expect(fixture.componentInstance.items[0].checked).toBe(true);
    connection.next(true);
    expect(itemsService.listar).toHaveBeenCalledTimes(2);
  });

  it('FE-ITEM-11/15 - confirma remoção e anuncia resumo atualizado', () => {
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    fixture.componentInstance.requestRemove(ITEMS[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Remover ‘Banana’?");
    fixture.componentInstance.confirmRemove();
    fixture.detectChanges();
    expect(fixture.componentInstance.items.some((item: any) => item.id === '2')).toBe(false);
    expect(fixture.nativeElement.querySelector('[aria-live="polite"]')).toBeTruthy();
  });
});
