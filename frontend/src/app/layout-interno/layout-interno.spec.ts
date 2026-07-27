import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { SessaoService } from '../auth/sessao.service';
import { LayoutInterno } from './layout-interno';

const SESSION = {
  user: {
    id: 'u1',
    name: 'Larissa Barros',
    email: 'larissa@example.com',
    status: 'ACTIVE',
    createdAt: '2026-07-27T12:00:00Z',
  },
  csrfToken: 'csrf',
  expiresAt: '2026-07-28T00:00:00Z',
};

describe('LayoutInterno - EF09', () => {
  let fixture: ComponentFixture<LayoutInterno>;
  let session: { consultar: ReturnType<typeof vi.fn>; sair: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    session = {
      consultar: vi.fn().mockReturnValue(of(SESSION)),
      sair: vi.fn().mockReturnValue(of(undefined)),
    };
    await TestBed.configureTestingModule({
      imports: [LayoutInterno],
      providers: [
        provideRouter([]),
        { provide: SessaoService, useValue: session },
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(LayoutInterno);
    fixture.detectChanges();
  });

  it('FE-NAV-01/02 - exibe marca e destinos compartilhados com rotas corretas', () => {
    const navigation = fixture.nativeElement.querySelector('nav[aria-label="Navegação principal"]');
    expect(navigation.textContent).toContain('Minhas listas');
    expect(navigation.textContent).toContain('Categorias');
    expect(navigation.textContent).toContain('Produtos');
    expect(fixture.nativeElement.querySelector('a[aria-label="CompraCerta — abrir Minhas listas"]')
      .getAttribute('href')).toBe('/listas');
    expect(navigation.querySelector('a[href="/listas"]')).toBeTruthy();
    expect(navigation.querySelector('a[href="/categorias"]')).toBeTruthy();
    expect(navigation.querySelector('a[href="/produtos"]')).toBeTruthy();
  });

  it('FE-NAV-04 - apresenta nome e iniciais sem atribuir papel de lista à conta', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Larissa Barros');
    expect(text).toContain('LB');
    expect(text).toContain('Conta ativa');
    expect(text).not.toContain('Proprietária');
  });

  it('FE-NAV-05 - bloqueia duplo logout e abre o login no sucesso', () => {
    const pending = new Subject<void>();
    session.sair.mockReturnValue(pending);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const button = fixture.nativeElement.querySelector('button[data-action="logout"]') as HTMLButtonElement;
    button.click();
    button.click();
    fixture.detectChanges();
    expect(session.sair).toHaveBeenCalledOnce();
    expect(button.disabled).toBe(true);
    pending.next();
    pending.complete();
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  it('FE-NAV-05 - preserva a sessão e apresenta mensagem polida quando o logout falha', () => {
    session.sair.mockReturnValue(throwError(() => new Error('offline')));
    const button = fixture.nativeElement.querySelector('button[data-action="logout"]') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Não foi possível sair da sua conta. Verifique sua conexão e tente novamente.',
    );
    expect(button.disabled).toBe(false);
  });

  it('FE-NAV-06/07 - abre e fecha o drawer, bloqueia o fundo e restaura o foco', async () => {
    const toggle = fixture.nativeElement.querySelector('button[data-action="toggle-menu"]') as HTMLButtonElement;
    toggle.focus();
    toggle.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(toggle.getAttribute('aria-label')).toBe('Fechar menu');
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('.menu-lateral').getAttribute('data-open')).toBe('true');
    expect(fixture.nativeElement.querySelector('.conteudo-interno').getAttribute('inert')).not.toBeNull();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-label')).toBe('Abrir menu');
    expect(document.activeElement).toBe(toggle);
  });

  it('FE-NAV-06 - fecha o drawer ao acionar a camada externa', () => {
    const toggle = fixture.nativeElement.querySelector('button[data-action="toggle-menu"]') as HTMLButtonElement;
    toggle.click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.menu-overlay') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
  });

  it('FE-NAV-09 - consulta a sessão por serviço e protege o conteúdo quando ela é inválida', () => {
    expect(session.consultar).toHaveBeenCalledOnce();
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    session.consultar.mockReturnValue(throwError(() => new Error('invalid')));
    const invalidFixture = TestBed.createComponent(LayoutInterno);
    invalidFixture.detectChanges();
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  it('FE-NAV-11 - oferece semântica e nomes acessíveis para menu e controles', () => {
    expect(fixture.nativeElement.querySelector('aside[aria-label="Menu da área autenticada"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('nav[aria-label="Navegação principal"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('button[aria-label="Abrir menu"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('button[data-action="logout"]').textContent).toContain('Sair');
  });
});
