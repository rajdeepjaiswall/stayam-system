import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';
import { hashPassword, signToken } from '@/lib/auth';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  if (!isAdmin(auth.user)) return NextResponse.json({ error: 'Admin only' }, { status: 403 });

  const users = await sql`
    SELECT id, email, full_name, organisation_name, mobile, role, permissions, is_disabled, vibrate, created_at
    FROM users ORDER BY created_at ASC
  `;
  return NextResponse.json(users);
}

export async function POST(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  if (!isAdmin(auth.user)) return NextResponse.json({ error: 'Admin only' }, { status: 403 });

  try {
    const { email, password, full_name, permissions } = await req.json();
    if (!email || !password || !full_name) {
      return NextResponse.json({ error: 'email, password, full_name required' }, { status: 400 });
    }

    const password_hash = await hashPassword(password);
    const perms = permissions || {
      manage_contacts: true,
      manage_events: true,
      manage_invoices: true,
      view_team: true,
    };

    const [user] = await sql`
      INSERT INTO users (email, password_hash, full_name, role, permissions)
      VALUES (${email.toLowerCase().trim()}, ${password_hash}, ${full_name}, 'member', ${JSON.stringify(perms)})
      RETURNING id, email, full_name, role, permissions, is_disabled, created_at
    `;

    const token = await signToken({ sub: user.id, role: user.role, permissions: user.permissions });
    return NextResponse.json({ token, user }, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
