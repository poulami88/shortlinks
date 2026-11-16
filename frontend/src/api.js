const BASE = '/api/links';

async function request(path, options = {}) {
  const response = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.detail || body.title || message;
    } catch {
      // response had no JSON body; keep the default message
    }
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}

export const createLink = (payload) =>
  request('', { method: 'POST', body: JSON.stringify(payload) });

export const listLinks = () => request('');

export const deleteLink = (code) => request(`/${code}`, { method: 'DELETE' });
