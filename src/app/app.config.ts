import { ApplicationConfig, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { cadastroMockInterceptor } from './auth/cadastro/cadastro-mock.interceptor';
import { loginMockInterceptor } from './auth/login/login-mock.interceptor';
import { recuperacaoSenhaMockInterceptor } from './auth/recuperacao-senha/recuperacao-senha-mock.interceptor';
import { sessaoMockInterceptor } from './auth/mocks/sessao-mock.interceptor';

const mockInterceptors = [
  cadastroMockInterceptor,
  loginMockInterceptor,
  recuperacaoSenhaMockInterceptor,
  sessaoMockInterceptor,
];
const httpProvider = isDevMode()
  ? provideHttpClient(withInterceptors(mockInterceptors))
  : provideHttpClient();

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    httpProvider,
  ]
};
