import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CompartilhamentoService } from './compartilhamento.service';

describe('CompartilhamentoService - FE-SHARE-12', () => {
  let service: CompartilhamentoService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CompartilhamentoService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CompartilhamentoService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('cobre consulta, convite, reenvio e aceite', () => {
    service.consultarAcesso('l1').subscribe();
    http.expectOne('/api/v1/lists/l1/access').flush({});
    service.convidar('l1', 'a@b.com').subscribe();
    const invite = http.expectOne('/api/v1/lists/l1/invitations');
    expect(invite.request.headers.has('Idempotency-Key')).toBe(true);
    invite.flush({});
    service.reenviar('l1', 'i1', 2).subscribe();
    const resend = http.expectOne('/api/v1/lists/l1/invitations/i1/resend');
    expect(resend.request.headers.get('If-Match')).toBe('"2"');
    resend.flush({});
    service.aceitar('token').subscribe();
    const accept = http.expectOne('/api/v1/invitations/accept');
    expect(accept.request.body).toEqual({ token: 'token' });
    accept.flush({});
  });

  it('cobre preview, cancelamento, remoção e saída', () => {
    service.preview('token').subscribe();
    http.expectOne('/api/v1/invitations/preview').flush({});
    service.cancelarConvite('l1', 'i1', 1).subscribe();
    http.expectOne('/api/v1/lists/l1/invitations/i1').flush(null);
    service.removerMembro('l1', 'u1', 2).subscribe();
    http.expectOne('/api/v1/lists/l1/members/u1').flush(null);
    service.sair('l1', 3).subscribe();
    http.expectOne('/api/v1/lists/l1/members/me').flush(null);
  });
});
