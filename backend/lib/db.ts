import { neon } from '@neondatabase/serverless';

// Lazily initialize neon so DATABASE_URL is only needed at request time, not build time.
let _sql: ReturnType<typeof neon> | null = null;
function getDb() {
  if (!_sql) { _sql = neon(process.env.DATABASE_URL!); }
  return _sql;
}
export const sql = new Proxy(
  function (strings: TemplateStringsArray, ...values: unknown[]) {
    return getDb()(strings, ...values);
  } as ReturnType<typeof neon>,
  { get(_t, prop) { return (getDb() as any)[prop]; } }
);