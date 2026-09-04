import { test, expect } from '@playwright/test';

/**
 * All tests in this file start already authenticated — the auth state
 * is loaded from .auth/user.json (written by auth.setup.ts).
 *
 * Each test waits for the form to be visible before interacting,
 * since Keycloak's check-sso initialisation is async.
 */

/** Wait for the quote form to be visible after auth initialisation. */
async function waitForForm(
  page: Parameters<typeof test>[1] extends (args: { page: infer P }) => unknown ? P : never
) {
  await expect(page.getByLabel(/loan amount/i)).toBeVisible({ timeout: 15_000 });
}

// ─── Happy path ───────────────────────────────────────────────────────────────
test('fills form and shows quote result', async ({ page }) => {
  await page.goto('/');
  await waitForForm(page);

  // Intercept the API call and always return a fixed quote for deterministic assertions
  await page.route('/api/commission-quote', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: 'test-quote-id-001',
        commission: 5833.33,
        totalRepayable: 55833.33,
      }),
    });
  });

  await page.fill('#loanAmount', '50000');
  await page.fill('#loanTermMonths', '36');
  await page.selectOption('#riskBand', 'medium');
  await page.click('button[type="submit"]');

  const resultSection = page.getByRole('region', { name: 'Quote result' });
  await expect(resultSection).toBeVisible({ timeout: 10_000 });
  await expect(resultSection).toContainText('test-quote-id-001');

  const commissionText = await resultSection.textContent();
  expect(commissionText).toMatch(/(?:A?\$|AUD\s?)[\d,]+\.\d{2}/);
});

// ─── Auth — unauthenticated user sees login button ────────────────────────────
test('shows login button when not authenticated', async ({ browser }) => {
  // Use a fresh browser context with NO saved auth state and NO cookies
  // (storageState: empty ensures Keycloak SSO session cookies don't carry over)
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
  const page = await context.newPage();

  await page.goto('/');
  await expect(page.getByRole('button', { name: /log in/i })).toBeVisible({ timeout: 15_000 });
  await expect(page.getByLabel(/loan amount/i)).not.toBeVisible();

  await context.close();
});

// ─── Validation errors ────────────────────────────────────────────────────────
test('shows validation error when loanAmount is empty', async ({ page }) => {
  await page.goto('/');
  await waitForForm(page);

  await page.fill('#loanTermMonths', '36');
  await page.selectOption('#riskBand', 'medium');
  await page.click('button[type="submit"]');

  const errorSpan = page.locator('#loanAmount-error');
  await expect(errorSpan).toBeVisible();
  await expect(errorSpan).toContainText(/required|positive/i);

  await expect(page.getByRole('region', { name: 'Quote result' })).not.toBeVisible();
});

// ─── API error ────────────────────────────────────────────────────────────────
test('shows error message when API returns 502', async ({ page }) => {
  await page.goto('/');
  await waitForForm(page);

  await page.route('/api/commission-quote', async (route) => {
    await route.fulfill({
      status: 502,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'Commission API error' }),
    });
  });

  await page.fill('#loanAmount', '50000');
  await page.fill('#loanTermMonths', '36');
  await page.selectOption('#riskBand', 'medium');
  await page.click('button[type="submit"]');

  const errorDiv = page.getByRole('alert');
  await expect(errorDiv).toBeVisible({ timeout: 5_000 });
  await expect(errorDiv).toContainText('Commission API error');
  await expect(page.getByRole('region', { name: 'Quote result' })).not.toBeVisible();
});

// ─── Loading state ────────────────────────────────────────────────────────────
test('shows loading indicator while request is in-flight', async ({ page }) => {
  await page.goto('/');
  await waitForForm(page);

  await page.route('/api/commission-quote', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2000));
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        quoteId: 'loading-test-id',
        commission: 5833.33,
        totalRepayable: 55833.33,
      }),
    });
  });

  await page.fill('#loanAmount', '50000');
  await page.fill('#loanTermMonths', '36');
  await page.selectOption('#riskBand', 'medium');
  await page.click('button[type="submit"]');

  await expect(page.getByRole('status', { name: 'Loading' })).toBeVisible({ timeout: 1_000 });
  await expect(page.locator('button[type="submit"]')).toBeDisabled();
  await expect(page.getByRole('region', { name: 'Quote result' })).toBeVisible({ timeout: 10_000 });
});

// ─── Retry after error ────────────────────────────────────────────────────────
test('clears error and shows new result on retry after error', async ({ page }) => {
  await page.goto('/');
  await waitForForm(page);

  let callCount = 0;
  await page.route('/api/commission-quote', async (route) => {
    callCount++;
    if (callCount === 1) {
      await route.fulfill({
        status: 502,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Commission API error' }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          quoteId: 'retry-success-id',
          commission: 5833.33,
          totalRepayable: 55833.33,
        }),
      });
    }
  });

  await page.fill('#loanAmount', '50000');
  await page.fill('#loanTermMonths', '36');
  await page.selectOption('#riskBand', 'medium');

  await page.click('button[type="submit"]');
  const errorDiv = page.getByRole('alert');
  await expect(errorDiv).toBeVisible({ timeout: 5_000 });

  await page.click('button[type="submit"]');
  await expect(errorDiv).not.toBeVisible({ timeout: 5_000 });

  const resultSection = page.getByRole('region', { name: 'Quote result' });
  await expect(resultSection).toBeVisible({ timeout: 10_000 });
  await expect(resultSection).toContainText('retry-success-id');
});
