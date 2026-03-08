import { useEffect, useState, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  getIncident, assignOwner, updateStatus,
  attachEvidence, recordRca, getAuditTrail, getSlaMetrics,
  listUsers,
} from '../api/client'
import StatusBadge from '../components/StatusBadge'
import SeverityBadge from '../components/SeverityBadge'
import UserSelect from '../components/UserSelect'
import {
  ArrowLeft, User, PlayCircle, FileText,
  ClipboardList, Clock, Shield, ChevronRight,
} from 'lucide-react'

const WORKFLOW = ['OPEN', 'INVESTIGATING', 'FIXED', 'RCA_PENDING', 'CLOSED']
const NEXT_STATUS = {
  OPEN: 'INVESTIGATING',
  INVESTIGATING: 'FIXED',
  FIXED: 'RCA_PENDING',
  RCA_PENDING: 'CLOSED',
}
const RCA_CATEGORIES = [
  'CODE_BUG', 'CONFIGURATION_ERROR', 'INFRASTRUCTURE_FAILURE',
  'DEPENDENCY_FAILURE', 'CAPACITY_ISSUE', 'SECURITY_BREACH',
  'HUMAN_ERROR', 'MONITORING_GAP', 'PROCESS_FAILURE', 'UNKNOWN',
]

export default function IncidentDetail() {
  const { id } = useParams()
  const [incident, setIncident] = useState(null)
  const [audit, setAudit] = useState([])
  const [sla, setSla] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [tab, setTab] = useState('actions')
  const [users, setUsers] = useState([])

  const [currentUserId, setCurrentUserId] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [evidenceUrl, setEvidenceUrl] = useState('')
  const [evidenceDesc, setEvidenceDesc] = useState('')
  const [rcaCategory, setRcaCategory] = useState('CODE_BUG')
  const [rcaSummary, setRcaSummary] = useState('')

  const reload = useCallback(async () => {
    setLoading(true)
    try {
      const [inc, auditData, slaData, usersData] = await Promise.all([
        getIncident(id),
        getAuditTrail(id).catch(() => []),
        getSlaMetrics(id).catch(() => null),
        listUsers().catch(() => []),
      ])
      setIncident(inc)
      setAudit(auditData)
      setSla(slaData)
      setUsers(usersData)
      setError(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { reload() }, [reload])

  const doAction = async (fn) => {
    setActionError(null)
    try {
      await fn()
      await reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  if (loading) return <div className="p-8 text-gray-400">Loading…</div>
  if (error) return <div className="p-8 text-red-600">{error}</div>
  if (!incident) return null

  const nextStatus = NEXT_STATUS[incident.status]
  const currentIdx = WORKFLOW.indexOf(incident.status)

  return (
    <div className="p-8 max-w-5xl">
      <Link to="/incidents" className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 mb-4 transition">
        <ArrowLeft size={16} /> Back to Incidents
      </Link>

      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{incident.title}</h2>
          <p className="font-mono text-xs text-gray-400 mt-1">{incident.id}</p>
        </div>
        <div className="flex gap-2">
          <SeverityBadge severity={incident.severity} />
          <StatusBadge status={incident.status} />
        </div>
      </div>

      {/* Workflow Progress */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 mb-6">
        <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Workflow Progress</h3>
        <div className="flex items-center gap-1">
          {WORKFLOW.map((step, i) => (
            <div key={step} className="flex items-center gap-1 flex-1">
              <div
                className={`flex-1 h-2 rounded-full transition-colors ${
                  i <= currentIdx ? 'bg-indigo-500' : 'bg-gray-200'
                }`}
              />
              {i < WORKFLOW.length - 1 && <ChevronRight size={14} className="text-gray-300 shrink-0" />}
            </div>
          ))}
        </div>
        <div className="flex justify-between mt-2 text-xs text-gray-500">
          {WORKFLOW.map((step, i) => (
            <span key={step} className={i <= currentIdx ? 'text-indigo-600 font-medium' : ''}>
              {step.replace('_', ' ')}
            </span>
          ))}
        </div>
      </div>

      {actionError && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{actionError}</div>
      )}

      {/* Info Grid */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        <InfoCard label="Reporter" value={resolveUserName(incident.reporterId, users)} />
        <InfoCard label="Assignee" value={incident.assigneeId ? resolveUserName(incident.assigneeId, users) : 'Unassigned'} />
        <InfoCard label="Created" value={new Date(incident.createdAt).toLocaleString()} />
        <InfoCard label="Updated" value={new Date(incident.updatedAt).toLocaleString()} />
      </div>

      {incident.description && (
        <div className="bg-white rounded-xl border border-gray-200 p-5 mb-6">
          <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Description</h3>
          <p className="text-sm text-gray-700 whitespace-pre-wrap">{incident.description}</p>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-gray-200 mb-6 gap-1">
        {[
          { key: 'actions', label: 'Actions', icon: PlayCircle },
          { key: 'sla', label: 'SLA Metrics', icon: Clock },
          { key: 'audit', label: 'Audit Trail', icon: ClipboardList },
        ].map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition ${
              tab === key
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Icon size={16} />
            {label}
          </button>
        ))}
      </div>

      {tab === 'actions' && (
        <div className="space-y-6">
          {/* Acting-as user selector */}
          <div className="bg-indigo-50 rounded-xl border border-indigo-200 p-4">
            <UserSelect
              label="Acting as (current user)"
              value={currentUserId}
              onChange={(userId) => setCurrentUserId(userId)}
              placeholder="Select your user or type a name to create…"
            />
            {!currentUserId && (
              <p className="text-xs text-indigo-500 mt-1.5">Select yourself to perform actions on this incident</p>
            )}
          </div>

          {/* Assign Owner */}
          {!incident.assigneeId && (
            <ActionCard title="Assign Owner" icon={User} description="An owner must be assigned before investigation can begin.">
              <div className="space-y-3">
                <UserSelect
                  label="Assignee"
                  value={assigneeId}
                  onChange={(userId) => setAssigneeId(userId)}
                  placeholder="Select user to assign…"
                />
                <button
                  onClick={() => {
                    if (!assigneeId) { setActionError('Please select an assignee'); return }
                    const actingUser = currentUserId || assigneeId
                    doAction(() => assignOwner(id, assigneeId, actingUser))
                  }}
                  disabled={!assigneeId}
                  className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition"
                >
                  Assign
                </button>
              </div>
            </ActionCard>
          )}

          {/* Status Transition */}
          {nextStatus && (
            <ActionCard
              title={`Transition to ${nextStatus.replace('_', ' ')}`}
              icon={PlayCircle}
              description={
                nextStatus === 'CLOSED'
                  ? 'RCA must be recorded before closing.'
                  : `Move this incident to ${nextStatus.replace('_', ' ')} state.`
              }
            >
              <button
                onClick={() => {
                  if (!currentUserId) { setActionError('Please select "Acting as" user above'); return }
                  doAction(() => updateStatus(id, nextStatus, currentUserId))
                }}
                disabled={!currentUserId}
                className="px-5 py-2.5 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition"
              >
                Transition → {nextStatus.replace('_', ' ')}
              </button>
            </ActionCard>
          )}

          {/* Attach Evidence */}
          {incident.status !== 'CLOSED' && (
            <ActionCard title="Attach Evidence" icon={FileText} description="Add logs, screenshots, or links as evidence.">
              <div className="space-y-3">
                <input
                  placeholder="URL (e.g. link to logs, dashboard)"
                  value={evidenceUrl}
                  onChange={(e) => setEvidenceUrl(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                />
                <input
                  placeholder="Description"
                  value={evidenceDesc}
                  onChange={(e) => setEvidenceDesc(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                />
                <button
                  onClick={() =>
                    doAction(() =>
                      attachEvidence(id, {
                        type: 'LINK',
                        url: evidenceUrl,
                        description: evidenceDesc,
                        addedBy: currentUserId,
                      })
                    )
                  }
                  disabled={!evidenceUrl || !currentUserId}
                  className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition"
                >
                  Attach
                </button>
              </div>
            </ActionCard>
          )}

          {/* Record RCA */}
          {(incident.status === 'RCA_PENDING' || incident.status === 'FIXED') && (
            <ActionCard title="Record Root Cause Analysis" icon={Shield} description="Document the root cause to allow closure.">
              <div className="space-y-3">
                <select
                  value={rcaCategory}
                  onChange={(e) => setRcaCategory(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
                >
                  {RCA_CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>
                  ))}
                </select>
                <textarea
                  rows={3}
                  placeholder="Root cause summary…"
                  value={rcaSummary}
                  onChange={(e) => setRcaSummary(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none"
                />
                <button
                  onClick={() => {
                    if (!currentUserId) { setActionError('Please select "Acting as" user above'); return }
                    doAction(() =>
                      recordRca(id, {
                        category: rcaCategory,
                        summary: rcaSummary,
                        details: '',
                        actionItems: [],
                        createdBy: currentUserId,
                      })
                    )
                  }}
                  disabled={!rcaSummary || !currentUserId}
                  className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition"
                >
                  Submit RCA
                </button>
              </div>
            </ActionCard>
          )}

          {incident.status === 'CLOSED' && (
            <div className="bg-green-50 border border-green-200 rounded-xl p-5 text-center">
              <p className="text-green-700 font-medium">This incident is closed.</p>
            </div>
          )}
        </div>
      )}

      {tab === 'sla' && <SlaTab sla={sla} />}
      {tab === 'audit' && <AuditTab audit={audit} />}
    </div>
  )
}

function resolveUserName(userId, users) {
  if (!userId) return '—'
  const user = users.find((u) => u.id === userId)
  return user ? user.displayName : userId.slice(0, 8) + '…'
}

function InfoCard({ label, value, mono }) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 px-5 py-4">
      <p className="text-xs text-gray-500 font-medium mb-1">{label}</p>
      <p className={`text-sm text-gray-900 ${mono ? 'font-mono' : ''} truncate`}>{value}</p>
    </div>
  )
}

function ActionCard({ title, icon: Icon, description, children }) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <div className="flex items-center gap-2 mb-1">
        <Icon size={16} className="text-indigo-600" />
        <h4 className="text-sm font-semibold text-gray-900">{title}</h4>
      </div>
      <p className="text-xs text-gray-500 mb-4">{description}</p>
      {children}
    </div>
  )
}

function SlaTab({ sla }) {
  if (!sla) return <p className="text-gray-400 text-sm">SLA data unavailable.</p>

  const formatMs = (ms) => {
    if (ms == null) return '—'
    if (ms < 1000) return `${ms}ms`
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
    if (ms < 3600000) return `${(ms / 60000).toFixed(1)}m`
    return `${(ms / 3600000).toFixed(1)}h`
  }

  return (
    <div className="grid grid-cols-2 gap-4">
      <MetricCard
        label="Acknowledge SLA"
        value={formatMs(sla.acknowledgeSlaMs)}
        breached={sla.acknowledgeSlaBreached}
      />
      <MetricCard
        label="Resolution SLA"
        value={formatMs(sla.resolutionSlaMs)}
        breached={sla.resolutionSlaBreached}
      />
      <MetricCard label="Actual Acknowledge Time" value={formatMs(sla.actualAcknowledgeMs)} />
      <MetricCard label="Actual Resolution Time" value={formatMs(sla.actualResolutionMs)} />
      <MetricCard label="Acknowledged At" value={sla.acknowledgedAt ? new Date(sla.acknowledgedAt).toLocaleString() : '—'} />
      <MetricCard label="Resolved At" value={sla.resolvedAt ? new Date(sla.resolvedAt).toLocaleString() : '—'} />
    </div>
  )
}

function MetricCard({ label, value, breached }) {
  return (
    <div className={`rounded-xl border p-5 ${breached ? 'bg-red-50 border-red-200' : 'bg-white border-gray-200'}`}>
      <p className="text-xs text-gray-500 font-medium mb-1">{label}</p>
      <p className={`text-lg font-bold ${breached ? 'text-red-600' : 'text-gray-900'}`}>{value}</p>
      {breached && <p className="text-xs text-red-500 mt-1 font-medium">SLA BREACHED</p>}
    </div>
  )
}

function AuditTab({ audit }) {
  if (!audit.length) return <p className="text-gray-400 text-sm">No audit entries yet.</p>

  return (
    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200">
            <th className="text-left px-5 py-3 font-semibold text-gray-600">Field</th>
            <th className="text-left px-5 py-3 font-semibold text-gray-600">Old Value</th>
            <th className="text-left px-5 py-3 font-semibold text-gray-600">New Value</th>
            <th className="text-left px-5 py-3 font-semibold text-gray-600">User</th>
            <th className="text-left px-5 py-3 font-semibold text-gray-600">Time</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {audit.map((entry, i) => (
            <tr key={i}>
              <td className="px-5 py-3 font-medium text-gray-900">{entry.field}</td>
              <td className="px-5 py-3 text-gray-500 font-mono text-xs">{entry.oldValue || '—'}</td>
              <td className="px-5 py-3 text-gray-900 font-mono text-xs">{entry.newValue}</td>
              <td className="px-5 py-3 text-gray-500 font-mono text-xs">{entry.userId?.slice(0, 8)}…</td>
              <td className="px-5 py-3 text-gray-400 text-xs">{new Date(entry.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
