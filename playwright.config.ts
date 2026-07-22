import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './src/app/auth',
  testMatch: '**/*.integration.e2e.ts',
  fullyParallel: false,
  timeout: 10_000,
  use: {
    baseURL: 'http://127.0.0.1:4201',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm start -- --configuration production --host 127.0.0.1 --port 4201',
    url: 'http://127.0.0.1:4201',
    reuseExistingServer: false,
  },
});
