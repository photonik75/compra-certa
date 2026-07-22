import { Routes } from '@angular/router';
import { Cadastro } from './auth/cadastro/cadastro';
import { MinhasListas } from './listas/minhas-listas';
import { Login } from './auth/login/login';
import { sessaoGuard } from './auth/sessao.guard';
import { RecuperacaoSenha } from './auth/recuperacao-senha/recuperacao-senha';

export const routes: Routes = [
  { path: 'cadastro', component: Cadastro },
  { path: 'entrar', component: Login },
  { path: 'recuperar-senha', component: RecuperacaoSenha },
  { path: 'listas', component: MinhasListas, canActivate: [sessaoGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'entrar' },
];
