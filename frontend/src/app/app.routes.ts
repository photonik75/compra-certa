import { Routes } from '@angular/router';
import { Cadastro } from './auth/cadastro/cadastro';
import { AceitarConvite } from './compartilhamento/aceitar-convite';
import { CompartilharLista } from './compartilhamento/compartilhar-lista';
import { Categorias } from './categorias/categorias';
import { DetalheLista } from './listas/detalhe-lista/detalhe-lista';
import { EditarItem } from './listas/editar-item/editar-item';
import { EditarLista } from './listas/editar-lista/editar-lista';
import { MinhasListas } from './listas/minhas-listas';
import { NovaLista } from './listas/nova-lista/nova-lista';
import { NovoItem } from './listas/novo-item/novo-item';
import { Login } from './auth/login/login';
import { Produtos } from './produtos/produtos';
import { sessaoGuard } from './auth/sessao.guard';
import { RecuperacaoSenha } from './auth/recuperacao-senha/recuperacao-senha';
import { RedefinicaoSenha } from './auth/redefinicao-senha/redefinicao-senha';
import { visitanteGuard } from './auth/visitante.guard';

export const routes: Routes = [
  { path: 'cadastro', component: Cadastro, canActivate: [visitanteGuard] },
  { path: 'entrar', component: Login, canActivate: [visitanteGuard] },
  { path: 'recuperar-senha', component: RecuperacaoSenha },
  { path: 'redefinir-senha', component: RedefinicaoSenha },
  {
    path: 'listas',
    canActivate: [sessaoGuard],
    children: [
      { path: '', component: MinhasListas },
      { path: 'nova', component: NovaLista },
      { path: ':listId/editar', component: EditarLista },
      { path: ':listId/itens/novo', component: NovoItem },
      { path: ':listId/itens/:itemId/editar', component: EditarItem },
      { path: ':listId/compartilhar', component: CompartilharLista },
      { path: ':listId', component: DetalheLista },
    ],
  },
  { path: 'categorias', component: Categorias, canActivate: [sessaoGuard] },
  { path: 'produtos', component: Produtos, canActivate: [sessaoGuard] },
  { path: 'convites/aceitar', component: AceitarConvite },
  { path: '', pathMatch: 'full', redirectTo: 'entrar' },
];
