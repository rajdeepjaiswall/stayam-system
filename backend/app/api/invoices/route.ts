import { NextRequest, NextResponse } from 'next/server';
import { sql } from '@/lib/db';
import { withAuth } from '@/lib/middleware';

function getFinancialYear(date: Date): string {
  const month = date.getMonth(); // 0-indexed
  const year = date.getFullYear();
  // April (3) starts new FY
  if (month >= 3) {
    return `${year}-${String(year + 1).slice(-2)}`;
  } else {
    return `${year - 1}-${String(year).slice(-2)}`;
  }
}

export async function GET(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  const invoices = await sql`
    SELECT i.*, c.name AS contact_name, c.organisation AS contact_organisation
    FROM invoices i
    LEFT JOIN contacts c ON i.contact_id = c.id
    ORDER BY i.created_at DESC
  `;
  return NextResponse.json(invoices);
}

export async function POST(req: NextRequest) {
  const auth = await withAuth(req);
  if ('error' in auth) return auth.error;

  try {
    const { contact_id, items } = await req.json();
    if (!items || !Array.isArray(items) || items.length === 0) {
      return NextResponse.json({ error: 'items array is required' }, { status: 400 });
    }

    // Compute totals
    let subtotal = 0;
    let tax = 0;
    const processedItems = items.map((item: {
      name: string;
      qty: number;
      rate: number;
      gst_percent?: number;
    }) => {
      const lineTotal = item.qty * item.rate;
      const lineTax = lineTotal * ((item.gst_percent || 0) / 100);
      subtotal += lineTotal;
      tax += lineTax;
      return { ...item, line_total: lineTotal, line_tax: lineTax };
    });
    const total = subtotal + tax;

    const now = new Date();
    const financial_year = getFinancialYear(now);
    const yyyymm = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`;

    // Get per-month counter
    const [countRow] = await sql`
      SELECT COUNT(*) + 1 AS nnn FROM invoices
      WHERE invoice_number LIKE ${'INV-' + yyyymm + '-%'}
    `;
    const nnn = String(countRow.nnn).padStart(3, '0');
    const invoice_number = `INV-${yyyymm}-${nnn}`;

    const [invoice] = await sql`
      INSERT INTO invoices (invoice_number, contact_id, items, subtotal, tax, total, financial_year, created_by)
      VALUES (
        ${invoice_number},
        ${contact_id || null},
        ${JSON.stringify(processedItems)},
        ${subtotal},
        ${tax},
        ${total},
        ${financial_year},
        ${auth.user.sub}
      )
      RETURNING *
    `;

    return NextResponse.json(invoice, { status: 201 });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Failed to create invoice';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
