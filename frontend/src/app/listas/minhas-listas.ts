import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';

const ROTA_ENTRAR = '/entrar';

@Component({
  selector: 'app-minhas-listas',
  templateUrl: './minhas-listas.html',
  styleUrl: './minhas-listas.css',
})
export class MinhasListas {
  private readonly sessaoService = inject(SessaoService);
  private readonly router = inject(Router);
  protected readonly encerrandoSessao = signal(false);

  protected sair(): void {
    if (this.encerrandoSessao()) return;
    this.encerrandoSessao.set(true);
    this.sessaoService.sair().pipe(finalize(() => this.encerrandoSessao.set(false))).subscribe({
      next: () => this.router.navigateByUrl(ROTA_ENTRAR),
    });
  }
}
