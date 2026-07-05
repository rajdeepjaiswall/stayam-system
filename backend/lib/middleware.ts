import { NextRequest, NextResponse } from 'next/server';
import { verifyToken, TokenPayload } from './auth';

export async function withAuth(
  req: NextRequest
): Promise<{ user: TokenPayload } | { error: NextResponse }> {
  const auth = req.headers.get('authorization');
  if (!auth?.startsWith('Bearer ')) {
    return { error: NextResponse.json({ error: 'Unauthorized' }, { status: 401 }) };
  }
  try {
    const user = await verifyToken(auth.slice(7));
    return { user };
  } catch {
    return { error: NextResponse.json({ error: 'Invalid or expired token' }, { status: 401 }) };
  }
}

export function isAdmin(user: TokenPayload): boolean {
  return user.role === 'admin';
}
