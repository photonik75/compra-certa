import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { Cadastro } from './auth/cadastro/cadastro';
import { CadastroService } from './auth/cadastro/cadastro.service';
import { Login } from './auth/login/login';
import { RecuperacaoSenha } from './auth/recuperacao-senha/recuperacao-senha';
import { RecuperacaoSenhaService } from './auth/recuperacao-senha/recuperacao-senha.service';
import { RedefinicaoSenha } from './auth/redefinicao-senha/redefinicao-senha';
import { RedefinicaoSenhaService } from './auth/redefinicao-senha/redefinicao-senha.service';
import { SessaoService } from './auth/sessao.service';
import { AceitarConvite } from './compartilhamento/aceitar-convite';
import { CompartilharLista } from './compartilhamento/compartilhar-lista';
import { Categorias } from './categorias/categorias';
import { DetalheLista } from './listas/detalhe-lista/detalhe-lista';
import { EditarItem } from './listas/editar-item/editar-item';
import { EditarLista } from './listas/editar-lista/editar-lista';
import { MinhasListas } from './listas/minhas-listas';
import { NovaLista } from './listas/nova-lista/nova-lista';
import { NovoItem } from './listas/novo-item/novo-item';
import { SincronizacaoListaService } from './listas/sincronizacao-lista.service';
import { LayoutInterno } from './layout-interno/layout-interno';
import { Produtos } from './produtos/produtos';
import { routes } from './app.routes';

describe('Testes das rotas da aplicação', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: CadastroService, useValue: {} },
        { provide: RecuperacaoSenhaService, useValue: {} },
        { provide: RedefinicaoSenhaService, useValue: {} },
        {
          provide: SincronizacaoListaService,
          useValue: {
            connection$: new BehaviorSubject(true),
            events$: new Subject(),
            connect: () => undefined,
            disconnect: () => undefined,
          },
        },
        {
          provide: SessaoService,
          useValue: { consultar: () => throwError(() => new Error()), sair: () => of(undefined) },
        },
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
    TestBed.overrideProvider(SessaoService, {
      useValue: { consultar: () => of({}), sair: () => of(undefined) },
    });
    const harness = await RouterTestingHarness.create();
    await esperarTelaInterna(harness, '/listas', 'Minhas listas');
  });

  it('ROT-4 - Exibe a tela de recuperação de senha ao acessar /recuperar-senha.', async () => {
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/recuperar-senha', RecuperacaoSenha);
    expect(componente).toBeInstanceOf(RecuperacaoSenha);
  });

  it('ROT-5 - Exibe a tela de redefinição de senha ao acessar /redefinir-senha.', async () => {
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/redefinir-senha#token=token-recuperacao', RedefinicaoSenha);
    expect(componente).toBeInstanceOf(RedefinicaoSenha);
  });

  it('FE-LIS-17/FE-ITEM-17/FE-SHOP-14/FE-LIFE-14 - Exibe as telas das rotas de listas e itens.', async () => {
    const harness = await criarHarnessAutenticado();
    await esperarTelaInterna(harness, '/listas/nova', 'Nova lista');
    await esperarTelaInterna(harness, '/listas/1', 'Detalhe da lista');
    await esperarTelaInterna(harness, '/listas/1/editar', 'Editar lista');
    await esperarTelaInterna(harness, '/listas/1/itens/novo', 'Adicionar item');
    await esperarTelaInterna(harness, '/listas/1/itens/2/editar', 'Editar item');
  });

  it('FE-CAT-16/FE-PROD-17 - Exibe as telas das rotas de catálogo.', async () => {
    const harness = await criarHarnessAutenticado();
    await esperarTelaInterna(harness, '/categorias', 'Categorias');
    await esperarTelaInterna(harness, '/produtos', 'Produtos');
  });

  it('FE-SHARE-16 - Exibe compartilhamento e aceite de convite.', async () => {
    const harness = await criarHarnessAutenticado();
    await esperarTelaInterna(harness, '/listas/1/compartilhar', 'Compartilhar lista');
    await esperarTelaInterna(harness, '/convites/aceitar#token=convite', 'Convite para lista');
  });

  async function criarHarnessAutenticado(): Promise<RouterTestingHarness> {
    TestBed.overrideProvider(SessaoService, {
      useValue: { consultar: () => of({}), sair: () => of(undefined) },
    });
    return RouterTestingHarness.create();
  }

  async function esperarTelaInterna(
    harness: RouterTestingHarness,
    url: string,
    titulo: string,
  ): Promise<void> {
    expect(await harness.navigateByUrl(url, LayoutInterno)).toBeInstanceOf(LayoutInterno);
    expect(harness.routeNativeElement?.querySelector('.conteudo-interno h1')?.textContent).toContain(titulo);
  }
});
