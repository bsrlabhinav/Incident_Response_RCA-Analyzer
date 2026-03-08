const BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.detail || body.message || `Request failed: ${res.status}`);
  }
  return res.json();
}

// ── Incidents ────────────────────────────────────────────
export function listIncidents({ status, severity, page = 0, size = 50 } = {}) {
  const params = new URLSearchParams({ page, size });
  if (status) params.set('status', status);
  if (severity) params.set('severity', severity);
  return request(`/incidents?${params}`);
}

export function getIncident(id) {
  return request(`/incidents/${id}`);
}

export function createIncident(data) {
  return request('/incidents', { method: 'POST', body: JSON.stringify(data) });
}

export function assignOwner(incidentId, assigneeId, userId) {
  return request(`/incidents/${incidentId}/owner`, {
    method: 'PUT',
    body: JSON.stringify({ assigneeId, userId }),
  });
}

export function updateStatus(incidentId, status, userId) {
  return request(`/incidents/${incidentId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status, userId }),
  });
}

// ── Evidence ─────────────────────────────────────────────
export function attachEvidence(incidentId, data) {
  return request(`/incidents/${incidentId}/evidence`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// ── RCA ──────────────────────────────────────────────────
export function recordRca(incidentId, data) {
  return request(`/incidents/${incidentId}/rca`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// ── Audit ────────────────────────────────────────────────
export function getAuditTrail(incidentId) {
  return request(`/incidents/${incidentId}/audit`);
}

// ── SLA ──────────────────────────────────────────────────
export function getSlaMetrics(incidentId) {
  return request(`/incidents/${incidentId}/sla`);
}

// ── Reports (Shifu) ──────────────────────────────────────
export function queryReport(shifuRequest) {
  return request('/reports/query', {
    method: 'POST',
    body: JSON.stringify(shifuRequest),
  });
}

export function listReportDefinitions() {
  return request('/reports/definitions');
}

// ── Users ────────────────────────────────────────────────
export function listUsers() {
  return request('/users');
}

export function createUser(data) {
  return request('/users', { method: 'POST', body: JSON.stringify(data) });
}

export function getUser(id) {
  return request(`/users/${id}`);
}

export function resolveOrCreateUser(nameOrId) {
  return request('/users/resolve', {
    method: 'POST',
    body: JSON.stringify({ nameOrId }),
  });
}

// ── Database Explorer ────────────────────────────────────
export function searchDatabase(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') params.set(k, v);
  });
  return request(`/database/incidents?${params}`);
}

export function listIndices() {
  return request('/database/indices');
}
