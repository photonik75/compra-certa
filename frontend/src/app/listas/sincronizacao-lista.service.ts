import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SincronizacaoListaService {
  readonly connection$ = new BehaviorSubject(true);
  readonly events$ = new Subject<any>();
  private source?: EventSource;
  private listId?: string;
  private readonly online = () => {
    if (this.listId) this.open(this.listId);
  };
  private readonly connected = () => this.connection$.next(true);
  private readonly offline = () => this.connection$.next(false);

  connect(listId: string): void {
    this.disconnect();
    this.listId = listId;
    window.addEventListener('online', this.online);
    window.addEventListener('offline', this.offline);
    this.connection$.next(navigator.onLine);
    this.open(listId);
  }

  private open(listId: string): void {
    this.source?.close();
    this.source = new EventSource(`/api/v1/lists/${listId}/events`);
    this.source.addEventListener('connected', this.connected);
    for (const type of [
      'list.item.created',
      'list.item.updated',
      'list.item.deleted',
      'list.item.checked',
      'list.status.changed',
      'list.deleted',
      'list.access.changed',
    ]) {
      this.source.addEventListener(type, (message) => {
        this.connection$.next(true);
        this.events$.next(JSON.parse((message as MessageEvent).data));
      });
    }
    this.source.onerror = () => this.connection$.next(false);
  }

  disconnect(): void {
    this.source?.close();
    this.source = undefined;
    this.listId = undefined;
    window.removeEventListener('online', this.online);
    window.removeEventListener('offline', this.offline);
  }
}
