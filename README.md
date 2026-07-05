# Sahu Sales Solution

A complete sales management app: Next.js REST backend on Vercel + native Android app (Kotlin/Compose) with full-screen reminder alarms.

---

## Backend

### Environment Variables

| Variable | Description |
|---|---|
| `DATABASE_URL` | Neon Postgres connection string |
| `JWT_SECRET` | 64-char random hex secret |

### Setup

```bash
cd backend
npm install
```

### Apply Schema (first time)

```bash
DATABASE_URL="your_neon_url" npm run db:push
```

### Deploy to Vercel

```bash
npm i -g vercel
vercel login
cd backend
vercel link
vercel env add DATABASE_URL production
vercel env add JWT_SECRET production
vercel deploy --prod
```

### API Endpoints

- `POST /api/auth/register` — First user becomes admin
- `POST /api/auth/login`
- `GET|PATCH /api/me`
- `GET|POST /api/contacts`, `GET|PATCH|DELETE /api/contacts/:id`
- `GET|POST /api/events`, `PATCH|DELETE /api/events/:id`
- `GET /api/reminders?due=1` — Alarm sync feed
- `PATCH /api/reminders/:id` — Update status (triggered/snoozed/ended)
- `GET /api/activity`
- `GET|POST /api/event-tags`, `PATCH|DELETE /api/event-tags/:id`
- `GET|POST /api/products`, `PATCH|DELETE /api/products/:id`
- `GET|POST /api/invoices`, `GET|PATCH /api/invoices/:id`
- `GET|POST /api/team` (admin only), `PATCH /api/team/:id`

---

## Android App

### Build

Requirements: JDK 17, Android Studio with SDK (API 26+)

```bash
cd android
./gradlew assembleDebug
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`

### Install

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

Or side-load via file manager on the phone.

### Phone Permission Checklist (REQUIRED for alarms to work)

After installing, go to **Settings → Alarm Permissions** in the app:

1. **Notifications** — Grant when prompted on first launch
2. **Exact Alarms** — Tap "FIX" → Enable "Alarms & Reminders" for Sahu Sales
3. **Battery Optimization** — Tap "FIX" → Select "Don't optimize" for Sahu Sales
4. **OEM Autostart** — Tap "Open Autostart Settings" → Enable autostart for Sahu Sales
   - Xiaomi/MIUI: Security → Permissions → Autostart
   - Oppo/ColorOS: Phone Manager → Startup Manager
   - Vivo/Funtouch: Phone Manager → Memory Usage → Autostart
   - Realme: Phone Manager → Startup Manager

### Test the Alarm

1. Open app → Login
2. Tap the **Dashboard** icon 7 times rapidly → Debug Screen opens
3. Tap **"TEST ALARM IN 2 MIN"**
4. Force-close the app (swipe away from recents)
5. Lock the phone
6. Within 2 minutes: phone wakes, full-screen alarm shows with END + SNOOZE buttons
7. Test END (stops ring) and SNOOZE (reschedules)

---

## Architecture

```
backend/
  app/api/          Next.js API routes
  lib/              auth, db, middleware helpers
  schema.sql        Postgres schema
  scripts/          db-push.mjs

android/
  app/src/main/java/in/getdownfoundation/sahusales/
    alarm/          AlarmScheduler, AlarmReceiver, AlarmActivity, BootReceiver, ReminderSyncer
    core/           Config, Models, ApiService, RetrofitClient, SessionStore
    sync/           SyncWorker (WorkManager 15-min background sync)
    ui/             Screens: auth, dashboard, contacts, events, invoices, team, settings, debug
    MainActivity    NavHost + bottom navigation
    SahuApplication Application class, WorkManager init
```
