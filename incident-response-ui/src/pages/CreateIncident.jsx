import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createIncident } from '../api/client'
import UserSelect from '../components/UserSelect'

const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

export default function CreateIncident() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    title: '',
    description: '',
    severity: 'HIGH',
    reporterId: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.reporterId) {
      setError('Please select a reporter')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const created = await createIncident(form)
      navigate(`/incidents/${created.id}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div className="p-8 max-w-2xl">
      <h2 className="text-2xl font-bold text-gray-900 mb-1">Report New Incident</h2>
      <p className="text-sm text-gray-500 mb-8">Fill in the details to create a new incident ticket.</p>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Title *</label>
          <input
            required
            value={form.title}
            onChange={set('title')}
            placeholder="e.g. Database connection pool exhaustion"
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder:text-gray-400"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Description</label>
          <textarea
            rows={4}
            value={form.description}
            onChange={set('description')}
            placeholder="Describe the incident, its impact, and any initial observations…"
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder:text-gray-400 resize-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Severity *</label>
          <div className="flex gap-3">
            {SEVERITIES.map((sev) => (
              <label
                key={sev}
                className={`flex-1 cursor-pointer rounded-lg border-2 px-4 py-3 text-center text-sm font-medium transition ${
                  form.severity === sev
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                <input
                  type="radio"
                  name="severity"
                  value={sev}
                  checked={form.severity === sev}
                  onChange={set('severity')}
                  className="sr-only"
                />
                {sev}
              </label>
            ))}
          </div>
        </div>

        <UserSelect
          label="Reporter *"
          value={form.reporterId}
          onChange={(userId) => setForm({ ...form, reporterId: userId })}
          placeholder="Select reporter or type a name to create…"
        />

        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={submitting}
            className="px-6 py-2.5 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition"
          >
            {submitting ? 'Creating…' : 'Create Incident'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/incidents')}
            className="px-6 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
