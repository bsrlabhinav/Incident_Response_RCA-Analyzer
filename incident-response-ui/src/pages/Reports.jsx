import { useEffect, useState } from 'react'
import { queryReport } from '../api/client'
import { BarChart3, CheckCircle, AlertTriangle, Users, RefreshCw, Loader } from 'lucide-react'

function timeRange() {
  return {
    from: new Date(Date.now() - 30 * 86400000).toISOString(),
    to: new Date(Date.now() + 86400000).toISOString(),
  }
}

const REPORT_CONFIGS = {
  statusBreakdown: {
    title: 'Cases by Status',
    description: 'Distribution of all incidents by their current status',
    icon: BarChart3,
    request: {
      reportType: 'incident_summary',
      timeRange: timeRange(),
      dimensions: ['status'],
      measurements: [{ name: 'count', type: 'VALUE_COUNT', field: 'incident_count' }],
    },
  },
  totalClosed: {
    title: 'Total Cases Closed',
    description: 'Number of incidents that have been fully resolved and closed',
    icon: CheckCircle,
    request: {
      reportType: 'incident_summary',
      timeRange: timeRange(),
      dimensions: [],
      measurements: [{ name: 'count', type: 'VALUE_COUNT', field: 'incident_count' }],
      filters: { status: ['CLOSED'] },
    },
  },
  slaBreach: {
    title: 'SLA Breach Report',
    description: 'Incidents that exceeded their SLA thresholds, broken down by severity',
    icon: AlertTriangle,
    request: {
      reportType: 'sla_breach',
      timeRange: timeRange(),
      dimensions: ['severity'],
      measurements: [
        { name: 'breach_count', type: 'VALUE_COUNT', field: 'breach_count' },
        { name: 'avg_resolution_ms', type: 'AVG', field: 'avg_resolution_ms' },
      ],
    },
  },
  byAssignee: {
    title: 'Cases Resolved by User',
    description: 'Closed incidents grouped by the assigned resolver',
    icon: Users,
    request: {
      reportType: 'incident_summary',
      timeRange: timeRange(),
      dimensions: ['assignee'],
      measurements: [{ name: 'resolved', type: 'VALUE_COUNT', field: 'incident_count' }],
      filters: { status: ['CLOSED'] },
    },
  },
}

export default function Reports() {
  const [results, setResults] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadAll = async () => {
    setLoading(true)
    setError(null)
    const out = {}
    try {
      const entries = Object.entries(REPORT_CONFIGS)
      const responses = await Promise.allSettled(
        entries.map(([, cfg]) => queryReport(cfg.request))
      )
      entries.forEach(([key], i) => {
        const r = responses[i]
        out[key] = r.status === 'fulfilled' ? r.value : { error: r.reason?.message }
      })
      setResults(out)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadAll() }, [])

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Reports</h2>
          <p className="text-sm text-gray-500 mt-1">Shifu Reporting Engine — aggregated incident analytics</p>
        </div>
        <button
          onClick={loadAll}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition"
        >
          {loading ? <Loader size={16} className="animate-spin" /> : <RefreshCw size={16} />}
          Refresh
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <StatusBreakdownCard
          config={REPORT_CONFIGS.statusBreakdown}
          data={results.statusBreakdown}
          loading={loading}
        />
        <TotalClosedCard
          config={REPORT_CONFIGS.totalClosed}
          data={results.totalClosed}
          loading={loading}
        />
        <SlaBreachCard
          config={REPORT_CONFIGS.slaBreach}
          data={results.slaBreach}
          loading={loading}
        />
        <ByAssigneeCard
          config={REPORT_CONFIGS.byAssignee}
          data={results.byAssignee}
          loading={loading}
        />
      </div>
    </div>
  )
}

function CardShell({ config, loading, children }) {
  const Icon = config.icon
  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-100">
        <div className="flex items-center gap-2">
          <Icon size={18} className="text-indigo-600" />
          <h3 className="text-sm font-semibold text-gray-900">{config.title}</h3>
        </div>
        <p className="text-xs text-gray-500 mt-0.5">{config.description}</p>
      </div>
      <div className="px-6 py-5">
        {loading ? (
          <div className="flex items-center justify-center py-8 text-gray-400">
            <Loader size={20} className="animate-spin" />
          </div>
        ) : children}
      </div>
    </div>
  )
}

const STATUS_COLORS = {
  OPEN: 'bg-blue-500',
  INVESTIGATING: 'bg-amber-500',
  FIXED: 'bg-emerald-500',
  RCA_PENDING: 'bg-purple-500',
  CLOSED: 'bg-gray-500',
}

