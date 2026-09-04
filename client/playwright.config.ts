import { defineConfig, devices } from '@playwright/test';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// When running E2E against the Docker compose stack, set:
//   USE_DOCKER=true npx playwright test
// The compose stack must already be running (`make docker-up`).
// Without USE_DOCKER, Playwright starts both servers locally (default local dev mode).
const useDocker = process.env.USE_DOCKER === 'true';

const baseURL = useDocker ? 'http://localhost:80' : 'http://localhost:5173';

export default defineConfig({
  testDir: './e2e',

  // Run auth setup before any test project
  projects: [
    {
      name: 'auth-setup',
      testMatch: /auth\.setup\.ts/,
    },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Load saved auth state so tests start already authenticated
        storageState: path.resolve(__dirname, 'e2e/.auth/user.json'),
      },
      dependencies: ['auth-setup'],
    },
  ],

  use: {
    baseURL,
    trace: 'on-first-retry',
  },

  // Only spin up local servers when NOT running against Docker
  webServer: useDocker
    ? undefined
    : [
        {
          command: 'cd ../server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun',
          port: 8080,
          timeout: 120_000,
          reuseExistingServer: !process.env.CI,
        },
        {
          command: 'npm run dev',
          port: 5173,
          timeout: 30_000,
          reuseExistingServer: !process.env.CI,
        },
      ],
});
