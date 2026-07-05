import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  const products = await sql`SELECT * FROM products ORDER BY name`;
  return NextResponse.json(products);
}

export async function POST(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  try {
    const { name, description, price, gst_percent } = await req.json();
    if (!name) return NextResponse.json({ error: 'name required' }, { status: 400 });
    const [p] = await sql`
      INSERT INTO products (name, description, price, gst_percent)
      VALUES (${name}, ${description || null}, ${price || null}, ${gst_percent || null})
      RETURNING *
    `;
    return NextResponse.json(p, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
