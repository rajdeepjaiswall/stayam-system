import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';
import { hashPassword } from '@/lib/auth';

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  if (!isAdmin(auth.user)) return NextResponse.json({ error: 'Admin only' }, { status: 403 });

  try {
    const { permissions, is_disabled, password } = await req.json();

    let password_hash = null;
    if (password) {
      password_hash = await hashPassword(password);
    }

    const [user] = await sql`
      UPDATE users SET
        permissions = COALESCE(${permissions ? JSON.stringify(permissions) : null}::jsonb, permissions),
        is_disabled = COALESCE(${is_disabled ?? null}, is_disabled),
        password_hash = COALESCE(${password_hash}, password_hash)
      WHERE id = ${params.id}
      RETURNING id, email, full_name, role, permissions, is_disabled, created_at
    `;

    if (!user) return NextResponse.json({ error: 'Not found' }, { status: 404 });
    return NextResponse.json(user);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
