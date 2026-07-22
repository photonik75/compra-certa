import { Injectable } from '@angular/core';
import { SessionResponse } from '../models/session-response';

@Injectable({ providedIn: 'root' })
export class SessaoMockStore {
  private sessao: SessionResponse | null = null;

  salvar(sessao: SessionResponse): void {
    this.sessao = sessao;
  }

  obter(): SessionResponse | null {
    return this.sessao;
  }

  limpar(): void {
    this.sessao = null;
  }
}
