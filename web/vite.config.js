import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.js'],
    env: {
      VITE_API_BASE_URL: 'https://api.test',
      VITE_API_TOKEN: 'test-token',
    },
  },
});
