import { neon } from '@neondatabase/serverless';

// neon() is lazy — it does not connect at module load time.
// The actual DB connection happens only when sql`` is called inside a route handler,
// so DATABASE_URL only needs to be present at request time (not build time).
export const sql = neon(process.env.DATABASE_URL!);
