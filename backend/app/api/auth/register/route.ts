import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { hashPassword, signToken } from '@/lib/auth';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { email, password, full_name, organisation_name, mobile } = body;

    if (!email || !password || !full_name) {
      return NextResponse.json(
        { error: 'email, password, and full_name are required' },
        { status: 400 }
      );
    }

    // Check if email already exists
    const existing = await sql`SELECT id FROM users WHERE email = ${email.toLowerCase().trim()}`;
    if (existing.length > 0) {
      return NextResponse.json({ error: 'Email already registered' }, { status: 409 });
    }

    const password_hash = await hashPassword(password);

    // Determine role: first user is admin
    const countResult = await sql`SELECT COUNT(*)::int AS cnt FROM users`;
    const isFirst = countResult[0].cnt === 0;
    const role = isFirst ? 'admin' : 'member';
    const permissions = {
      manage_contacts: true,
      manage_events: true,
      manage_invoices: true,
      view_team: true,
    };

    const [user] = await sql`
      INSERT INTO users (email, password_hash, full_name, organisation_name, mobile, role, permissions)
      VALUES (
        ${email.toLowerCase().trim()},
        ${password_hash},
        ${full_name},
        ${organisation_name || null},
        ${mobile || null},
        ${role},
        ${JSON.stringify(permissions)}
      )
      RETURNING id, email, full_name, organisation_name, mobile, role, permissions, created_at
    `;

    const token = await signToken({
      sub: user.id,
      role: user.role,
      permissions: user.permissions,
    });

    return NextResponse.json({ token, user }, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Registration failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
