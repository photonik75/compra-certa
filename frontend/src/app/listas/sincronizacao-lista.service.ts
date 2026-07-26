import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SincronizacaoListaService {
  readonly connection$ = new BehaviorSubject(true);
  readonly events$ = new Subject<any>();
  connect(_listId: string): void {}
  disconnect(): void {}
}
