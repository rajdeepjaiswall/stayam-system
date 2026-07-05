import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  try {
    const { status, snoozed_until } = await req.json();

    if (!status) {
      return NextResponse.json({ error: 'status is required' }, { status: 400 });
    }

    const validStatuses = ['pending', 'snoozed', 'triggered', 'ended', 'delegated'];
    if (!validStatuses.includes(status)) {
      return NextResponse.json({ error: `status must be one of: ${validStatuses.join(', ')}` }, { status: 400 });
    }

    const [reminder] = await sql`
      UPDATE reminders SET
        status = ${status},
        snoozed_until = ${snoozed_until || null}
      WHERE id = ${params.id}
      RETURNING *
    `;

    if (!reminder) return NextResponse.json({ error: 'Not found' }, { status: 404 });

    // Map status to activity action
    const actionMap: Record<string, string> = {
      triggered: 'triggered',
      snoozed: 'snoozed',
      ended: 'ended',
      delegated: 'delegated',
    };

    const action = actionMap[status];
    if (action) {
      await sql`
        INSERT INTO reminder_activity (reminder_id, event_id, actor_id, action, detail)
        VALUES (
          ${params.id},
          ${reminder.event_id},
          ${auth.user.sub},
          ${action},
          ${snoozed_until ? `Snoozed until ${snoozed_until}` : null}
        )
      `;
    }

    return NextResponse.json(reminder);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Update failed';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
