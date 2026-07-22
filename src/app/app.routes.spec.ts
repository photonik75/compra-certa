import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { Cadastro } from './auth/cadastro/cadastro';
import { CadastroService } from './auth/cadastro/cadastro.service';
import { Login } from './auth/login/login';
import { RecuperacaoSenha } from './auth/recuperacao-senha/recuperacao-senha';
import { RecuperacaoSenhaService } from './auth/recuperacao-senha/recuperacao-senha.service';
import { SessaoService } from './auth/sessao.service';
import { MinhasListas } from './listas/minhas-listas';
import { routes } from './app.routes';

describe('Testes das rotas da aplicação', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: CadastroService, useValue: {} },
        { provide: RecuperacaoSenhaService, useValue: {} },
        { provide: SessaoService, useValue: { consultar: () => of({}), sair: () => of(undefined) } },
      ],
    });
  });

  it('ROT-1 - Exibe a tela de login ao acessar /entrar.', async () => {
    const harness = await RouterTestingHarness.create();
    expect(await harness.navigateByUrl('/entrar', Login)).toBeInstanceOf(Login);
  });

  it('ROT-2 - Exibe a tela de cadastro ao acessar /cadastro.', async () => {
    const harness = await RouterTestingHarness.create();
    expect(await harness.navigateByUrl('/cadastro', Cadastro)).toBeInstanceOf(Cadastro);
  });

  it('ROT-3 - Exibe a tela de listas ao acessar /listas.', async () => {
    const harness = await RouterTestingHarness.create();
    expect(await harness.navigateByUrl('/listas', MinhasListas)).toBeInstanceOf(MinhasListas);
  });

  it('ROT-4 - Exibe a tela de recuperação de senha ao acessar /recuperar-senha.', async () => {
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/recuperar-senha', RecuperacaoSenha);
    expect(componente).toBeInstanceOf(RecuperacaoSenha);
  });
});
