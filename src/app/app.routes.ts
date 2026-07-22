import { Routes } from '@angular/router';
import { Cadastro } from './auth/cadastro/cadastro';
import { MinhasListas } from './listas/minhas-listas';
import { Login } from './auth/login/login';
import { sessaoGuard } from './auth/sessao.guard';

export const routes: Routes = [
  { path: 'cadastro', component: Cadastro },
  { path: 'entrar', component: Login },
  { path: 'listas', component: MinhasListas, canActivate: [sessaoGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'entrar' },
];
