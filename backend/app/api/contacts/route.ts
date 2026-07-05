import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const search = req.nextUrl.searchParams.get('search') || '';
  const like = `%${search}%`;

  let contacts;
  if (isAdmin(auth.user)) {
    contacts = await sql`
      SELECT * FROM contacts
      WHERE (${search} = '' OR name ILIKE ${like} OR mobile ILIKE ${like} OR email ILIKE ${like} OR organisation ILIKE ${like})
      ORDER BY created_at DESC
    `;
  } else {
    contacts = await sql`
      SELECT * FROM contacts
      WHERE created_by = ${auth.user.sub}
      AND (${search} = '' OR name ILIKE ${like} OR mobile ILIKE ${like} OR email ILIKE ${like} OR organisation ILIKE ${like})
      ORDER BY created_at DESC
    `;
  }
  return NextResponse.json(contacts);
}

export async function POST(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  try {
    const { name, organisation, mobile, whatsapp, email, address, notes } = await req.json();
    if (!name) return NextResponse.json({ error: 'name is required' }, { status: 400 });

    const [contact] = await sql`
      INSERT INTO contacts (name, organisation, mobile, whatsapp, email, address, notes, created_by)
      VALUES (${name}, ${organisation || null}, ${mobile || null}, ${whatsapp || null},
              ${email || null}, ${address || null}, ${notes || null}, ${auth.user.sub})
      RETURNING *
    `;
    return NextResponse.json(contact, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed to create contact';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
