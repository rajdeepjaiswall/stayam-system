import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { checkPassword, signToken } from '@/lib/auth';

export async function POST(req: NextRequest) {
  try {
    const { email, password } = await req.json();

    if (!email || !password) {
      return NextResponse.json({ error: 'email and password are required' }, { status: 400 });
    }

    const [user] = await sql`
      SELECT id, email, full_name, organisation_name, mobile, role, permissions, password_hash, is_disabled
      FROM users WHERE email = ${email.toLowerCase().trim()}
    `;

    if (!user) {
      return NextResponse.json({ error: 'Invalid credentials' }, { status: 401 });
    }

    if (user.is_disabled) {
      return NextResponse.json({ error: 'Account is disabled' }, { status: 403 });
    }

    const valid = await checkPassword(password, user.password_hash);
    if (!valid) {
      return NextResponse.json({ error: 'Invalid credentials' }, { status: 401 });
    }

    const token = await signToken({
      sub: user.id,
      role: user.role,
      permissions: user.permissions,
    });

    const { password_hash: _, ...safeUser } = user;
    return NextResponse.json({ token, user: safeUser });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Login failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
