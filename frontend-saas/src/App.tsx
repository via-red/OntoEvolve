import { Routes, Route, Navigate } from 'react-router-dom'
import DomainSelector from './pages/DomainSelector'
import DomainWorkspace from './pages/DomainWorkspace'
import AdminLayout from './pages/admin/AdminLayout'
import DomainList from './pages/admin/DomainList'
import DomainEditor from './pages/admin/DomainEditor'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<DomainSelector />} />
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<DomainList />} />
        <Route path="domains/:domainId" element={<DomainEditor />} />
      </Route>
      <Route path="/:domainId" element={<DomainWorkspace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
