import { useEffect, useState, useCallback } from 'react'
import { searchDatabase } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import SeverityBadge from '../components/SeverityBadge'
import { Database, RefreshCw, Search, X, ChevronDown, ChevronRight, AlertTriangle, Clock } from 'lucide-react'

const STATUSES = ['', 'OPEN', 'INVESTIGATING', 'FIXED', 'RCA_PENDING', 'CLOSED']
const SEVERITIES = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const RCA_CATEGORIES = [
  '', 'CODE_BUG', 'CONFIGURATION_ERROR', 'INFRASTRUCTURE_FAILURE',
  'DEPENDENCY_FAILURE', 'CAPACITY_ISSUE', 'SECURITY_BREACH',
  'HUMAN_ERROR', 'MONITORING_GAP', 'PROCESS_FAILURE', 'UNKNOWN',
]

export default function DatabaseExplorer() {
  const [hits, setHits] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [expanded, setExpanded] = useState({})

  const [filters, setFilters] = useState({
    status: '',
    severity: '',
    assigneeId: '',
    ackSlaBreached: '',
    resSlaBreached: '',
    rcaCategory: '',
    q: '',
  })

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {}
      if (filters.status) params.status = filters.status
      if (filters.severity) params.severity = filters.severity
      if (filters.assigneeId) params.assigneeId = filters.assigneeId
      if (filters.ackSlaBreached === 'true') params.ackSlaBreached = true
      if (filters.resSlaBreached === 'true') params.resSlaBreached = true
      if (filters.rcaCategory) params.rcaCategory = filters.rcaCategory
      if (filters.q) params.q = filters.q
      const data = await searchDatabase(params)
      setHits(data.hits || [])
      setTotal(data.total || 0)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => { load() }, [load])

  const set = (field) => (e) => setFilters((f) => ({ ...f, [field]: e.target.value }))

  const clearFilters = () => setFilters({
    status: '', severity: '', assigneeId: '',
    ackSlaBreached: '', resSlaBreached: '', rcaCategory: '', q: '',
  })

  const hasFilters = Object.values(filters).some(Boolean)

  const toggle = (id) => setExpanded((prev) => ({ ...prev, [id]: !prev[id] }))

  const formatMs = (ms) => {
    if (ms == null) return '—'
    if (ms < 1000) return `${ms}ms`
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
    if (ms < 3600000) return `${(ms / 60000).toFixed(1)}m`
    return `${(ms / 3600000).toFixed(1)}h`
  }

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Database size={24} className="text-indigo-600" />
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Database Explorer</h2>
            <p className="text-sm text-gray-500">Raw Elasticsearch documents — incidents index</p>
          </div>
        </div>
        <div className="flex gap-3 items-center">
          <span className="text-sm text-gray-500 bg-gray-100 px-3 py-1.5 rounded-lg font-mono">
            {total} document{total !== 1 ? 's' : ''}
          </span>
          <button
            onClick={load}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
          >
            <RefreshCw size={16} /> Refresh
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 mb-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Filters</h3>
          {hasFilters && (
            <button onClick={clearFilters} className="text-xs text-indigo-600 hover:underline flex items-center gap-1">
              <X size={12} /> Clear all
            </button>
          )}
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <FilterSelect label="Status" value={filters.status} onChange={set('status')} options={STATUSES} />
          <FilterSelect label="Severity" value={filters.severity} onChange={set('severity')} options={SEVERITIES} />
          <FilterSelect label="Ack SLA Breached" value={filters.ackSlaBreached} onChange={set('ackSlaBreached')}
            options={[{ v: '', l: 'Any' }, { v: 'true', l: 'Yes — Breached' }]} />
          <FilterSelect label="Res SLA Breached" value={filters.resSlaBreached} onChange={set('resSlaBreached')}
            options={[{ v: '', l: 'Any' }, { v: 'true', l: 'Yes — Breached' }]} />
          <FilterSelect label="RCA Category" value={filters.rcaCategory} onChange={set('rcaCategory')}
            options={RCA_CATEGORIES.map((c) => c || '')} />
          <div className="col-span-2 lg:col-span-2">
            <label className="block text-xs text-gray-500 mb-1">Assignee ID</label>
            <input
              value={filters.assigneeId}
              onChange={set('assigneeId')}
              placeholder="Filter by assignee UUID…"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
          <div className="col-span-2 lg:col-span-4">
            <label className="block text-xs text-gray-500 mb-1">Full-text search</label>
            <div className="relative">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                value={filters.q}
                onChange={set('q')}
                placeholder="Search title, description, RCA summary…"
                className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              />
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>
      )}

      {/* Results */}
      <div className="space-y-3">
        {loading ? (
          <div className="text-center py-16 text-gray-400">Loading…</div>
        ) : hits.length === 0 ? (
          <div className="text-center py-16 text-gray-400">No documents match the current filters</div>
        ) : (
          hits.map(({ _id, _source: doc }) => {
            const isOpen = expanded[_id]
            const ackBreach = doc?.acknowledgeSlaBreached
            const resBreach = doc?.resolutionSlaBreached
            return (
              <div
                key={_id}
                className={`bg-white rounded-xl border overflow-hidden transition-shadow ${
                  ackBreach || resBreach ? 'border-red-200' : 'border-gray-200'
                } ${isOpen ? 'shadow-md' : 'shadow-sm hover:shadow-md'}`}
              >
                {/* Row header */}
                <button
                  onClick={() => toggle(_id)}
                  className="w-full flex items-center gap-4 px-5 py-4 text-left"
                >
                  {isOpen ? <ChevronDown size={16} className="text-gray-400 shrink-0" /> : <ChevronRight size={16} className="text-gray-400 shrink-0" />}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="font-semibold text-sm text-gray-900 truncate">{doc?.title}</span>
                      {(ackBreach || resBreach) && <AlertTriangle size={14} className="text-red-500 shrink-0" />}
                    </div>
                    <span className="text-xs font-mono text-gray-400">{_id}</span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <SeverityBadge severity={doc?.severity} />
                    <StatusBadge status={doc?.status} />
                  </div>
                </button>

                {/* Expanded detail */}
                {isOpen && (
                  <div className="border-t border-gray-100 px-5 py-4">
                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-3 text-sm mb-4">
                      <Field label="Reporter" value={doc?.reporterId} mono />
                      <Field label="Assignee" value={doc?.assigneeId || '—'} mono />
                      <Field label="Created" value={doc?.createdAt ? new Date(doc.createdAt).toLocaleString() : '—'} />
                      <Field label="Updated" value={doc?.updatedAt ? new Date(doc.updatedAt).toLocaleString() : '—'} />
                      <Field label="Time Partition" value={doc?.timePartition || '—'} />
                      <Field label="RCA Category" value={doc?.rcaCategory || '—'} />
                    </div>

                    {doc?.description && (
                      <div className="mb-4 p-3 bg-gray-50 rounded-lg text-sm text-gray-700">{doc.description}</div>
                    )}

                    {/* SLA Section */}
                    <div className="border-t border-gray-100 pt-3">
                      <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                        <Clock size={12} /> SLA Details
                      </h4>
                      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                        <SlaField
                          label="Ack Threshold"
                          value={formatMs(doc?.acknowledgeSlaMs)}
                        />
                        <SlaField
                          label="Actual Ack Time"
                          value={formatMs(doc?.actualAcknowledgeMs)}
                          breached={doc?.acknowledgeSlaBreached}
                        />
                        <SlaField
                          label="Res Threshold"
                          value={formatMs(doc?.resolutionSlaMs)}
                        />
                        <SlaField
                          label="Actual Res Time"
                          value={formatMs(doc?.actualResolutionMs)}
                          breached={doc?.resolutionSlaBreached}
                        />
                      </div>
                    </div>

                    {/* Raw JSON toggle */}
                    <details className="mt-3 border-t border-gray-100 pt-3">
                      <summary className="text-xs text-indigo-600 cursor-pointer hover:underline">
                        View raw JSON
                      </summary>
                      <pre className="mt-2 p-3 bg-gray-900 text-green-400 rounded-lg text-xs overflow-x-auto max-h-64">
                        {JSON.stringify(doc, null, 2)}
                      </pre>
                    </details>
                  </div>
                )}
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}

function FilterSelect({ label, value, onChange, options }) {
  const isObjOptions = options.length > 0 && typeof options[0] === 'object'
  return (
    <div>
      <label className="block text-xs text-gray-500 mb-1">{label}</label>
      <select
        value={value}
        onChange={onChange}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
      >
        {isObjOptions
          ? options.map((o) => <option key={o.v} value={o.v}>{o.l || o.v || `All ${label}s`}</option>)
          : options.map((o) => <option key={o} value={o}>{o ? o.replace(/_/g, ' ') : `All ${label}s`}</option>)
        }
      </select>
    </div>
  )
}

function Field({ label, value, mono }) {
  return (
    <div>
      <span className="text-xs text-gray-500">{label}</span>
      <p className={`text-sm text-gray-900 truncate ${mono ? 'font-mono' : ''}`}>{value}</p>
    </div>
  )
}

function SlaField({ label, value, breached }) {
  return (
    <div className={`rounded-lg px-3 py-2 ${breached ? 'bg-red-50 border border-red-200' : 'bg-gray-50'}`}>
      <span className="text-xs text-gray-500">{label}</span>
      <p className={`text-sm font-bold ${breached ? 'text-red-600' : 'text-gray-900'}`}>
        {value}
        {breached && <span className="text-xs font-normal ml-1">BREACHED</span>}
      </p>
    </div>
  )
}
