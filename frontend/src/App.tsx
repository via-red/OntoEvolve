import { Routes, Route, NavLink } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Events from './pages/Events';
import Interventions from './pages/Interventions';
import Graph from './pages/Graph';
import Evolution from './pages/Evolution';
import Docs from './pages/Docs';

const NAV_ITEMS = [
  { path: '/', icon: '📊', label: '工作台' },
  { path: '/events', icon: '🔍', label: '事件追溯' },
  { path: '/interventions', icon: '💡', label: '方案库' },
  { path: '/graph', icon: '🔗', label: '关系图谱' },
  { path: '/evolution', icon: '🧬', label: '进化监控' },
  { path: '/docs', icon: '📖', label: '文档中心' },
];

export default function App() {
  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1>
            <span>🧬</span>
            <span>OntoEvolve</span>
          </h1>
          <div className="subtitle">学生行为干预管理系统</div>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `nav-item ${isActive ? 'active' : ''}`
              }
            >
              <span className="icon">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div style={{ padding: '16px 20px', borderTop: '1px solid var(--border)', fontSize: 11, color: 'var(--text-muted)' }}>
          OntoEvolve v0.2.0
        </div>
      </aside>
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/events" element={<Events />} />
          <Route path="/interventions" element={<Interventions />} />
          <Route path="/graph" element={<Graph />} />
          <Route path="/evolution" element={<Evolution />} />
          <Route path="/docs" element={<Docs />} />
        </Routes>
      </main>
    </div>
  );
}
