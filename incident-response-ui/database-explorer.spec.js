/**
 * Database Explorer E2E verification
 * Run: npx playwright test database-explorer.spec.js
 * Screenshots saved to test-results/
 */
import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'

test.describe('Database Explorer', () => {
  test('full verification flow', async ({ page }) => {
    test.setTimeout(90000)

    // 1. Navigate to /database and screenshot full page (10 incident documents)
    await page.goto(`${BASE_URL}/database`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h2')).toContainText('Database Explorer')
    await page.screenshot({ path: 'test-results/db-01-full-page.png', fullPage: true })

    const docCountEl = page.getByText(/\d+ documents?/).first()
    const docCount = await docCountEl.textContent()
    const incidentCards = page.locator('[class*="rounded-xl"][class*="border"]').filter({ has: page.locator('button') })
    const count = await incidentCards.count()
    console.log(`Step 1: Found ${count} incident documents, doc count text: ${docCount}`)

    // 2. Ack SLA Breached = "Yes — Breached" → expect 3 CRITICAL incidents with red breach indicators
    await page.getByRole('combobox').nth(2).selectOption({ label: 'Yes — Breached' }) // Status=0, Severity=1, Ack SLA=2
    await page.waitForLoadState('networkidle')
    await page.screenshot({ path: 'test-results/db-02-ack-breached-filter.png', fullPage: true })

    const breachedCount = await page.locator('[class*="border-red-200"]').count()
    const criticalBadges = await page.getByText('CRITICAL').count()
    console.log(`Step 2: Ack breached filter — ${breachedCount} cards with red border, ${criticalBadges} CRITICAL badges`)

    // 3. Clear filter and Status = CLOSED → expect 6 incidents
    await page.getByRole('button', { name: 'Clear all' }).click()
    await page.waitForLoadState('networkidle')
    await page.getByRole('combobox').first().selectOption({ label: 'CLOSED' }) // Status is first combobox
    await page.waitForLoadState('networkidle')
    await page.screenshot({ path: 'test-results/db-03-status-closed.png', fullPage: true })

    const closedCount = await page.locator('[class*="rounded-xl"][class*="border"]').filter({ has: page.locator('button') }).count()
    console.log(`Step 3: Status CLOSED filter — ${closedCount} incidents`)

    // 4. Clear filter, re-apply Ack SLA Breached, expand first CRITICAL breached incident
    await page.getByRole('button', { name: 'Clear all' }).click()
    await page.waitForLoadState('networkidle')
    await page.getByRole('combobox').nth(2).selectOption({ label: 'Yes — Breached' })
    await page.waitForLoadState('networkidle')

    // Expand first CRITICAL SLA-breached incident (first row with red border)
    const firstBreached = page.locator('[class*="border-red-200"]').first()
    await firstBreached.locator('button').first().click()
    await page.waitForTimeout(500)
    await expect(page.locator('text=SLA Details')).toBeVisible()
    await page.screenshot({ path: 'test-results/db-04-expanded-sla-details.png', fullPage: true })
  })
})
