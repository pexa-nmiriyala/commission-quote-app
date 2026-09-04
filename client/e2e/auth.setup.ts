/**
 * Playwright global setup — authenticates with Keycloak once and saves
 * the browser storage state to a file. All E2E tests load this state
 * so they start already authenticated without going through the login UI.
 *
 * Run order: this setup runs before the test suite via `project.dependencies`
 * in playwright.config.ts.
 */
import { test as setup, expect } from '@playwright/test';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const AUTH_STATE_FILE = path.resolve(__dirname, '.auth/user.json');

const KEYCLOAK_URL = process.env.VITE_KEYCLOAK_URL ?? 'http://localhost:9090';
const KEYCLOAK_REALM = process.env.VITE_KEYCLOAK_REALM ?? 'commission-app';
const KEYCLOAK_CLIENT_ID = process.env.VITE_KEYCLOAK_CLIENT_ID ?? 'commission-app-client';
const TEST_USERNAME = 'staff-user';
const TEST_PASSWORD = 'password123';

setup('authenticate with Keycloak', async ({ page }) => {
  // Navigate to the app — this will trigger the Keycloak check-sso flow
  await page.goto('/');

  // Wait for either the login button (unauthenticated) or the form (already authenticated)
  await page.waitForSelector('button:has-text("Log in"), label:has-text("Loan Amount")', {
    timeout: 15_000,
  });

  // If already authenticated (e.g. re-running with saved state), skip login
  const loginButton = page.getByRole('button', { name: /log in/i });
  if (!(await loginButton.isVisible())) {
    await page.context().storageState({ path: AUTH_STATE_FILE });
    return;
  }

  // Click "Log in" → redirects to Keycloak login page
  await loginButton.click();

  // Wait for Keycloak login page to load
  await page.waitForURL(`${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/**`, {
    timeout: 15_000,
  });

  // Fill in credentials
  await page.fill('#username', TEST_USERNAME);
  await page.fill('#password', TEST_PASSWORD);
  await page.click('#kc-login');

  // Wait for redirect back to the app and the form to appear
  await expect(page.getByLabel(/loan amount/i)).toBeVisible({ timeout: 15_000 });

  // Save authenticated browser state for all other tests
  await page.context().storageState({ path: AUTH_STATE_FILE });
});
