import { defineConfig } from '@playwright/test'

// When running E2E against the Docker compose stack, set:
//   USE_DOCKER=true npx playwright test
// The compose stack must already be running (`make docker-up`).
// Without USE_DOCKER, Playwright starts both servers locally (default local dev mode).
const useDocker = process.env.USE_DOCKER === 'true'

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: useDocker ? 'http://localhost:80' : 'http://localhost:5173',
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
})
