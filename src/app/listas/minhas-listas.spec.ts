import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { vi } from 'vitest';
import { SessaoService } from '../auth/sessao.service';
import { MinhasListas } from './minhas-listas';

const SAIR = 'Sair';
const MINHAS_LISTAS = 'Minhas Listas';
const ROTA_ENTRAR = '/entrar';

describe('MinhasListas', () => {
  let fixture: ComponentFixture<MinhasListas>;
  let respostaSaida: Subject<void>;
  let sessaoService: { sair: ReturnType<typeof vi.fn> };
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    respostaSaida = new Subject<void>();
    sessaoService = { sair: vi.fn().mockReturnValue(respostaSaida) };
    router = { navigateByUrl: vi.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [MinhasListas],
      providers: [
        { provide: SessaoService, useValue: sessaoService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MinhasListas);
    fixture.detectChanges();
  });

  it('LIS-1 - Apresenta o título “Minhas Listas”.', () => {
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe(MINHAS_LISTAS);
  });

  it('LIS-2 - Apresenta o estado vazio e mantém disponível a ação “Sair”.', () => {
    expect(fixture.nativeElement.querySelector('ul, ol, [role="list"], [role="listitem"]')).toBeNull();
    expect(obterBotaoSair().disabled).toBe(false);
  });

  it('LIS-3 - Bloqueia novos pedidos de saída enquanto encerra a sessão.', () => {
    const botao = obterBotaoSair();
    botao.click();
    fixture.detectChanges();
    expect(botao.disabled).toBe(true);
    botao.click();
    expect(sessaoService.sair).toHaveBeenCalledOnce();
    respostaSaida.complete();
    fixture.detectChanges();
    expect(botao.disabled).toBe(false);
  });

  it('LIS-4 - Abre o login após encerrar a sessão com sucesso.', () => {
    obterBotaoSair().click();
    respostaSaida.next();
    respostaSaida.complete();
    expect(router.navigateByUrl).toHaveBeenCalledWith(ROTA_ENTRAR);
  });

  function obterBotaoSair(): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('button')].find((botao) => botao.textContent === SAIR);
  }
});
