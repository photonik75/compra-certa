import { ApplicationConfig, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { cadastroMockInterceptor } from './auth/cadastro/cadastro-mock.interceptor';

const httpProvider = isDevMode() ? provideHttpClient(withInterceptors([cadastroMockInterceptor])) : provideHttpClient();

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    httpProvider,
  ]
};
