import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Produto, ProdutosService } from './produtos.service';
import { Produtos } from './produtos';

const CATEGORIES = [
  { id: 'c1', name: 'Bebidas', icon: '🧃', available: true },
  { id: 'c2', name: 'Mercearia', icon: '🛍️', available: true },
];
const PRODUCTS: Produto[] = [
  { id: '1', name: 'Arroz', category: CATEGORIES[1], defaultUnit: 'PACKAGE', active: true, version: 1 },
  { id: '2', name: 'Leite', category: CATEGORIES[0], defaultUnit: 'LITER', active: true, version: 2 },
];

describe('Produtos - EF04', () => {
  let fixture: ComponentFixture<Produtos>;
  let service: any;

  beforeEach(async () => {
    service = {
      listar: vi.fn().mockReturnValue(of({ items: PRODUCTS, page: { nextCursor: null, hasMore: false } })),
      listarCategorias: vi.fn().mockReturnValue(of(CATEGORIES)),
      criar: vi.fn().mockReturnValue(of(PRODUCTS[0])),
      atualizar: vi.fn().mockReturnValue(of(PRODUCTS[0])),
      desativar: vi.fn().mockReturnValue(of(undefined)),
      obter: vi.fn().mockReturnValue(of(PRODUCTS[0])),
    };
    await TestBed.configureTestingModule({
      imports: [Produtos],
      providers: [{ provide: ProdutosService, useValue: service }],
    }).compileComponents();
    fixture = TestBed.createComponent(Produtos);
    fixture.detectChanges();
  });

  it('FE-PROD-01 - renderiza ativos ordenados, controles, dados e aviso histórico', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Produtos');
    expect(text).toContain('Pesquisar produtos');
    expect(text).toContain('Todas as categorias');
    expect(text).toContain('Novo produto');
    expect(text).toContain('Arroz');
    expect(text).toContain('Mercearia');
    expect(text).toContain('pacote');
    expect(text).toContain('Histórico preservado');
  });

  it('FE-PROD-02 - normaliza busca, combina categoria e limpa critérios', () => {
    service.listar.mockReturnValue(of({ items: [], page: { nextCursor: null, hasMore: false } }));
    fixture.componentInstance.selectCategory('c1');
    fixture.componentInstance.search(`  leite   ${'x'.repeat(60)}`);
    fixture.detectChanges();
    expect(service.listar).toHaveBeenLastCalledWith({
      search: `leite ${'x'.repeat(54)}`, categoryId: 'c1', status: 'ACTIVE', limit: 30,
    });
    expect(fixture.nativeElement.textContent).toContain('Nenhum produto encontrado para esta pesquisa.');
    fixture.componentInstance.clearFilters();
    expect(service.listar).toHaveBeenLastCalledWith({
      search: '', categoryId: '', status: 'ACTIVE', limit: 30,
    });
  });

  it('FE-PROD-03/14 - ações abrem o produto correto por meio do serviço', () => {
    fixture.componentInstance.openEdit(PRODUCTS[1]);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.value).toEqual({
      name: 'Leite', categoryId: 'c1', defaultUnit: 'LITER',
    });
    expect(fixture.nativeElement.textContent).toContain('Editar produto');
    expect(fixture.nativeElement.querySelectorAll('[data-action="edit"]')).toHaveLength(2);
  });

  it('FE-PROD-04/05/06 - valida nome, categoria e unidade com mensagens normativas', () => {
    fixture.componentInstance.openCreate();
    fixture.componentInstance.save();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Por favor, informe o nome do produto.');
    expect(text).toContain('Por favor, escolha uma categoria.');
    expect(text).toContain('Por favor, escolha uma unidade disponível.');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="unit-option"]')).toHaveLength(13);
    expect(fixture.nativeElement.querySelectorAll('[data-testid="category-option"]')).toHaveLength(2);
    service.criar.mockReturnValue(throwError(() => ({ error: { code: 'PRODUCT_NAME_ALREADY_IN_USE' } })));
    fixture.componentInstance.form.setValue({ name: ' arroz ', categoryId: 'c2', defaultUnit: 'UNIT' });
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Você já possui um produto ativo com este nome.');
  });

  it('FE-PROD-07 - bloqueia reenvio, normaliza, fecha e inclui no sucesso', () => {
    const response = new Subject<Produto>();
    service.criar.mockReturnValue(response);
    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({ name: '  Café   moído ', categoryId: 'c2', defaultUnit: 'PACKAGE' });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    expect(service.criar).toHaveBeenCalledOnce();
    expect(service.criar).toHaveBeenCalledWith({
      name: 'Café moído', categoryId: 'c2', defaultUnit: 'PACKAGE',
    });
    response.next({ ...PRODUCTS[0], id: '3', name: 'Café moído' });
    response.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Produto criado com sucesso.');
  });

  it('FE-PROD-08/09 - salva somente alterações e preserva dados locais no conflito', () => {
    fixture.componentInstance.openEdit(PRODUCTS[0]);
    fixture.componentInstance.form.controls.name.setValue('Arroz integral');
    fixture.componentInstance.form.controls.name.markAsDirty();
    service.atualizar.mockReturnValue(throwError(() => ({ error: { code: 'CONFLICT' } })));
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(service.atualizar).toHaveBeenCalledWith('1', { name: 'Arroz integral' }, 1);
    expect(fixture.nativeElement.textContent).toContain('Este produto foi alterado em outro lugar.');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
    expect(fixture.componentInstance.form.controls.name.value).toBe('Arroz integral');
  });

  it('FE-PROD-10 - confirma efeito e remove produto no sucesso', () => {
    fixture.componentInstance.openDeactivate(PRODUCTS[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Desativar o produto ‘Arroz’?");
    fixture.componentInstance.confirmDeactivate();
    fixture.detectChanges();
    expect(service.desativar).toHaveBeenCalledWith('1', 1);
    expect(fixture.componentInstance.products.some((product) => product.id === '1')).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Produto desativado com sucesso.');
  });

  it('FE-PROD-12/15 - Escape fecha sem mutação e devolve foco', () => {
    const trigger: HTMLButtonElement = fixture.nativeElement.querySelector('[data-action="create"]');
    trigger.focus();
    trigger.click();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
    expect(service.criar).not.toHaveBeenCalled();
  });
});
