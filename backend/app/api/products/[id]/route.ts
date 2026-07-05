import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  try {
    const { name, description, price, gst_percent } = await req.json();
    const [p] = await sql`
      UPDATE products SET
        name = COALESCE(${name ?? null}, name),
        description = COALESCE(${description ?? null}, description),
        price = COALESCE(${price ?? null}, price),
        gst_percent = COALESCE(${gst_percent ?? null}, gst_percent)
      WHERE id = ${params.id} RETURNING *
    `;
    if (!p) return NextResponse.json({ error: 'Not found' }, { status: 404 });
    return NextResponse.json(p);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;
  await sql`DELETE FROM products WHERE id = ${params.id}`;
  return NextResponse.json({ success: true });
}
