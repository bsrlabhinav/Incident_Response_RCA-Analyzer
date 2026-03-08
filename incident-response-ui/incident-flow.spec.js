/**
 * Incident flow E2E test: Incidents list → Create → Detail → Assign → Transition
 * Run: npx playwright test incident-flow.spec.js --project=chromium
 * Screenshots saved to test-results/
 */
import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'

test.describe('Incident flow', () => {
  test('full flow: list → create → assign → transition', async ({ page }) => {
    test.setTimeout(60000)
    // 1. Navigate to /incidents and verify page loads
    await page.goto(`${BASE_URL}/incidents`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h2')).toContainText('Incidents')
    await page.screenshot({ path: 'test-results/01-incidents-list.png', fullPage: true })

    // Verify at least one incident exists (e.g. "Test from UI")
    const table = page.locator('table tbody')
    await expect(table).toBeVisible()
    const hasTestIncident = await page.locator('text=Test from UI').count() > 0
    if (!hasTestIncident) {
      console.warn('Note: "Test from UI" incident not found in table - may be empty or different data')
    }

    // 2. Navigate to /incidents/new and fill form
    await page.goto(`${BASE_URL}/incidents/new`)
    await page.waitForLoadState('networkidle')

    await page.getByPlaceholder(/e\.g\. Database/).fill('Payment gateway timeout')
    await page.getByPlaceholder(/Describe the incident/).fill('Users reporting 504 errors on checkout')
    // Click the label (radio is sr-only, label intercepts)
    await page.locator('label').filter({ hasText: 'CRITICAL' }).click()
    await page.getByRole('button', { name: 'Create Incident' }).click()

    // 3. Should redirect to incident detail page
    await expect(page).toHaveURL(/\/incidents\/[a-f0-9-]+/)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h2')).toContainText('Payment gateway timeout')
    await page.screenshot({ path: 'test-results/02-incident-detail-after-create.png', fullPage: true })

    // 4. Assign Owner - click Assign (leave UUID blank to auto-generate)
    await expect(page.getByRole('button', { name: 'Assign' })).toBeVisible()
    await page.getByRole('button', { name: 'Assign' }).click()
    await page.waitForLoadState('networkidle')
    await page.screenshot({ path: 'test-results/03-after-assign.png', fullPage: true })

    // 5. Transition to INVESTIGATING
    await page.getByRole('button', { name: /Transition.*INVESTIGATING/ }).click()
    await page.waitForLoadState('networkidle')
    await page.screenshot({ path: 'test-results/04-after-transition.png', fullPage: true })

    // Verify status changed (use .first() - INVESTIGATING appears in badge and workflow)
    await expect(page.getByText('INVESTIGATING').first()).toBeVisible()
  })
})
