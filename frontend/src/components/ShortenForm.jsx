import { useState } from 'react';

export default function ShortenForm({ onCreate }) {
  const [url, setUrl] = useState('');
  const [alias, setAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const payload = { url: url.trim() };
      if (alias.trim()) payload.alias = alias.trim();
      if (expiresAt) payload.expiresAt = new Date(expiresAt).toISOString();
      await onCreate(payload);
      setUrl('');
      setAlias('');
      setExpiresAt('');
    } catch (e) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <label>
        Long URL
        <input
          type="url"
          required
          placeholder="https://example.com/some/long/path"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
      </label>

      <div className="row">
        <label>
          Custom alias (optional)
          <input
            type="text"
            placeholder="my-link"
            maxLength={32}
            value={alias}
            onChange={(e) => setAlias(e.target.value)}
          />
        </label>
        <label>
          Expires (optional)
          <input
            type="datetime-local"
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
          />
        </label>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Shortening…' : 'Shorten'}
      </button>
    </form>
  );
}
