import { Routes } from '@angular/router';
import { Cadastro } from './auth/cadastro/cadastro';
import { MinhasListas } from './listas/minhas-listas';

export const routes: Routes = [
  { path: 'cadastro', component: Cadastro },
  { path: 'listas', component: MinhasListas },
  { path: '', pathMatch: 'full', redirectTo: 'cadastro' },
];
