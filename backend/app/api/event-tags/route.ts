import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  const tags = await sql`SELECT * FROM event_tags ORDER BY name`;
  return NextResponse.json(tags);
}

export async function POST(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  try {
    const { name, color } = await req.json();
    if (!name || !color) return NextResponse.json({ error: 'name and color required' }, { status: 400 });
    const [tag] = await sql`INSERT INTO event_tags (name, color) VALUES (${name}, ${color}) RETURNING *`;
    return NextResponse.json(tag, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
