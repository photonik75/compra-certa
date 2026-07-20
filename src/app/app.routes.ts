import { Routes } from '@angular/router';
import { Cadastro } from './auth/cadastro/cadastro';

export const routes: Routes = [
  { path: 'cadastro', component: Cadastro },
  { path: '', pathMatch: 'full', redirectTo: 'cadastro' },
];