function StatusBreakdownCard({ config, data, loading }) {
  if (data?.error) return <CardShell config={config} loading={false}><ErrorMsg msg={data.error} /></CardShell>
  const buckets = data?.buckets || []
  const total = data?.totalHits || 0
  return (
    <CardShell config={config} loading={loading}>
      <div className="space-y-3">
        {buckets.map((b) => {
          const status = b.dimensions?.status
          const count = b.docCount
          const pct = total > 0 ? (count / total) * 100 : 0
          return (
            <div key={status}>
              <div className="flex justify-between text-sm mb-1">
                <span className="font-medium text-gray-700">{status?.replace('_', ' ')}</span>
                <span className="text-gray-500">{count} ({pct.toFixed(0)}%)</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2.5">
                <div
                  className={`h-2.5 rounded-full transition-all ${STATUS_COLORS[status] || 'bg-gray-400'}`}
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          )
        })}
        {buckets.length === 0 && <p className="text-sm text-gray-400">No data</p>}
      </div>
      <div className="mt-4 pt-3 border-t border-gray-100 text-right">
        <span className="text-2xl font-bold text-gray-900">{total}</span>
        <span className="text-sm text-gray-500 ml-1">total incidents</span>
      </div>
    </CardShell>
  )
}

function TotalClosedCard({ config, data, loading }) {
  if (data?.error) return <CardShell config={config} loading={false}><ErrorMsg msg={data.error} /></CardShell>
  const total = data?.totalHits ?? 0
  return (
    <CardShell config={config} loading={loading}>
      <div className="text-center py-4">
        <p className="text-5xl font-bold text-gray-900">{total}</p>
        <p className="text-sm text-gray-500 mt-2">incidents closed in the last 30 days</p>
      </div>
    </CardShell>
  )
}

const SEVERITY_COLORS = {
  CRITICAL: 'text-red-600 bg-red-50',
  HIGH: 'text-orange-600 bg-orange-50',
  MEDIUM: 'text-yellow-700 bg-yellow-50',
  LOW: 'text-green-600 bg-green-50',
}

function SlaBreachCard({ config, data, loading }) {
  if (data?.error) return <CardShell config={config} loading={false}><ErrorMsg msg={data.error} /></CardShell>
  const buckets = data?.buckets || []
  const total = data?.totalHits ?? 0

  const formatMs = (ms) => {
    if (ms == null || isNaN(ms)) return '—'
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
    if (ms < 3600000) return `${(ms / 60000).toFixed(1)}m`
    return `${(ms / 3600000).toFixed(1)}h`
  }

  return (
    <CardShell config={config} loading={loading}>
      {buckets.length > 0 ? (
        <div className="space-y-2">
          {buckets.map((b) => {
            const severity = b.dimensions?.severity
            const count = b.docCount
            const avgRes = b.measurements?.avg_resolution_ms
            return (
              <div
                key={severity}
                className={`flex items-center justify-between rounded-lg px-4 py-3 ${SEVERITY_COLORS[severity] || 'bg-gray-50'}`}
              >
                <div>
                  <span className="text-sm font-semibold">{severity}</span>
                  <span className="text-xs ml-2 opacity-70">avg resolution: {formatMs(avgRes)}</span>
                </div>
                <span className="text-lg font-bold">{count}</span>
              </div>
            )
          })}
        </div>
      ) : (
        <p className="text-sm text-gray-400 text-center py-4">No SLA breaches detected</p>
      )}
      <div className="mt-4 pt-3 border-t border-gray-100 text-right">
        <span className="text-2xl font-bold text-red-600">{total}</span>
        <span className="text-sm text-gray-500 ml-1">total breaches</span>
      </div>
    </CardShell>
  )
}

function ByAssigneeCard({ config, data, loading }) {
  if (data?.error) return <CardShell config={config} loading={false}><ErrorMsg msg={data.error} /></CardShell>
  const buckets = data?.buckets || []
  const total = data?.totalHits ?? 0
  return (
    <CardShell config={config} loading={loading}>
      {buckets.length > 0 ? (
        <div className="space-y-2">
          {buckets.map((b, i) => {
            const assignee = b.dimensions?.assignee
            const count = b.docCount
            return (
              <div key={assignee} className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-xs font-bold">
                    {String.fromCharCode(65 + i)}
                  </div>
                  <span className="text-sm font-mono text-gray-700">{assignee?.slice(0, 12)}…</span>
                </div>
                <div className="text-right">
                  <span className="text-lg font-bold text-gray-900">{count}</span>
                  <span className="text-xs text-gray-500 ml-1">closed</span>
                </div>
              </div>
            )
          })}
        </div>
      ) : (
        <p className="text-sm text-gray-400 text-center py-4">No closed incidents</p>
      )}
      <div className="mt-4 pt-3 border-t border-gray-100 text-right">
        <span className="text-2xl font-bold text-gray-900">{total}</span>
        <span className="text-sm text-gray-500 ml-1">total resolved</span>
      </div>
    </CardShell>
  )
}

function ErrorMsg({ msg }) {
  return <p className="text-sm text-red-500 py-4">{msg}</p>
}
