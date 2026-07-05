import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function GET(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  const [inv] = await sql`
    SELECT i.*, c.name AS contact_name, c.organisation AS contact_organisation,
           c.address AS contact_address, c.mobile AS contact_mobile
    FROM invoices i LEFT JOIN contacts c ON i.contact_id = c.id
    WHERE i.id = ${params.id}
  `;
  if (!inv) return NextResponse.json({ error: 'Not found' }, { status: 404 });
  return NextResponse.json(inv);
}

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  try {
    const { status } = await req.json();
    const [inv] = await sql`
      UPDATE invoices SET status = COALESCE(${status ?? null}, status)
      WHERE id = ${params.id} RETURNING *
    `;
    if (!inv) return NextResponse.json({ error: 'Not found' }, { status: 404 });
    return NextResponse.json(inv);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
