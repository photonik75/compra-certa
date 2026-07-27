import { ChangeDetectorRef, Component, EventEmitter, HostListener, Input, Output, inject } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CicloVidaListaService } from '../ciclo-vida-lista.service';

type Action = 'complete' | 'reopen' | 'delete' | null;
interface LifecycleList {
  id: string; name: string; status: string; role: string; version: number;
  summary: { pending: number };
}

@Component({
  selector: 'app-concluir-lista',
  templateUrl: './concluir-lista.html',
  styleUrl: './concluir-lista.css',
})
export class ConcluirLista {
  private readonly service = inject(CicloVidaListaService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  @Input({ required: true }) lista!: LifecycleList;
  @Output() readonly listaChange = new EventEmitter<LifecycleList>();
  action: Action = null;
  sending = false;
  conflict = false;
  notice = '';
  private trigger?: HTMLElement;

  open(action: Action, source?: Event | HTMLElement): void {
    this.action = action;
    this.trigger = source instanceof Event ? source.currentTarget as HTMLElement : source;
    this.conflict = false;
    this.notice = '';
    this.changeDetector.markForCheck();
  }

  confirm(): void {
    if (!this.action || this.sending) return;
    this.sending = true;
    if (this.action === 'delete') {
      this.service.excluir(this.lista.id, this.lista.version).pipe(finalize(() => this.sending = false)).subscribe({
        next: () => { this.router.navigate(['/listas']); this.notice = 'Lista excluída com sucesso.'; this.close(); },
        error: (error) => this.handleError(error),
      });
      return;
    }
    const status = this.action === 'complete' ? 'COMPLETED' : 'ACTIVE';
    this.service.alterarStatus(this.lista.id, status, this.lista.version)
      .pipe(finalize(() => this.sending = false)).subscribe({
        next: (list) => {
          this.lista = list;
          this.listaChange.emit(list);
          this.notice = status === 'COMPLETED' ? 'Lista concluída com sucesso.' : 'Lista reaberta com sucesso.';
          this.close();
        },
        error: (error) => this.handleError(error),
      });
  }

  close(): void {
    this.action = null;
    this.changeDetector.markForCheck();
    queueMicrotask(() => this.trigger?.focus());
  }

  reload(): void { this.notice = 'Recarregue a lista para obter os dados atuais.'; }

  @HostListener('document:keydown.escape')
  onEscape(): void { if (this.action) this.close(); }

  private handleError(response: any): void {
    this.conflict = response?.error?.code === 'CONFLICT';
    this.notice = this.conflict
      ? 'A lista foi alterada em outro lugar. Recarregue os dados para continuar.'
      : 'Não foi possível alterar a lista. Tente novamente em alguns instantes.';
    this.changeDetector.markForCheck();
  }
}
