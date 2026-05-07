import { Routes, Route, NavLink, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Students from './pages/Students';
import Ontology from './pages/Ontology';
import Events from './pages/Events';
import Evolution from './pages/Evolution';
import Metrics from './pages/Metrics';
import HowItWorks from './pages/HowItWorks';

const NAV_ITEMS = [
  { path: '/', icon: '📊', label: '仪表盘' },
  { path: '/how-it-works', icon: '📖', label: '原理说明' },
  { path: '/students', icon: '👨‍🎓', label: '学生数据' },
  { path: '/ontology', icon: '🌳', label: '本体视图' },
  { path: '/events', icon: '⚡', label: '事件处理' },
  { path: '/evolution', icon: '🧬', label: '进化引擎' },
  { path: '/metrics', icon: '📈', label: '评估指标' },
];

export default function App() {
  const location = useLocation();

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1>
            <span>🧬</span>
            <span>OntoEvolve</span>
          </h1>
          <div className="subtitle">教育决策进化系统</div>
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
          OntoEvolve v0.1.0
        </div>
      </aside>
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/how-it-works" element={<HowItWorks />} />
          <Route path="/students" element={<Students />} />
          <Route path="/ontology" element={<Ontology />} />
          <Route path="/events" element={<Events />} />
          <Route path="/evolution" element={<Evolution />} />
          <Route path="/metrics" element={<Metrics />} />
        </Routes>
      </main>
    </div>
  );
}
