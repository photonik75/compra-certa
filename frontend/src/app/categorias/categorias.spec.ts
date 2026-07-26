import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Categoria, CategoriasService } from './categorias.service';
import { Categorias } from './categorias';

const CATEGORIES: Categoria[] = [
  { id: '1', name: 'Bebidas', icon: '🧃', activeProductCount: 0, version: 1 },
  { id: '2', name: 'Hortifruti', icon: '🥬', activeProductCount: 2, version: 1 },
  { id: '3', name: 'Limpeza', icon: '🧴', activeProductCount: 0, version: 1 },
  { id: '4', name: 'Mercearia', icon: '🛍️', activeProductCount: 0, version: 1 },
];

describe('Categorias - EF03', () => {
  let fixture: ComponentFixture<Categorias>;
  let service: {
    listar: ReturnType<typeof vi.fn>;
    criar: ReturnType<typeof vi.fn>;
    atualizar: ReturnType<typeof vi.fn>;
    excluir: ReturnType<typeof vi.fn>;
    obter: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    service = {
      listar: vi.fn().mockReturnValue(of({ items: CATEGORIES, page: { nextCursor: null, hasMore: false } })),
      criar: vi.fn().mockReturnValue(of(CATEGORIES[0])),
      atualizar: vi.fn().mockReturnValue(of(CATEGORIES[0])),
      excluir: vi.fn().mockReturnValue(of(undefined)),
      obter: vi.fn().mockReturnValue(of(CATEGORIES[0])),
    };
    await TestBed.configureTestingModule({
      imports: [Categorias],
      providers: [{ provide: CategoriasService, useValue: service }],
    }).compileComponents();
    fixture = TestBed.createComponent(Categorias);
    fixture.detectChanges();
  });

  it('FE-CAT-01/02 - renderiza controles e categorias iniciais ordenadas', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Categorias');
    expect(text).toContain('Pesquisar categorias');
    expect(text).toContain('Nova categoria');
    const cards = [...fixture.nativeElement.querySelectorAll('[data-testid="category"]')];
    expect(cards.map((card: Element) => card.querySelector('h2')?.textContent?.trim()))
      .toEqual(['Bebidas', 'Hortifruti', 'Limpeza', 'Mercearia']);
    expect(text).toContain('🧃');
    expect(text).toContain('🥬');
    expect(text).toContain('🧴');
    expect(text).toContain('🛍️');
    expect(text).toContain('0 produtos ativos');
  });

  it('FE-CAT-03 - normaliza e limita pesquisa e permite limpar estado vazio', () => {
    service.listar.mockReturnValue(of({ items: [], page: { nextCursor: null, hasMore: false } }));
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = `  graos   ${'x'.repeat(50)}`;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(service.listar).toHaveBeenLastCalledWith({ search: `graos ${'x'.repeat(34)}` });
    expect(fixture.nativeElement.textContent).toContain('Nenhuma categoria encontrada para esta pesquisa.');
    fixture.componentInstance.clearSearch();
    expect(service.listar).toHaveBeenLastCalledWith({ search: '' });
  });

  it('FE-CAT-04/13 - abre edição preenchida e usa somente o serviço injetado', () => {
    fixture.componentInstance.openEdit(CATEGORIES[0]);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.value).toEqual({ name: 'Bebidas', icon: '🧃' });
    expect(fixture.nativeElement.querySelector('[role="dialog"]').textContent).toContain('Editar categoria');
    expect(service.listar).toHaveBeenCalled();
  });

  it('FE-CAT-05/06 - valida nome, duplicidade e ícone permitido com mensagens normativas', () => {
    fixture.componentInstance.openCreate();
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Por favor, informe o nome da categoria.');
    expect(fixture.nativeElement.textContent).toContain('Por favor, escolha um ícone disponível.');
    fixture.componentInstance.form.setValue({ name: 'x'.repeat(41), icon: 'inválido' });
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('O nome da categoria deve ter no máximo 40 caracteres.');
    service.criar.mockReturnValue(throwError(() => ({ error: { code: 'CATEGORY_NAME_ALREADY_IN_USE' } })));
    fixture.componentInstance.form.setValue({ name: ' bebidas ', icon: '🧃' });
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Você já possui uma categoria com este nome.');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="icon-option"]')).toHaveLength(8);
  });

  it('FE-CAT-07 - bloqueia reenvio, normaliza e inclui criação ordenada', () => {
    const response = new Subject<Categoria>();
    service.criar.mockReturnValue(response);
    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({ name: '  Açougue   Central ', icon: '🍞' });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    expect(service.criar).toHaveBeenCalledOnce();
    expect(service.criar).toHaveBeenCalledWith({ name: 'Açougue Central', icon: '🍞' });
    response.next({ id: '5', name: 'Açougue Central', icon: '🍞', activeProductCount: 0, version: 1 });
    response.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Categoria criada com sucesso.');
    expect(fixture.componentInstance.categories[0].name).toBe('Açougue Central');
  });

  it('FE-CAT-07 - preserva diálogo e campos no erro de criação', () => {
    service.criar.mockReturnValue(throwError(() => new Error('offline')));
    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({ name: 'Padaria', icon: '🍞' });
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeTruthy();
    expect(fixture.componentInstance.form.value).toEqual({ name: 'Padaria', icon: '🍞' });
    expect(fixture.nativeElement.textContent).toContain('Não foi possível criar a categoria.');
  });

  it('FE-CAT-08 - desabilita sem mudança e atualiza a lista no sucesso', () => {
    fixture.componentInstance.openEdit(CATEGORIES[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    fixture.componentInstance.form.controls.name.setValue('Sucos');
    fixture.componentInstance.form.controls.name.markAsDirty();
    service.atualizar.mockReturnValue(of({ ...CATEGORIES[0], name: 'Sucos', version: 2 }));
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(service.atualizar).toHaveBeenCalledWith('1', { name: 'Sucos' }, 1);
    expect(fixture.componentInstance.categories.find((category) => category.id === '1')?.name).toBe('Sucos');
    expect(fixture.nativeElement.textContent).toContain('Categoria atualizada com sucesso.');
  });

  it('FE-CAT-09 - preserva campos e oferece recarga após conflito', () => {
    service.atualizar.mockReturnValue(throwError(() => ({ error: { code: 'CONFLICT' } })));
    fixture.componentInstance.openEdit(CATEGORIES[0]);
    fixture.componentInstance.form.controls.name.setValue('Minha versão');
    fixture.componentInstance.form.controls.name.markAsDirty();
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Esta categoria foi alterada em outro lugar.');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
    expect(fixture.componentInstance.form.controls.name.value).toBe('Minha versão');
  });

  it('FE-CAT-10 - bloqueia categoria em uso e confirma e remove categoria livre', () => {
    fixture.componentInstance.requestDelete(CATEGORIES[1]);
    fixture.detectChanges();
    expect(service.excluir).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Esta categoria possui 2 produtos ativos.');
    fixture.componentInstance.requestDelete(CATEGORIES[2]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Excluir a categoria ‘Limpeza’?");
    fixture.componentInstance.confirmDelete();
    fixture.detectChanges();
    expect(service.excluir).toHaveBeenCalledWith('3', 1);
    expect(fixture.componentInstance.categories.some((category) => category.id === '3')).toBe(false);
  });

  it('FE-CAT-11/14 - Escape fecha sem mutação e devolve foco ao acionador', () => {
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
