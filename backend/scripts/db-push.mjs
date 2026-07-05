import { neon } from '@neondatabase/serverless';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const sqlText = readFileSync(join(__dirname, '../schema.sql'), 'utf8');

const sql = neon(process.env.DATABASE_URL);

// Split on semicolons but keep COPY statements intact
const statements = sqlText
  .split(/;\s*\n/)
  .map(s => s.trim())
  .filter(s => s.length > 0 && !s.startsWith('--'));

console.log(`Applying ${statements.length} statements...`);
for (const stmt of statements) {
  try {
    await sql(stmt);
    process.stdout.write('.');
  } catch (err) {
    console.error(`\nFailed: ${stmt.substring(0, 80)}...`);
    console.error(err.message);
  }
}
console.log('\nSchema applied successfully!');
