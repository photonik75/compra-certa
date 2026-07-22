import { Routes } from '@angular/router';
import { Cadastro } from './auth/cadastro/cadastro';
import { MinhasListas } from './listas/minhas-listas';
import { Login } from './auth/login/login';
import { sessaoGuard } from './auth/sessao.guard';
import { RecuperacaoSenha } from './auth/recuperacao-senha/recuperacao-senha';
import { RedefinicaoSenha } from './auth/redefinicao-senha/redefinicao-senha';
import { visitanteGuard } from './auth/visitante.guard';

export const routes: Routes = [
  { path: 'cadastro', component: Cadastro, canActivate: [visitanteGuard] },
  { path: 'entrar', component: Login, canActivate: [visitanteGuard] },
  { path: 'recuperar-senha', component: RecuperacaoSenha },
  { path: 'redefinir-senha', component: RedefinicaoSenha },
  { path: 'listas', component: MinhasListas, canActivate: [sessaoGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'entrar' },
];
