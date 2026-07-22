import { UserSummary } from './user-summary';

export interface SessionResponse {
  user: UserSummary;
  csrfToken: string;
  expiresAt: string;
}
