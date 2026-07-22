import { SessionResponse } from '../models/session-response';
import { UserSummary } from '../models/user-summary';

const DURACAO_SESSAO_MS = 86_400_000;
const MOCK_CSRF_TOKEN = 'mock-csrf-token';

export function criarSessaoMock(dados: Pick<UserSummary, 'name' | 'email'>): SessionResponse {
  return {
    user: {
      id: crypto.randomUUID(),
      name: dados.name,
      email: dados.email,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    },
    csrfToken: MOCK_CSRF_TOKEN,
    expiresAt: new Date(Date.now() + DURACAO_SESSAO_MS).toISOString(),
  };
}
