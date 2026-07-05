CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  full_name TEXT NOT NULL,
  organisation_name TEXT,
  mobile TEXT,
  role TEXT NOT NULL DEFAULT 'member',
  permissions JSONB NOT NULL DEFAULT '{"manage_contacts":true,"manage_events":true,"manage_invoices":true,"view_team":true}',
  is_disabled BOOLEAN NOT NULL DEFAULT false,
  vibrate BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS event_tags (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  color TEXT NOT NULL
);

INSERT INTO event_tags (name, color) VALUES
  ('Call','#F97316'),
  ('Demo Call','#3B82F6'),
  ('Payment Collection','#22C55E'),
  ('Payment Request','#EAB308'),
  ('AMC Renewal','#EF4444'),
  ('Follow Up','#8B5CF6'),
  ('Installation','#10B981'),
  ('Meeting','#EC4899')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS contacts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  organisation TEXT,
  mobile TEXT,
  whatsapp TEXT,
  email TEXT,
  address TEXT,
  notes TEXT,
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  contact_id UUID REFERENCES contacts(id),
  tag_id UUID REFERENCES event_tags(id),
  title TEXT NOT NULL,
  notes TEXT,
  assigned_to UUID REFERENCES users(id),
  status TEXT NOT NULL DEFAULT 'upcoming' CHECK (status IN ('upcoming','completed','cancelled')),
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reminders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  remind_at TIMESTAMPTZ NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','snoozed','triggered','ended','delegated')),
  snoozed_until TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  price NUMERIC(12,2),
  gst_percent NUMERIC(5,2)
);

CREATE TABLE IF NOT EXISTS invoices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_number TEXT UNIQUE NOT NULL,
  contact_id UUID REFERENCES contacts(id),
  items JSONB NOT NULL,
  subtotal NUMERIC(12,2) NOT NULL,
  tax NUMERIC(12,2) NOT NULL,
  total NUMERIC(12,2) NOT NULL,
  status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','sent','paid','cancelled')),
  financial_year TEXT NOT NULL,
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reminder_activity (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reminder_id UUID REFERENCES reminders(id),
  event_id UUID REFERENCES events(id),
  actor_id UUID REFERENCES users(id),
  action TEXT NOT NULL CHECK (action IN ('triggered','snoozed','ended','delegated')),
  detail TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
