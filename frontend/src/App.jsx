import { useCallback, useEffect, useState } from 'react';
import { createLink, deleteLink, listLinks } from './api';
import ShortenForm from './components/ShortenForm.jsx';
import LinkTable from './components/LinkTable.jsx';

export default function App() {
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const refresh = useCallback(async () => {
    try {
      setLinks(await listLinks());
      setError('');
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Errors from creating a link bubble up to the form so it can show them.
  async function handleCreate(payload) {
    const created = await createLink(payload);
    setLinks((previous) => [created, ...previous]);
  }

  async function handleDelete(code) {
    try {
      await deleteLink(code);
      setLinks((previous) => previous.filter((link) => link.code !== code));
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <main className="container">
      <header>
        <h1>ShorTLinks</h1>
        <p className="subtitle">Shorten URLs and track every click.</p>
      </header>

      <ShortenForm onCreate={handleCreate} />

      {error && <p className="error" role="alert">{error}</p>}

      <section>
        <div className="section-header">
          <h2>Your links</h2>
          <button className="secondary" onClick={refresh}>Refresh</button>
        </div>
        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <LinkTable links={links} onDelete={handleDelete} />
        )}
      </section>
    </main>
  );
}
