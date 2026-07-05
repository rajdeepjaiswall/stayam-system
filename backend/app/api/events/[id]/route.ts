import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const [existing] = await sql`SELECT * FROM events WHERE id = ${params.id}`;
  if (!existing) return NextResponse.json({ error: 'Not found' }, { status: 404 });
  if (!isAdmin(auth.user) && existing.created_by !== auth.user.sub && existing.assigned_to !== auth.user.sub) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }

  try {
    const { status, title, notes, assigned_to, tag_id } = await req.json();
    const [event] = await sql`
      UPDATE events SET
        status = COALESCE(${status ?? null}, status),
        title = COALESCE(${title ?? null}, title),
        notes = COALESCE(${notes ?? null}, notes),
        assigned_to = COALESCE(${assigned_to ?? null}::uuid, assigned_to),
        tag_id = COALESCE(${tag_id ?? null}::uuid, tag_id)
      WHERE id = ${params.id}
      RETURNING *
    `;
    return NextResponse.json(event);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Update failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const [existing] = await sql`SELECT * FROM events WHERE id = ${params.id}`;
  if (!existing) return NextResponse.json({ error: 'Not found' }, { status: 404 });
  if (!isAdmin(auth.user) && existing.created_by !== auth.user.sub) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }

  await sql`DELETE FROM events WHERE id = ${params.id}`;
  return NextResponse.json({ success: true });
}
