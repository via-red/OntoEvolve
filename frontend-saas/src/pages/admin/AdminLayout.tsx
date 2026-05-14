import { Outlet, Link, useLocation } from 'react-router-dom'

export default function AdminLayout() {
  const location = useLocation()
  const isActive = (path: string) => location.pathname === path

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <nav style={{ width: 200, background: '#f5f5f5', padding: 20 }}>
        <h2 style={{ fontSize: 16, marginBottom: 20 }}>管理后台</h2>
        <Link
          to="/admin"
          style={{
            display: 'block',
            padding: '8px 12px',
            borderRadius: 6,
            textDecoration: 'none',
            color: isActive('/admin') ? '#1677ff' : '#333',
            background: isActive('/admin') ? '#e6f4ff' : 'transparent',
          }}
        >
          领域列表
        </Link>
        <Link
          to="/"
          style={{ display: 'block', marginTop: 16, padding: '8px 12px', color: '#666', textDecoration: 'none', fontSize: 13 }}
        >
          ← 返回首页
        </Link>
      </nav>
      <main style={{ flex: 1, padding: 24 }}>
        <Outlet />
      </main>
    </div>
  )
}
