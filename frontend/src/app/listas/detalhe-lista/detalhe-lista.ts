import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { formatQuantity, unitLabel } from '../item-form';
import { ListItem, ListSummary, ListaItensService } from '../lista-itens.service';
import { SincronizacaoListaService } from '../sincronizacao-lista.service';
import { ListDetail, ListasService } from '../listas.service';
import { ConcluirLista } from '../concluir-lista/concluir-lista';

@Component({
  selector: 'app-detalhe-lista',
  imports: [RouterLink, ConcluirLista],
  templateUrl: './detalhe-lista.html',
  styleUrl: './detalhe-lista.css',
})
export class DetalheLista implements OnInit, OnDestroy {
  private readonly service = inject(ListaItensService);
  private readonly listsService = inject(ListasService);
  private readonly sync = inject(SincronizacaoListaService);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly subscriptions = new Subscription();
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  items: ListItem[] = [];
  summary: ListSummary = { total: 0, checked: 0, pending: 0, percentage: 0 };
  connected = true;
  processing = new Set<string>();
  selected?: ListItem;
  notice = '';
  list?: ListDetail;
  unavailable = false;
  private initializedConnection = false;

  ngOnInit(): void {
    this.loadList();
    this.load();
    this.sync.connect(this.listId);
    this.subscriptions.add(this.sync.connection$.subscribe((connected) => {
      const reconnecting = this.initializedConnection && !this.connected && connected;
      this.connected = connected;
      this.initializedConnection = true;
      if (reconnecting) this.load();
      this.changeDetector.markForCheck();
    }));
    this.subscriptions.add(this.sync.events$.subscribe((event) => {
      if (event.listId !== this.listId) return;
      this.load();
      this.loadList();
    }));
  }

  ngOnDestroy(): void { this.subscriptions.unsubscribe(); this.sync.disconnect(); }

  get groups() {
    const grouped = new Map<string, { category: ListItem['category']; items: ListItem[] }>();
    for (const item of this.items) {
      const key = item.category.name.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
      const group = grouped.get(key) ?? { category: item.category, items: [] };
      group.items.push(item);
      grouped.set(key, group);
    }
    return [...grouped.values()].sort((a, b) => a.category.name.localeCompare(b.category.name, 'pt-BR'));
  }

  toggle(item: ListItem): void {
    if (!this.connected || this.list?.status === 'COMPLETED' || this.processing.has(item.id)) return;
    this.processing.add(item.id);
    this.service.marcar(this.listId, item.id, !item.checked, item.version)
      .pipe(finalize(() => {
        this.processing.delete(item.id);
        this.changeDetector.markForCheck();
      })).subscribe({
        next: (result) => {
          this.apply(result.item, result.listSummary);
          this.notice = result.item.checked ? 'Item marcado com sucesso.' : 'Item desmarcado com sucesso.';
          this.changeDetector.markForCheck();
        },
        error: (response) => {
          if (response?.error?.code === 'CONFLICT') {
            this.notice = 'A lista foi atualizada em outro lugar.';
            this.apply(response.error.meta.item, response.error.meta.listSummary);
          } else {
            this.notice = 'Não foi possível atualizar o item. Verifique sua conexão e tente novamente.';
          }
          this.changeDetector.markForCheck();
        },
      });
  }

  requestRemove(item: ListItem): void { this.selected = item; this.changeDetector.markForCheck(); }
  cancelRemove(): void { this.selected = undefined; this.changeDetector.markForCheck(); }

  confirmRemove(): void {
    if (!this.selected) return;
    const item = this.selected;
    this.service.remover(this.listId, item.id, item.version).subscribe({
      next: (result) => {
        this.items = this.items.filter((current) => current.id !== result.deletedItemId);
        this.summary = result.listSummary;
        this.selected = undefined;
        this.notice = 'Item removido com sucesso.';
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.notice = 'Não foi possível remover o item. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  quantity(value: string): string { return formatQuantity(value); }
  unit(value: string): string { return unitLabel(value); }
  onLifecycle(list: any): void { this.list = { ...this.list, ...list }; this.load(); }

  private load(): void {
    this.service.listar(this.listId).subscribe({
      next: (result) => {
        this.items = result.items;
        this.summary = result.listSummary;
        this.changeDetector.markForCheck();
      },
      error: () => undefined,
    });
  }

  private loadList(): void {
    this.listsService.obter(this.listId).subscribe({
      next: (list) => {
        this.list = list;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.unavailable = true;
        this.notice = 'Lista não encontrada ou indisponível para sua conta.';
        this.changeDetector.markForCheck();
      },
    });
  }

  private apply(item: ListItem, summary?: ListSummary): void {
    const current = this.items.find((candidate) => candidate.id === item.id);
    if (current && item.version < current.version) return;
    this.items = this.items.map((candidate) => candidate.id === item.id ? item : candidate);
    if (summary) this.summary = summary;
    this.changeDetector.markForCheck();
  }
}
