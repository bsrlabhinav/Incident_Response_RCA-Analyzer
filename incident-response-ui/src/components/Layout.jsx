import { NavLink, Outlet } from 'react-router-dom'
import { AlertTriangle, BarChart3, Plus, Database, Users } from 'lucide-react'

const nav = [
  { to: '/incidents', label: 'Incidents', icon: AlertTriangle },
  { to: '/incidents/new', label: 'New Incident', icon: Plus },
  { to: '/reports', label: 'Reports', icon: BarChart3 },
  { to: '/database', label: 'Database', icon: Database },
  { to: '/users', label: 'Users', icon: Users },
]

export default function Layout() {
  return (
    <div className="flex h-screen bg-gray-50">
      <aside className="w-60 bg-gray-900 text-gray-300 flex flex-col">
        <div className="px-5 py-5 border-b border-gray-800">
          <h1 className="text-lg font-bold text-white tracking-tight">
            Incident Response
          </h1>
          <p className="text-xs text-gray-500 mt-0.5">Operations Console</p>
        </div>
        <nav className="flex-1 py-4 space-y-1 px-3">
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/incidents'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-indigo-600/20 text-indigo-400'
                    : 'hover:bg-gray-800 hover:text-white'
                }`
              }
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="px-5 py-4 border-t border-gray-800 text-xs text-gray-600">
          Shifu Reporting Engine
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
