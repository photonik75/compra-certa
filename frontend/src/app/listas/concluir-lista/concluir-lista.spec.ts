import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CicloVidaListaService } from '../ciclo-vida-lista.service';
import { ConcluirLista } from './concluir-lista';

const LIST = { id: 'l1', name: 'Feira', status: 'ACTIVE', role: 'OWNER', version: 2,
  summary: { total: 4, checked: 1, pending: 3, percentage: 25 } };

describe('ConcluirLista - EF07', () => {
  let service: any;
  let router: any;
  beforeEach(async () => {
    service = { alterarStatus: vi.fn().mockReturnValue(of({ ...LIST, status: 'COMPLETED', version: 3 })),
      excluir: vi.fn().mockReturnValue(of(undefined)) };
    router = { navigate: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ConcluirLista],
      providers: [{ provide: CicloVidaListaService, useValue: service }, { provide: Router, useValue: router }],
    }).compileComponents();
  });

  it('FE-LIFE-01/04/05 - mostra consequência, pendentes e ações conforme estado/papel', () => {
    const fixture = TestBed.createComponent(ConcluirLista);
    fixture.componentRef.setInput('lista', LIST);
    fixture.detectChanges();
    fixture.componentInstance.open('complete');
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('3 itens pendentes');
    expect(text).toContain('Concluir');
    fixture.componentInstance.close();
    fixture.componentRef.setInput('lista', { ...LIST, status: 'COMPLETED' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('somente para consulta');
    expect(fixture.nativeElement.textContent).toContain('Reabrir');
    expect(fixture.nativeElement.textContent).toContain('Excluir');
  });

  it('FE-LIFE-02/03 - confirma uma vez e preserva estado no conflito', () => {
    const pending = new Subject<any>();
    service.alterarStatus.mockReturnValue(pending);
    const fixture = TestBed.createComponent(ConcluirLista);
    fixture.componentRef.setInput('lista', LIST);
    fixture.detectChanges();
    fixture.componentInstance.open('complete');
    fixture.componentInstance.confirm();
    fixture.componentInstance.confirm();
    expect(service.alterarStatus).toHaveBeenCalledOnce();
    pending.error({ error: { code: 'CONFLICT' } });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('A lista foi alterada em outro lugar.');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
  });

  it('FE-LIFE-06/07 - reabre ou exclui e redireciona após sucesso', () => {
    const fixture = TestBed.createComponent(ConcluirLista);
    fixture.componentRef.setInput('lista', { ...LIST, status: 'COMPLETED' });
    fixture.detectChanges();
    fixture.componentInstance.open('reopen');
    fixture.componentInstance.confirm();
    expect(service.alterarStatus).toHaveBeenCalledWith('l1', 'ACTIVE', 2);
    fixture.componentInstance.open('delete');
    fixture.componentInstance.confirm();
    expect(service.excluir).toHaveBeenCalledWith('l1', 3);
    expect(router.navigate).toHaveBeenCalledWith(['/listas']);
  });

  it('FE-LIFE-09/12 - Escape fecha sem chamada e devolve foco', () => {
    const fixture = TestBed.createComponent(ConcluirLista);
    fixture.componentRef.setInput('lista', LIST);
    fixture.detectChanges();
    const trigger: HTMLButtonElement = fixture.nativeElement.querySelector('[data-action="complete"]');
    trigger.focus();
    trigger.click();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
    expect(service.alterarStatus).not.toHaveBeenCalled();
  });
});
