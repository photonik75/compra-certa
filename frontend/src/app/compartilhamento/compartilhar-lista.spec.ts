import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CompartilhamentoService } from './compartilhamento.service';
import { CompartilharLista } from './compartilhar-lista';

const ACCESS = {
  list: { id: 'l1', name: 'Feira', status: 'ACTIVE', role: 'OWNER', version: 2 },
  owner: { id: 'u1', name: 'Ana', email: 'ana@exemplo.com' },
  members: [{ id: 'u2', name: 'Bia', email: 'bia@exemplo.com' }],
  invitations: [{ id: 'i1', email: 'cai@exemplo.com', expiresAt: '2026-08-01', deliveryStatus: 'SENT', version: 1 }],
};

describe('CompartilharLista - EF08', () => {
  let service: any;
  beforeEach(async () => {
    service = {
      consultarAcesso: vi.fn().mockReturnValue(of(structuredClone(ACCESS))),
      convidar: vi.fn().mockReturnValue(of({ outcome: 'INVITATION_CREATED', invitation: ACCESS.invitations[0] })),
      reenviar: vi.fn().mockReturnValue(of(ACCESS.invitations[0])),
      cancelarConvite: vi.fn().mockReturnValue(of(undefined)),
      removerMembro: vi.fn().mockReturnValue(of(undefined)),
      sair: vi.fn().mockReturnValue(of(undefined)),
    };
    await TestBed.configureTestingModule({
      imports: [CompartilharLista],
      providers: [
        { provide: CompartilhamentoService, useValue: service },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['listId', 'l1']]) } } },
      ],
    }).compileComponents();
  });

  it('FE-SHARE-01/10 - mostra proprietário, membros, convites e ações do proprietário', () => {
    const fixture = TestBed.createComponent(CompartilharLista);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Ana');
    expect(text).toContain('Proprietário');
    expect(text).toContain('Bia');
    expect(text).toContain('Participante');
    expect(text).toContain('cai@exemplo.com');
    expect(text).toContain('2026-08-01');
    expect(text).toContain('Reenviar');
    expect(text).toContain('Cancelar convite');
    expect(text).toContain('Remover');
  });

  it('FE-SHARE-02/03 - valida e normaliza e-mail e bloqueia reenvio', () => {
    const response = new Subject<any>();
    service.convidar.mockReturnValue(response);
    const fixture = TestBed.createComponent(CompartilharLista);
    fixture.detectChanges();
    fixture.componentInstance.invite();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Informe um e-mail válido.');
    fixture.componentInstance.email.setValue('  NOVO@EXEMPLO.COM ');
    fixture.componentInstance.invite();
    fixture.componentInstance.invite();
    expect(service.convidar).toHaveBeenCalledOnce();
    expect(service.convidar).toHaveBeenCalledWith('l1', 'novo@exemplo.com');
    response.next({ outcome: 'INVITATION_CREATED', invitation: { id: 'i2', email: 'novo@exemplo.com' } });
    response.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Convite enviado com sucesso.');
  });

  it('FE-SHARE-02 - apresenta mensagens específicas retornadas pelo serviço', () => {
    service.convidar.mockReturnValue(throwError(() => ({ error: { code: 'ALREADY_MEMBER' } })));
    const fixture = TestBed.createComponent(CompartilharLista);
    fixture.detectChanges();
    fixture.componentInstance.email.setValue('bia@exemplo.com');
    fixture.componentInstance.invite();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Esta pessoa já participa da lista.');
  });

  it('FE-SHARE-04/08 - reenvia, cancela convite e remove participante', () => {
    const fixture = TestBed.createComponent(CompartilharLista);
    fixture.detectChanges();
    fixture.componentInstance.resend(ACCESS.invitations[0]);
    expect(service.reenviar).toHaveBeenCalledWith('l1', 'i1', 1);
    fixture.componentInstance.cancelInvitation(ACCESS.invitations[0]);
    expect(service.cancelarConvite).toHaveBeenCalledWith('l1', 'i1', 1);
    fixture.componentInstance.requestRemove(ACCESS.members[0]);
    fixture.componentInstance.confirmRemove();
    expect(service.removerMembro).toHaveBeenCalledWith('l1', 'u2', 2);
  });

  it('FE-SHARE-14 - diálogo acessível fecha com Escape sem remover', () => {
    const fixture = TestBed.createComponent(CompartilharLista);
    fixture.detectChanges();
    fixture.componentInstance.requestRemove(ACCESS.members[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeTruthy();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    expect(service.removerMembro).not.toHaveBeenCalled();
  });
});
