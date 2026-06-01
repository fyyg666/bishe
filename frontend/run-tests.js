import { execSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))

try {
  const output = execSync('npx vitest run --reporter verbose 2>&1', {
    cwd: __dirname,
    encoding: 'utf8',
    maxBuffer: 50 * 1024 * 1024,
    timeout: 120000
  })
  const lines = output.split('\n')
  const summary = lines.slice(-12).join('\n')
  writeFileSync(join(__dirname, 'test-summary.txt'), summary, 'utf8')
  console.log('DONE')
} catch (e) {
  const stderr = e.stderr || ''
  const stdout = e.stdout || ''
  const lines = (stdout + stderr).split('\n')
  const summary = lines.slice(-12).join('\n')
  writeFileSync(join(__dirname, 'test-summary.txt'), summary, 'utf8')
  console.log('DONE_WITH_FAILURES')
}
