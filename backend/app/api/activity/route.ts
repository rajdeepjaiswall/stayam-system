import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const activity = await sql`
    SELECT
      ra.*,
      e.title AS event_title,
      c.name AS contact_name,
      c.organisation AS contact_organisation,
      u.full_name AS actor_name,
      t.name AS tag_name,
      t.color AS tag_color
    FROM reminder_activity ra
    JOIN events e ON ra.event_id = e.id
    LEFT JOIN contacts c ON e.contact_id = c.id
    LEFT JOIN users u ON ra.actor_id = u.id
    LEFT JOIN event_tags t ON e.tag_id = t.id
    ORDER BY ra.created_at DESC
    LIMIT 100
  `;

  return NextResponse.json(activity);
}
