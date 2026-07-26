import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { SessaoService } from '../auth/sessao.service';
import { ListasService } from './listas.service';
import { MinhasListas } from './minhas-listas';

const COLECAO = {
  items: [
    {
      id: '2', name: 'Feira', status: 'ACTIVE', role: 'OWNER', updatedAt: '2026-07-25T10:00:00Z',
      summary: { total: 10, checked: 4, pending: 6, percentage: 40 },
    },
    {
      id: '1', name: 'Viagem', status: 'COMPLETED', role: 'EDITOR', updatedAt: '2026-07-24T10:00:00Z',
      summary: { total: 5, checked: 5, pending: 0, percentage: 100 },
    },
  ],
  page: { nextCursor: null, hasMore: false },
  summary: { activeLists: 7, pendingItems: 18 },
};

describe('MinhasListas - EF02', () => {
  let fixture: ComponentFixture<MinhasListas>;
  let service: { listar: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { listar: vi.fn().mockReturnValue(of(COLECAO)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [MinhasListas],
      providers: [
        { provide: ListasService, useValue: service },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: {} },
        { provide: SessaoService, useValue: { sair: vi.fn().mockReturnValue(of(undefined)) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MinhasListas);
    fixture.detectChanges();
  });

  it('FE-LIS-01/03/06 - renderiza controles, resumo e cartões retornados pelo serviço', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Minhas listas');
    expect(text).toContain('7 listas ativas');
    expect(text).toContain('18 itens pendentes');
    expect(text).toContain('Pesquisar listas');
    expect(text).toContain('Nova lista');
    expect(text).toContain('Feira');
    expect(text).toContain('Proprietário');
    expect(text).toContain('10 itens');
    expect(text).toContain('6 pendentes');
    expect(text).toContain('40%');
  });

  it('FE-LIS-02 - acrescenta página sem duplicar e preserva a ordem do serviço', () => {
    service.listar.mockReset();
    service.listar
      .mockReturnValueOnce(of({ ...COLECAO, items: [COLECAO.items[0]], page: { nextCursor: 'c2', hasMore: true } }))
      .mockReturnValueOnce(of({ ...COLECAO, items: [COLECAO.items[0], COLECAO.items[1]] }));
    const localFixture = TestBed.createComponent(MinhasListas);
    localFixture.detectChanges();
    localFixture.componentInstance.carregarMais();
    localFixture.detectChanges();
    const cards = [...localFixture.nativeElement.querySelectorAll('[data-testid="list-card"]')]
      .map((element: Element) => element.textContent);
    expect(cards).toHaveLength(2);
    expect(cards[0]).toContain('Feira');
    expect(cards[1]).toContain('Viagem');
  });

  it('FE-LIS-04 - normaliza e limita pesquisa, combinando-a ao filtro', () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = `  feira   do   bairro ${'x'.repeat(80)} `;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(service.listar).toHaveBeenLastCalledWith({
      status: 'ACTIVE',
      search: 'feira do bairro ' + 'x'.repeat(44),
      cursor: undefined,
    });
    fixture.componentInstance.limparPesquisa();
    expect(service.listar).toHaveBeenLastCalledWith({ status: 'ACTIVE', search: '', cursor: undefined });
  });

  it('FE-LIS-05 - inicia em Ativas e alterna filtros com estados vazios específicos', () => {
    expect(fixture.nativeElement.querySelector('[aria-pressed="true"]').textContent).toContain('Ativas');
    service.listar.mockReturnValue(of({ ...COLECAO, items: [] }));
    fixture.componentInstance.selecionarFiltro('COMPLETED');
    fixture.detectChanges();
    expect(service.listar).toHaveBeenLastCalledWith({ status: 'COMPLETED', search: '', cursor: undefined });
    expect(fixture.nativeElement.textContent).toContain('Você não possui listas concluídas.');
  });

  it('FE-LIS-06 - apresenta estado geral vazio com ação para criar', () => {
    service.listar.mockReturnValue(of({ ...COLECAO, items: [], summary: { activeLists: 0, pendingItems: 0 } }));
    const localFixture = TestBed.createComponent(MinhasListas);
    localFixture.detectChanges();
    expect(localFixture.nativeElement.textContent).toContain('Você ainda não possui listas.');
    expect(localFixture.nativeElement.textContent).toContain('Criar lista');
  });

  it('FE-LIS-07 - abre no modo correto e só oferece edição ao proprietário de ativa', () => {
    const editLinks = fixture.nativeElement.querySelectorAll('[data-action="edit"]');
    expect(editLinks).toHaveLength(1);
    fixture.componentInstance.abrir(COLECAO.items[1]);
    expect(router.navigate).toHaveBeenCalledWith(['/listas', '1'], { queryParams: { modo: 'consulta' } });
  });

  it('FE-LIS-14/15 - usa serviço injetado e oferece controles e mensagens acessíveis', () => {
    expect(service.listar).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('input[aria-label="Pesquisar listas"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[aria-live="polite"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('button, a').length).toBeGreaterThan(3);
  });
});
