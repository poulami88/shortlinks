import { useState } from 'react';

function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString() : '—';
}

export default function LinkTable({ links, onDelete }) {
  const [copiedCode, setCopiedCode] = useState('');

  async function copy(link) {
    try {
      await navigator.clipboard.writeText(link.shortUrl);
      setCopiedCode(link.code);
      setTimeout(() => setCopiedCode(''), 1500);
    } catch {
      window.prompt('Copy this link:', link.shortUrl);
    }
  }

  if (links.length === 0) {
    return <p className="muted">No links yet. Create your first one above.</p>;
  }

  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Short link</th>
            <th>Original URL</th>
            <th>Clicks</th>
            <th>Created</th>
            <th>Expires</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {links.map((link) => {
            const expired = link.expiresAt && new Date(link.expiresAt) < new Date();
            return (
              <tr key={link.code}>
                <td>
                  <a href={link.shortUrl} target="_blank" rel="noreferrer">
                    {link.shortUrl}
                  </a>
                </td>
                <td className="truncate" title={link.originalUrl}>{link.originalUrl}</td>
                <td>{link.clickCount}</td>
                <td>{formatDate(link.createdAt)}</td>
                <td>
                  {formatDate(link.expiresAt)}
                  {expired && <span className="badge">Expired</span>}
                </td>
                <td className="actions">
                  <button className="secondary" onClick={() => copy(link)}>
                    {copiedCode === link.code ? 'Copied!' : 'Copy'}
                  </button>
                  <button className="danger" onClick={() => onDelete(link.code)}>
                    Delete
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
