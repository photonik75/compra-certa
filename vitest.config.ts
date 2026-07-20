import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['test/src/**/*.spec.ts'],
    setupFiles: ['test/setup.ts'],
  },
});
