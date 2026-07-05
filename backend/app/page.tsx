export default function Home() {
  return (
    <main style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>Sahu Sales Solution API</h1>
      <p>Backend is running. Use the Android app to interact with the API.</p>
      <h2>Endpoints</h2>
      <ul>
        <li>POST /api/auth/register</li>
        <li>POST /api/auth/login</li>
        <li>GET/PATCH /api/me</li>
        <li>GET/POST /api/contacts</li>
        <li>GET/PATCH/DELETE /api/contacts/[id]</li>
        <li>GET/POST /api/events</li>
        <li>PATCH/DELETE /api/events/[id]</li>
        <li>GET /api/reminders?due=1</li>
        <li>PATCH /api/reminders/[id]</li>
        <li>GET /api/activity</li>
        <li>GET/POST /api/event-tags</li>
        <li>PATCH/DELETE /api/event-tags/[id]</li>
        <li>GET/POST /api/products</li>
        <li>PATCH/DELETE /api/products/[id]</li>
        <li>GET/POST /api/invoices</li>
        <li>GET/PATCH /api/invoices/[id]</li>
        <li>GET/POST /api/team</li>
        <li>PATCH /api/team/[id]</li>
      </ul>
    </main>
  );
}
