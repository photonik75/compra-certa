import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { defer, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { routes } from '../app.routes';
import { CadastroService } from './cadastro/cadastro.service';
import { LoginService } from './login/login.service';
import { SessaoService } from './sessao.service';

const EMAIL = 'maria@example.com';
const SENHA = 'senha123';
const ROTA_CADASTRO = '/cadastro';
const ROTA_ENTRAR = '/entrar';
const ROTA_ENTRAR_COM_RETORNO = '/entrar?returnUrl=%2Flistas';
const ROTA_LISTAS = '/listas';
const SESSION_RESPONSE = {
  user: {
    id: '4f32ccf4-e676-4c23-bd66-e0fb2c2f0ef9',
    name: 'Maria',
    email: EMAIL,
    status: 'ACTIVE',
    createdAt: '2026-07-22T12:00:00Z',
  },
  csrfToken: 'csrf-token',
  expiresAt: '2026-07-23T00:00:00Z',
};

function configurarTeste(sessaoAtiva: boolean) {
  const estado = { ativa: sessaoAtiva };
  const sessaoService = {
    consultar: vi.fn(() => estado.ativa ? of(SESSION_RESPONSE) : throwError(() => new Error())),
    sair: vi.fn(() => defer(() => {
      estado.ativa = false;
      return of(undefined);
    })),
  };
  const loginService = {
    entrar: vi.fn(() => defer(() => {
      estado.ativa = true;
      return of(SESSION_RESPONSE);
    })),
  };
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes),
      { provide: CadastroService, useValue: {} },
      { provide: LoginService, useValue: loginService },
      { provide: SessaoService, useValue: sessaoService },
    ],
  });
  return { estado, loginService, sessaoService };
}

function preencher(campo: HTMLInputElement, valor: string): void {
  campo.value = valor;
  campo.dispatchEvent(new Event('input', { bubbles: true }));
}

describe('Testes de navegação da autenticação', () => {
  it('NAV-1 - Visitante retorna à página interna solicitada após se autenticar.', async () => {
    configurarTeste(false);
    const harness = await RouterTestingHarness.create();
    const router = TestBed.inject(Router);
    await harness.navigateByUrl(ROTA_LISTAS);
    expect(router.url).toBe(ROTA_ENTRAR_COM_RETORNO);
    const pagina = harness.routeNativeElement as HTMLElement;
    preencher(pagina.querySelector<HTMLInputElement>('#email')!, EMAIL);
    preencher(pagina.querySelector<HTMLInputElement>('#senha')!, SENHA);
    await harness.fixture.whenStable();
    pagina.querySelector<HTMLButtonElement>('button.entrar')!.click();
    await harness.fixture.whenStable();
    expect(router.url).toBe(ROTA_LISTAS);
  });

  it.each([ROTA_ENTRAR, ROTA_CADASTRO])(
    'NAV-2 - Usuário autenticado é direcionado para Minhas Listas ao abrir %s.',
    async (rota) => {
      configurarTeste(true);
      const harness = await RouterTestingHarness.create();
      await harness.navigateByUrl(rota);
      expect(TestBed.inject(Router).url).toBe(ROTA_LISTAS);
    },
  );

  it('NAV-3 - Após sair, páginas internas voltam a exigir autenticação.', async () => {
    const { sessaoService } = configurarTeste(true);
    const harness = await RouterTestingHarness.create();
    const router = TestBed.inject(Router);
    await harness.navigateByUrl(ROTA_LISTAS);
    (harness.routeNativeElement as HTMLElement).querySelector<HTMLButtonElement>('button')!.click();
    await harness.fixture.whenStable();
    expect(sessaoService.sair).toHaveBeenCalledOnce();
    expect(router.url).toBe(ROTA_ENTRAR);
    await router.navigateByUrl(ROTA_LISTAS);
    expect(router.url).toBe(ROTA_ENTRAR_COM_RETORNO);
  });
});
