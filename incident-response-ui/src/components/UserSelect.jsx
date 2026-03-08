import { useEffect, useState, useRef } from 'react'
import { listUsers, resolveOrCreateUser } from '../api/client'
import { UserPlus, ChevronDown } from 'lucide-react'

/**
 * Dropdown that lists all registered users.
 * If no matching user exists, typing a name and pressing Enter/Create
 * auto-creates a user with that display name (naming convention: user-001, user-002…).
 *
 * Props:
 *   value    — currently selected user ID (string)
 *   onChange — (userId: string, user: object) => void
 *   label    — optional label text
 *   placeholder — optional placeholder
 */
export default function UserSelect({ value, onChange, label, placeholder = 'Select a user…' }) {
  const [users, setUsers] = useState([])
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [creating, setCreating] = useState(false)
  const ref = useRef()

  const loadUsers = async () => {
    try {
      const data = await listUsers()
      setUsers(data)
    } catch { /* empty list on error */ }
  }

  useEffect(() => { loadUsers() }, [])

  useEffect(() => {
    const close = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const selected = users.find((u) => u.id === value)
  const filtered = users.filter((u) =>
    u.displayName.toLowerCase().includes(search.toLowerCase()) ||
    (u.email && u.email.toLowerCase().includes(search.toLowerCase()))
  )
  const exactMatch = users.some((u) => u.displayName.toLowerCase() === search.toLowerCase())

  const handleCreate = async () => {
    if (!search.trim()) return
    setCreating(true)
    try {
      const user = await resolveOrCreateUser(search.trim())
      await loadUsers()
      onChange(user.id, user)
      setSearch('')
      setOpen(false)
    } catch { /* ignore */ }
    finally { setCreating(false) }
  }

  const handleSelect = (user) => {
    onChange(user.id, user)
    setSearch('')
    setOpen(false)
  }

  return (
    <div className="relative" ref={ref}>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1.5">{label}</label>}
      <button
        type="button"
        onClick={() => { setOpen(!open); if (!open) loadUsers() }}
        className="w-full flex items-center justify-between px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white hover:border-gray-400 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
      >
        <span className={selected ? 'text-gray-900' : 'text-gray-400'}>
          {selected ? selected.displayName : placeholder}
        </span>
        <ChevronDown size={16} className="text-gray-400 shrink-0" />
      </button>

      {open && (
        <div className="absolute z-50 mt-1 w-full bg-white rounded-lg border border-gray-200 shadow-lg max-h-64 overflow-hidden">
          <div className="p-2 border-b border-gray-100">
            <input
              autoFocus
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && search.trim() && !exactMatch) {
                  e.preventDefault()
                  handleCreate()
                }
              }}
              placeholder="Search or type a name to create…"
              className="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          <div className="overflow-y-auto max-h-44">
            {filtered.map((user) => (
              <button
                key={user.id}
                type="button"
                onClick={() => handleSelect(user)}
                className={`w-full flex items-center gap-3 px-4 py-2.5 text-left hover:bg-indigo-50 transition text-sm ${
                  user.id === value ? 'bg-indigo-50 text-indigo-700' : 'text-gray-700'
                }`}
              >
                <div className="w-7 h-7 rounded-full bg-indigo-100 flex items-center justify-center text-xs font-bold text-indigo-600 shrink-0">
                  {user.displayName.charAt(0).toUpperCase()}
                </div>
                <div className="min-w-0">
                  <div className="font-medium truncate">{user.displayName}</div>
                  {user.email && <div className="text-xs text-gray-400 truncate">{user.email}</div>}
                </div>
                {user.role && (
                  <span className="ml-auto text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded shrink-0">
                    {user.role}
                  </span>
                )}
              </button>
            ))}

            {filtered.length === 0 && !search && (
              <div className="px-4 py-6 text-center text-sm text-gray-400">
                No users yet. Type a name above to create one.
              </div>
            )}
          </div>

          {search.trim() && !exactMatch && (
            <div className="border-t border-gray-100 p-2">
              <button
                type="button"
                onClick={handleCreate}
                disabled={creating}
                className="w-full flex items-center gap-2 justify-center px-4 py-2.5 text-sm font-medium text-indigo-700 bg-indigo-50 rounded-lg hover:bg-indigo-100 disabled:opacity-50 transition"
              >
                <UserPlus size={16} />
                {creating ? 'Creating…' : `Create "${search.trim()}"`}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
