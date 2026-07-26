import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ListaItensService } from '../lista-itens.service';
import { EditarItem } from './editar-item';

const ITEM = {
  id: 'i1', product: { id: 'p1', name: 'Arroz' },
  category: { id: 'c1', name: 'Mercearia', icon: '🛍️' },
  quantity: '2', unit: 'PACKAGE', notes: 'integral', checked: true, version: 3,
};

describe('EditarItem - EF05', () => {
  let service: any;
  beforeEach(async () => {
    service = { obter: vi.fn().mockReturnValue(of(ITEM)), atualizar: vi.fn().mockReturnValue(of({ item: ITEM })) };
    await TestBed.configureTestingModule({
      imports: [EditarItem],
      providers: [
        { provide: ListaItensService, useValue: service },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ActivatedRoute, useValue: {
          snapshot: { paramMap: new Map([['listId', 'l1'], ['itemId', 'i1']]) },
        } },
      ],
    }).compileComponents();
  });

  it('FE-ITEM-09 - carrega campos e desabilita salvar sem mudança', () => {
    const fixture = TestBed.createComponent(EditarItem);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.value).toEqual({
      productId: 'p1', quantity: '2', unit: 'PACKAGE', categoryId: 'c1', notes: 'integral',
    });
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });

  it('FE-ITEM-10 - preserva dados locais e oferece recarga no conflito', () => {
    service.atualizar.mockReturnValue(throwError(() => ({ error: { code: 'CONFLICT' } })));
    const fixture = TestBed.createComponent(EditarItem);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.quantity.setValue('3');
    fixture.componentInstance.form.controls.quantity.markAsDirty();
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(service.atualizar).toHaveBeenCalledWith('l1', 'i1', { quantity: '3' }, 3);
    expect(fixture.nativeElement.textContent).toContain('Este item foi alterado em outro lugar.');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
    expect(fixture.componentInstance.form.controls.quantity.value).toBe('3');
  });
});
