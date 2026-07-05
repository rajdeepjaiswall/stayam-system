import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  try {
    const { name, color } = await req.json();
    const [tag] = await sql`
      UPDATE event_tags SET
        name = COALESCE(${name ?? null}, name),
        color = COALESCE(${color ?? null}, color)
      WHERE id = ${params.id} RETURNING *
    `;
    if (!tag) return NextResponse.json({ error: 'Not found' }, { status: 404 });
    return NextResponse.json(tag);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  await sql`DELETE FROM event_tags WHERE id = ${params.id}`;
  return NextResponse.json({ success: true });
}
