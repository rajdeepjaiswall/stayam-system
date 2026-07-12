import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';

// GET /api/reminders?due=1 — alarm sync feed
export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const reminders = await sql`
    SELECT
      r.id,
      r.event_id,
      r.remind_at,
      r.status,
      r.snoozed_until,
      COALESCE(r.snoozed_until, r.remind_at) AS effective_time,
      e.title AS event_title,
      e.notes AS event_notes,
      e.status AS event_status,
      t.name AS tag_name,
      t.color AS tag_color,
      c.name AS contact_name,
      c.organisation AS contact_organisation,
      c.mobile AS contact_mobile,
      c.whatsapp AS contact_whatsapp
    FROM reminders r
    JOIN events e ON r.event_id = e.id
    LEFT JOIN event_tags t ON e.tag_id = t.id
    LEFT JOIN contacts c ON e.contact_id = c.id
    WHERE
      r.status IN ('pending', 'snoozed')
      AND (
        ${isAdmin(auth.user)} OR e.created_by = ${auth.user.sub} OR e.assigned_to = ${auth.user.sub}
      )
      AND COALESCE(r.snoozed_until, r.remind_at) <= now() + INTERVAL '365 days'
    ORDER BY COALESCE(r.snoozed_until, r.remind_at) ASC
  `;

  return NextResponse.json(reminders);
}
