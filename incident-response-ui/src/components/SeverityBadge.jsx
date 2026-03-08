const colors = {
  CRITICAL: 'bg-red-100 text-red-800 ring-red-200',
  HIGH: 'bg-orange-100 text-orange-800 ring-orange-200',
  MEDIUM: 'bg-yellow-100 text-yellow-800 ring-yellow-200',
  LOW: 'bg-green-100 text-green-800 ring-green-200',
}

export default function SeverityBadge({ severity }) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ring-1 ring-inset ${
        colors[severity] || 'bg-gray-100 text-gray-600'
      }`}
    >
      {severity}
    </span>
  )
}
