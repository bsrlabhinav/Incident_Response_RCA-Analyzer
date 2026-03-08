import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listIncidents } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import SeverityBadge from '../components/SeverityBadge'
import { RefreshCw, Search, Filter } from 'lucide-react'

const STATUSES = ['', 'OPEN', 'INVESTIGATING', 'FIXED', 'RCA_PENDING', 'CLOSED']
const SEVERITIES = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

export default function Incidents() {
  const [incidents, setIncidents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [severityFilter, setSeverityFilter] = useState('')

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listIncidents({
        status: statusFilter || undefined,
        severity: severityFilter || undefined,
      })
      setIncidents(data.content || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [statusFilter, severityFilter])

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Incidents</h2>
          <p className="text-sm text-gray-500 mt-1">Manage and track production incidents</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={load}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
          >
            <RefreshCw size={16} />
            Refresh
          </button>
          <Link
            to="/incidents/new"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 transition"
          >
            + New Incident
          </Link>
        </div>
      </div>

      <div className="flex gap-3 mb-4">
        <div className="relative">
          <Filter size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="pl-9 pr-8 py-2 text-sm border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
          >
            <option value="">All Statuses</option>
            {STATUSES.filter(Boolean).map((s) => (
              <option key={s} value={s}>{s.replace('_', ' ')}</option>
            ))}
          </select>
        </div>
        <div className="relative">
          <Filter size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="pl-9 pr-8 py-2 text-sm border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
          >
            <option value="">All Severities</option>
            {SEVERITIES.filter(Boolean).map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-6 py-3 font-semibold text-gray-600">Title</th>
              <th className="text-left px-6 py-3 font-semibold text-gray-600">Severity</th>
              <th className="text-left px-6 py-3 font-semibold text-gray-600">Status</th>
              <th className="text-left px-6 py-3 font-semibold text-gray-600">Assignee</th>
              <th className="text-left px-6 py-3 font-semibold text-gray-600">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr><td colSpan={5} className="px-6 py-12 text-center text-gray-400">Loading…</td></tr>
            ) : incidents.length === 0 ? (
              <tr><td colSpan={5} className="px-6 py-12 text-center text-gray-400">No incidents found</td></tr>
            ) : (
              incidents.map((inc) => (
                <tr key={inc.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4">
                    <Link to={`/incidents/${inc.id}`} className="text-indigo-600 font-medium hover:underline">
                      {inc.title}
                    </Link>
                    <p className="text-xs text-gray-400 mt-0.5 font-mono">{inc.id.slice(0, 8)}…</p>
                  </td>
                  <td className="px-6 py-4"><SeverityBadge severity={inc.severity} /></td>
                  <td className="px-6 py-4"><StatusBadge status={inc.status} /></td>
                  <td className="px-6 py-4 text-gray-600">
                    {inc.assigneeId ? <span className="font-mono text-xs">{inc.assigneeId.slice(0, 8)}…</span> : <span className="text-gray-400">Unassigned</span>}
                  </td>
                  <td className="px-6 py-4 text-gray-500 text-xs">
                    {new Date(inc.createdAt).toLocaleString()}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
