import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Incidents from './pages/Incidents'
import CreateIncident from './pages/CreateIncident'
import IncidentDetail from './pages/IncidentDetail'
import Reports from './pages/Reports'
import DatabaseExplorer from './pages/DatabaseExplorer'
import Users from './pages/Users'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/incidents" replace />} />
        <Route path="/incidents" element={<Incidents />} />
        <Route path="/incidents/new" element={<CreateIncident />} />
        <Route path="/incidents/:id" element={<IncidentDetail />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/database" element={<DatabaseExplorer />} />
        <Route path="/users" element={<Users />} />
      </Route>
    </Routes>
  )
}
