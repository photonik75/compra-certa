import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { ListCard, ListCollection, ListStatus, ListasService } from './listas.service';

const EMPTY: ListCollection = {
  items: [], page: { nextCursor: null, hasMore: false }, summary: { activeLists: 0, pendingItems: 0 },
};

@Component({
  selector: 'app-minhas-listas',
  imports: [RouterLink],
  templateUrl: './minhas-listas.html',
  styleUrl: './minhas-listas.css',
})
export class MinhasListas implements OnInit {
  private readonly service = inject(ListasService);
  private readonly router = inject(Router);
  private readonly session = inject(SessaoService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  collection = EMPTY;
  filter: ListStatus = 'ACTIVE';
  search = '';
  signingOut = false;

  ngOnInit(): void { this.load(); }

  onSearch(event: Event): void {
    this.search = this.normalize((event.target as HTMLInputElement).value).slice(0, 60);
    this.load();
  }

  limparPesquisa(): void { this.search = ''; this.load(); }
  selecionarFiltro(filter: ListStatus): void { this.filter = filter; this.load(); }
  carregarMais(): void { if (this.collection.page.nextCursor) this.load(this.collection.page.nextCursor, true); }
  abrir(item: ListCard): void {
    const queryParams = item.status === 'COMPLETED' ? { modo: 'consulta' } : {};
    this.router.navigate(['/listas', item.id], { queryParams });
  }

  sair(): void {
    if (this.signingOut) return;
    this.signingOut = true;
    this.session.sair().pipe(finalize(() => this.signingOut = false)).subscribe({
      next: () => this.router.navigateByUrl('/entrar'),
    });
  }

  private load(cursor?: string, append = false): void {
    this.service.listar({ status: this.filter, search: this.search, cursor }).subscribe({
      next: (result) => {
        if (!append) {
          this.collection = result;
          this.changeDetector.markForCheck();
          return;
        }
        const known = new Set(this.collection.items.map((item) => item.id));
        this.collection = { ...result, items: [...this.collection.items, ...result.items.filter((i) => !known.has(i.id))] };
        this.changeDetector.markForCheck();
      },
      error: () => undefined,
    });
  }

  private normalize(value: string): string { return value.trim().replace(/\s+/g, ' '); }
}
