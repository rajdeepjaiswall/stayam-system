import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth, isAdmin } from '@/lib/middleware';

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const { searchParams } = req.nextUrl;
  const status = searchParams.get('status');
  const tag_id = searchParams.get('tag_id');
  const assigned_to = searchParams.get('assigned_to');

  const events = await sql`
    SELECT
      e.*,
      c.name AS contact_name, c.organisation AS contact_organisation,
      c.mobile AS contact_mobile, c.whatsapp AS contact_whatsapp,
      t.name AS tag_name, t.color AS tag_color,
      u.full_name AS assignee_name,
      COALESCE(
        (SELECT json_agg(r.* ORDER BY r.remind_at) FROM reminders r WHERE r.event_id = e.id),
        '[]'::json
      ) AS reminders
    FROM events e
    LEFT JOIN contacts c ON e.contact_id = c.id
    LEFT JOIN event_tags t ON e.tag_id = t.id
    LEFT JOIN users u ON e.assigned_to = u.id
    WHERE
      (${!isAdmin(auth.user)} = false OR e.created_by = ${auth.user.sub} OR e.assigned_to = ${auth.user.sub})
      AND (${status} IS NULL OR e.status = ${status})
      AND (${tag_id} IS NULL OR e.tag_id = ${tag_id}::uuid)
      AND (${assigned_to} IS NULL OR e.assigned_to = ${assigned_to}::uuid)
    ORDER BY
      (SELECT MIN(r.remind_at) FROM reminders r WHERE r.event_id = e.id AND r.status = 'pending') ASC NULLS LAST,
      e.created_at DESC
  `;
  return NextResponse.json(events);
}

export async function POST(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  try {
    const { contact_id, tag_id, title, notes, assigned_to, reminders } = await req.json();
    if (!title) return NextResponse.json({ error: 'title is required' }, { status: 400 });

    const [event] = await sql`
      INSERT INTO events (contact_id, tag_id, title, notes, assigned_to, created_by)
      VALUES (
        ${contact_id || null},
        ${tag_id || null},
        ${title},
        ${notes || null},
        ${assigned_to || auth.user.sub},
        ${auth.user.sub}
      )
      RETURNING *
    `;

    // Insert reminders
    if (Array.isArray(reminders) && reminders.length > 0) {
      for (const r of reminders) {
        if (r.remind_at) {
          await sql`
            INSERT INTO reminders (event_id, remind_at)
            VALUES (${event.id}, ${r.remind_at})
          `;
        }
      }
    }

    // Return event with reminders
    const [full] = await sql`
      SELECT
        e.*,
        c.name AS contact_name, c.organisation AS contact_organisation,
        t.name AS tag_name, t.color AS tag_color,
        COALESCE(
          (SELECT json_agg(r.* ORDER BY r.remind_at) FROM reminders r WHERE r.event_id = e.id),
          '[]'::json
        ) AS reminders
      FROM events e
      LEFT JOIN contacts c ON e.contact_id = c.id
      LEFT JOIN event_tags t ON e.tag_id = t.id
      WHERE e.id = ${event.id}
    `;

    return NextResponse.json(full, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed to create event';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
