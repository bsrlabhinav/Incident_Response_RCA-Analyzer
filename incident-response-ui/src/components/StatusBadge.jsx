const colors = {
  OPEN: 'bg-blue-100 text-blue-800',
  INVESTIGATING: 'bg-amber-100 text-amber-800',
  FIXED: 'bg-emerald-100 text-emerald-800',
  RCA_PENDING: 'bg-purple-100 text-purple-800',
  CLOSED: 'bg-gray-100 text-gray-800',
}

export default function StatusBadge({ status }) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
        colors[status] || 'bg-gray-100 text-gray-800'
      }`}
    >
      {status?.replace('_', ' ')}
    </span>
  )
}
