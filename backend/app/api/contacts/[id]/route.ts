import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';

export async function GET(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const [contact] = await sql`SELECT * FROM contacts WHERE id = ${params.id}`;
  if (!contact) return NextResponse.json({ error: 'Not found' }, { status: 404 });
  if (!isAdmin(auth.user) && contact.created_by !== auth.user.sub) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }
  return NextResponse.json(contact);
}

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const [existing] = await sql`SELECT * FROM contacts WHERE id = ${params.id}`;
  if (!existing) return NextResponse.json({ error: 'Not found' }, { status: 404 });
  if (!isAdmin(auth.user) && existing.created_by !== auth.user.sub) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }

  try {
    const { name, organisation, mobile, whatsapp, email, address, notes } = await req.json();
    const [contact] = await sql`
      UPDATE contacts SET
        name = COALESCE(${name ?? null}, name),
        organisation = COALESCE(${organisation ?? null}, organisation),
        mobile = COALESCE(${mobile ?? null}, mobile),
        whatsapp = COALESCE(${whatsapp ?? null}, whatsapp),
        email = COALESCE(${email ?? null}, email),
        address = COALESCE(${address ?? null}, address),
        notes = COALESCE(${notes ?? null}, notes)
      WHERE id = ${params.id}
      RETURNING *
    `;
    return NextResponse.json(contact);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Update failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const [existing] = await sql`SELECT * FROM contacts WHERE id = ${params.id}`;
  if (!existing) return NextResponse.json({ error: 'Not found' }, { status: 404 });
  if (!isAdmin(auth.user) && existing.created_by !== auth.user.sub) {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
  }

  await sql`DELETE FROM contacts WHERE id = ${params.id}`;
  return NextResponse.json({ success: true });
}
