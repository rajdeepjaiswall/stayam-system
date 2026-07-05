import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const [user] = await sql`
    SELECT id, email, full_name, organisation_name, mobile, role, permissions, vibrate, created_at
    FROM users WHERE id = ${auth.user.sub}
  `;
  if (!user) return NextResponse.json({ error: 'User not found' }, { status: 404 });
  return NextResponse.json(user);
}

export async function PATCH(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  try {
    const { full_name, mobile, organisation_name, vibrate } = await req.json();

    const [user] = await sql`
      UPDATE users SET
        full_name = COALESCE(${full_name ?? null}, full_name),
        mobile = COALESCE(${mobile ?? null}, mobile),
        organisation_name = COALESCE(${organisation_name ?? null}, organisation_name),
        vibrate = COALESCE(${vibrate ?? null}, vibrate)
      WHERE id = ${auth.user.sub}
      RETURNING id, email, full_name, organisation_name, mobile, role, permissions, vibrate, created_at
    `;
    return NextResponse.json(user);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Update failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
