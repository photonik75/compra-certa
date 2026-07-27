import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CompartilhamentoService } from './compartilhamento.service';
import { AceitarConvite } from './aceitar-convite';

describe('AceitarConvite - EF08', () => {
  let service: any;
  let router: any;
  beforeEach(async () => {
    service = {
      preview: vi.fn().mockReturnValue(of({
        listName: 'Feira', ownerName: 'Ana', invitedEmail: 'bia@exemplo.com', status: 'PENDING',
      })),
      aceitar: vi.fn().mockReturnValue(of({ listId: 'l1' })),
    };
    router = { navigate: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [AceitarConvite],
      providers: [
        { provide: CompartilhamentoService, useValue: service },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: new Map(), fragment: 'token-secreto' } },
        },
      ],
    }).compileComponents();
  });

  it('FE-SHARE-05/13 - usa token do fragmento e mostra somente dados permitidos', () => {
    const fixture = TestBed.createComponent(AceitarConvite);
    fixture.detectChanges();
    expect(service.preview).toHaveBeenCalledWith('token-secreto');
    expect(fixture.nativeElement.textContent).toContain('Feira');
    expect(fixture.nativeElement.textContent).toContain('Ana');
    expect(fixture.nativeElement.textContent).toContain('bia@exemplo.com');
  });

  it('FE-SHARE-07 - aceita uma única vez e abre a lista', () => {
    const pending = new Subject<any>();
    service.aceitar.mockReturnValue(pending);
    const fixture = TestBed.createComponent(AceitarConvite);
    fixture.detectChanges();
    fixture.componentInstance.accept();
    fixture.componentInstance.accept();
    expect(service.aceitar).toHaveBeenCalledOnce();
    pending.next({ listId: 'l1' });
    pending.complete();
    expect(router.navigate).toHaveBeenCalledWith(['/listas', 'l1']);
  });

  it('FE-SHARE-05/07 - trata convite expirado ou e-mail divergente sem consumir', () => {
    service.preview.mockReturnValue(throwError(() => ({ error: { code: 'INVITATION_EXPIRED' } })));
    const fixture = TestBed.createComponent(AceitarConvite);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Este convite expirou.');
    expect(service.aceitar).not.toHaveBeenCalled();
  });
});
