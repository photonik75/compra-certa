import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ListasService } from '../listas.service';
import { NovaLista } from './nova-lista';

describe('NovaLista - EF02', () => {
  let fixture: ComponentFixture<NovaLista>;
  let service: { criar: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { criar: vi.fn().mockReturnValue(of({ id: 'nova' })) };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [NovaLista],
      providers: [
        { provide: ListasService, useValue: service },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: {} },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(NovaLista);
    fixture.detectChanges();
  });

  it('FE-LIS-08 - valida nome e descrição com mensagens normativas', () => {
    fixture.componentInstance.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Por favor, informe o nome da lista.');
    fixture.componentInstance.form.setValue({ name: 'x'.repeat(61), description: 'y'.repeat(241) });
    fixture.componentInstance.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('O nome da lista deve ter no máximo 60 caracteres.');
    expect(fixture.nativeElement.textContent).toContain('A descrição deve ter no máximo 240 caracteres.');
  });

  it('FE-LIS-08 - apresenta conflito de unicidade retornado pelo serviço', () => {
    service.criar.mockReturnValue(throwError(() => ({ error: { code: 'LIST_NAME_ALREADY_IN_USE' } })));
    fixture.componentInstance.form.setValue({ name: 'Feira', description: '' });
    fixture.componentInstance.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Você já possui uma lista com este nome.');
  });

  it('FE-LIS-09 - bloqueia reenvio, normaliza valores e navega no sucesso', () => {
    const response = new Subject<{ id: string }>();
    service.criar.mockReturnValue(response);
    fixture.componentInstance.form.setValue({ name: '  Feira   mensal ', description: '  observar preços  ' });
    fixture.componentInstance.salvar();
    fixture.componentInstance.salvar();
    expect(service.criar).toHaveBeenCalledOnce();
    expect(service.criar).toHaveBeenCalledWith({ name: 'Feira mensal', description: 'observar preços' });
    response.next({ id: 'abc' });
    response.complete();
    expect(router.navigate).toHaveBeenCalledWith(['/listas', 'abc']);
  });

  it('FE-LIS-09/15 - preserva formulário, anuncia erro e associa erros aos campos', () => {
    service.criar.mockReturnValue(throwError(() => new Error('offline')));
    fixture.componentInstance.form.setValue({ name: 'Feira', description: 'texto' });
    fixture.componentInstance.salvar();
    fixture.detectChanges();
    expect(fixture.componentInstance.form.value).toEqual({ name: 'Feira', description: 'texto' });
    const alerts = [...fixture.nativeElement.querySelectorAll('[role="alert"]')]
      .map((element: Element) => element.textContent).join(' ');
    expect(alerts)
      .toContain('Não foi possível criar a lista.');
    expect(fixture.nativeElement.querySelector('input[aria-describedby]')).toBeTruthy();
  });
});
