import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ListasService } from '../listas.service';
import { EditarLista } from './editar-lista';

const LISTA = { id: '1', name: 'Feira', description: 'Original', status: 'ACTIVE', role: 'OWNER', version: 3 };

describe('EditarLista - EF02', () => {
  let service: { obter: ReturnType<typeof vi.fn>; atualizar: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { obter: vi.fn().mockReturnValue(of(LISTA)), atualizar: vi.fn().mockReturnValue(of(LISTA)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [EditarLista],
      providers: [
        { provide: ListasService, useValue: service },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['listId', '1']]) } } },
      ],
    }).compileComponents();
  });

  it('FE-LIS-10 - carrega dados, permite remover descrição e desabilita salvar sem mudança', () => {
    const fixture = TestBed.createComponent(EditarLista);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.value).toEqual({ name: 'Feira', description: 'Original' });
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    fixture.componentInstance.form.controls.description.setValue('');
    fixture.componentInstance.form.controls.description.markAsDirty();
    expect(fixture.componentInstance.form.dirty).toBe(true);
  });

  it('FE-LIS-10 - restringe participante e lista concluída', () => {
    service.obter.mockReturnValue(of({ ...LISTA, role: 'EDITOR', status: 'COMPLETED' }));
    const fixture = TestBed.createComponent(EditarLista);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Esta lista está concluída e não pode ser editada.');
  });

  it('FE-LIS-11 - envia somente mudanças, bloqueia reenvio e navega no sucesso', () => {
    const response = new Subject<typeof LISTA>();
    service.atualizar.mockReturnValue(response);
    const fixture = TestBed.createComponent(EditarLista);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.name.setValue('Feira semanal');
    fixture.componentInstance.form.controls.name.markAsDirty();
    fixture.componentInstance.salvar();
    fixture.componentInstance.salvar();
    expect(service.atualizar).toHaveBeenCalledOnce();
    expect(service.atualizar).toHaveBeenCalledWith('1', { name: 'Feira semanal' }, 3);
    response.next({ ...LISTA, name: 'Feira semanal' });
    response.complete();
    expect(router.navigate).toHaveBeenCalledWith(['/listas', '1']);
  });

  it('FE-LIS-12 - mantém formulário e oferece recarga após conflito', () => {
    service.atualizar.mockReturnValue(throwError(() => ({ error: { code: 'CONFLICT' } })));
    const fixture = TestBed.createComponent(EditarLista);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.name.setValue('Minha alteração');
    fixture.componentInstance.form.controls.name.markAsDirty();
    fixture.componentInstance.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Esta lista foi alterada em outro lugar.');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
    expect(fixture.componentInstance.form.controls.name.value).toBe('Minha alteração');
  });
});
