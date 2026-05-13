import { useEffect, useState, useRef, useCallback } from 'react';
import { api } from '../api';
import type { OntologyNode, GraphData, GraphNode } from '../types';

const NODE_COLORS: Record<string, string> = {
  ActionEvent: '#B66A50',
  ActionType: '#395141',
  Assignment: '#4C6153',
  Intervention: '#C48A5A',
  Execution: '#708070',
  Evaluation: '#AC7C5D',
  EvolTrace: '#818cf8',
};

const NODE_LABELS: Record<string, string> = {
  ActionEvent: '事件',
  ActionType: '概念',
  Assignment: '方案',
  Intervention: '干预',
  Execution: '执行',
  Evaluation: '评价',
  EvolTrace: '进化',
};

const EDGE_LABELS: Record<string, string> = {
  CLASSIFIED_AS: '分类为',
  FOR_CONCEPT: '属于',
  DECIDES: '决策',
  HAS_PARENT: '源自',
  EXECUTES: '执行',
  EVALUATES: '评价',
  DERIVED_FROM: '派生',
};

export default function Graph() {
  const [ontology, setOntology] = useState<OntologyNode[]>([]);
  const [selectedConcept, setSelectedConcept] = useState<string | null>(null);
  const [conceptLabel, setConceptLabel] = useState('');
  const [graphData, setGraphData] = useState<GraphData | null>(null);
  const [graphLoading, setGraphLoading] = useState(false);
  const [error, setError] = useState('');

  // Detail popup
  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);

  useEffect(() => {
    api.getOntology().then(setOntology).catch(() => {});
  }, []);

  const selectConcept = async (iri: string, label: string) => {
    setSelectedConcept(iri);
    setConceptLabel(label);
    setSelectedNode(null);
    setGraphLoading(true);
    setError('');
    try {
      const data = await api.getGraphData(iri);
      setGraphData(data);
    } catch {
      setError('无法加载关系图谱数据');
      setGraphData(null);
    }
    setGraphLoading(false);
  };

  // Find a node label for display
  const findNodeLabel = useCallback((iri: string) => {
    const parts = iri.split('#');
    return parts.length > 1 ? parts[1] : iri;
  }, []);

  return (
    <div>
      <div className="page-header">
        <h2>关系图谱</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 4 }}>本体概念层次 + 实例数据关系可视化</p>
      </div>

      <div style={{ display: 'flex', gap: 24 }}>
        {/* Left: Ontology Tree */}
        <div style={{ width: 300, flexShrink: 0 }}>
          <div className="card" style={{ position: 'sticky', top: 24, maxHeight: 'calc(100vh - 140px)', overflowY: 'auto' }}>
            <div className="card-title" style={{ marginBottom: 12, fontSize: 15 }}>本体概念层次</div>
            {ontology.length === 0 ? (
              <div style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)', fontSize: 13 }}>加载中...</div>
            ) : (
              ontology.map(root => (
                <OntologyTreeItem
                  key={root.iri}
                  node={root}
                  depth={0}
                  selectedIri={selectedConcept}
                  onSelect={selectConcept}
                />
              ))
            )}
          </div>
        </div>

        {/* Right: Graph + Detail */}
        <div style={{ flex: 1, minWidth: 0 }}>
          {!selectedConcept ? (
            <div className="card" style={{ textAlign: 'center', padding: 60, color: 'var(--text-muted)' }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>🔗</div>
              <p>请从左侧本体树中选择一个概念</p>
              <p style={{ fontSize: 13, marginTop: 8 }}>选中后将展示该概念下的实例关系图</p>
            </div>
          ) : graphLoading ? (
            <div className="card" style={{ textAlign: 'center', padding: 60 }}>
              <div className="loading">加载图谱数据...</div>
            </div>
          ) : error ? (
            <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--danger)' }}>{error}</div>
          ) : graphData && (graphData.nodes.length > 0 || graphData.edges.length > 0) ? (
            <div>
              <div className="card mb-4">
                <div className="card-header" style={{ marginBottom: 0 }}>
                  <div className="card-title">实例关系图 — {graphData.conceptLabel || conceptLabel}</div>
                  <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                    {graphData.nodes.length} 节点 · {graphData.edges.length} 条边
                  </span>
                </div>
              </div>
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <ForceGraph
                  nodes={graphData.nodes}
                  edges={graphData.edges}
                  onNodeClick={setSelectedNode}
                  selectedNodeId={selectedNode?.id}
                />
              </div>
              {/* Legend */}
              <div className="card mt-4">
                <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                  {Object.entries(NODE_COLORS).map(([type, color]) => (
                    <div key={type} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
                      <span style={{ width: 12, height: 12, borderRadius: '50%', background: color, flexShrink: 0 }} />
                      <span style={{ color: 'var(--text-secondary)' }}>{NODE_LABELS[type] || type}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: 60, color: 'var(--text-muted)' }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>📭</div>
              <p>该概念下暂无实例数据</p>
              <p style={{ fontSize: 13, marginTop: 8 }}>提交相关行为事件后，实例数据将出现在此处</p>
            </div>
          )}

          {/* Node Detail Popup */}
          {selectedNode && (
            <div className="card mt-4">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <div className="card-title" style={{ fontSize: 15 }}>节点详情</div>
                <button className="btn btn-sm" onClick={() => setSelectedNode(null)} style={{ fontSize: 11, padding: '2px 8px' }}>✕</button>
              </div>
              <div style={{ fontSize: 13 }}>
                <div style={{ marginBottom: 8 }}>
                  <span className="badge" style={{ background: NODE_COLORS[selectedNode.type] || '#999', color: '#fff', marginRight: 8 }}>{NODE_LABELS[selectedNode.type] || selectedNode.type}</span>
                  <strong>{selectedNode.label}</strong>
                </div>
                {selectedNode.properties && Object.keys(selectedNode.properties).length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    {Object.entries(selectedNode.properties).map(([k, v]) => (
                      <div key={k} style={{ display: 'flex', gap: 8, fontSize: 12 }}>
                        <span style={{ color: 'var(--text-muted)', minWidth: 80 }}>{k}</span>
                        <span style={{ color: 'var(--text-secondary)' }}>{v}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* Ontology Tree Item */
function OntologyTreeItem({ node, depth, selectedIri, onSelect }: {
  node: OntologyNode;
  depth: number;
  selectedIri: string | null;
  onSelect: (iri: string, label: string) => void;
}) {
  const [expanded, setExpanded] = useState(depth < 2);
  const hasChildren = node.children && node.children.length > 0;
  const isSelected = selectedIri === node.iri;
  const catColor = node.category === 'Behavioral' ? '#B66A50' :
    node.category === 'Academic' ? '#395141' : '#708070';

  return (
    <div style={{ marginLeft: depth * 16 }}>
      <div
        onClick={() => {
          if (hasChildren) setExpanded(!expanded);
          onSelect(node.iri, node.label);
        }}
        style={{
          display: 'flex', alignItems: 'center', gap: 6, padding: '5px 8px',
          borderRadius: 6, cursor: 'pointer', fontSize: 13, marginBottom: 1,
          background: isSelected ? 'rgba(57,81,65,0.08)' : 'transparent',
          fontWeight: isSelected ? 600 : (depth < 2 ? 600 : 400),
        }}>
        {hasChildren ? <span style={{ fontSize: 9, width: 12 }}>{expanded ? '▼' : '▶'}</span> : <span style={{ width: 12 }} />}
        <span style={{ width: 8, height: 8, borderRadius: '50%', background: catColor, flexShrink: 0 }} />
        <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{node.label}</span>
        <span style={{ color: 'var(--text-muted)', fontSize: 10, flexShrink: 0 }}>
          {node.assignmentCount || 0}/{node.eventCount || 0}
        </span>
      </div>
      {expanded && hasChildren && node.children.map(child => (
        <OntologyTreeItem key={child.iri} node={child} depth={depth + 1} selectedIri={selectedIri} onSelect={onSelect} />
      ))}
    </div>
  );
}

/* Simple Force-Directed Graph using SVG */
function ForceGraph({ nodes, edges, onNodeClick, selectedNodeId }: {
  nodes: GraphNode[];
  edges: { source: string; target: string; type: string }[];
  onNodeClick: (n: GraphNode) => void;
  selectedNodeId?: string;
}) {
  const svgRef = useRef<SVGSVGElement>(null);
  const animRef = useRef<number>(0);
  const positionsRef = useRef<Map<string, { x: number; y: number; vx: number; vy: number }>>(new Map());

  const w = 800;
  const h = 500;

  const [tick, setTick] = useState(0);

  useEffect(() => {
    // Initialize positions in a circle
    const pos = new Map<string, { x: number; y: number; vx: number; vy: number }>();
    const cx = w / 2;
    const cy = h / 2;
    const r = Math.min(w, h) * 0.35;
    nodes.forEach((n, i) => {
      const angle = (2 * Math.PI * i) / nodes.length - Math.PI / 2;
      pos.set(n.id, {
        x: cx + r * Math.cos(angle),
        y: cy + r * Math.sin(angle),
        vx: 0, vy: 0,
      });
    });
    positionsRef.current = pos;

    // Simple force simulation
    const idSet = new Set(nodes.map(n => n.id));
    const edgePairs = edges
      .filter(e => idSet.has(e.source) && idSet.has(e.target))
      .map(e => ({ source: e.source, target: e.target }));

    let frame = 0;
    const maxFrames = 150;

    const simulate = () => {
      const positions = positionsRef.current;
      if (frame >= maxFrames) return;

      // Forces
      const alpha = 1 - frame / maxFrames;
      const repulsion = 3000 * alpha;
      const attraction = 0.005 * alpha;
      const centering = 0.01 * alpha;
      const damping = 0.6;

      for (const [id, p] of positions) {
        // Repulsion between all pairs
        for (const [, p2] of positions) {
          if (p === p2) continue;
          let dx = p.x - p2.x;
          let dy = p.y - p2.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          const force = repulsion / (dist * dist);
          p.vx += (dx / dist) * force;
          p.vy += (dy / dist) * force;
        }
        // Centering toward middle
        p.vx += (cx - p.x) * centering;
        p.vy += (cy - p.y) * centering;
      }

      // Attraction along edges
      for (const e of edgePairs) {
        const s = positions.get(e.source);
        const t = positions.get(e.target);
        if (!s || !t) continue;
        const dx = t.x - s.x;
        const dy = t.y - s.y;
        const dist = Math.sqrt(dx * dx + dy * dy) || 1;
        const force = dist * attraction;
        s.vx += (dx / dist) * force;
        s.vy += (dy / dist) * force;
        t.vx -= (dx / dist) * force;
        t.vy -= (dy / dist) * force;
      }

      // Apply velocities with damping
      for (const [, p] of positions) {
        p.vx *= damping;
        p.vy *= damping;
        p.x += p.vx;
        p.y += p.vy;
        // Clamp to bounds
        p.x = Math.max(40, Math.min(w - 40, p.x));
        p.y = Math.max(40, Math.min(h - 40, p.y));
      }

      frame++;
      setTick(frame);
      animRef.current = requestAnimationFrame(simulate);
    };

    animRef.current = requestAnimationFrame(simulate);
    return () => cancelAnimationFrame(animRef.current);
  }, [nodes, edges, w, h]);

  const positions = positionsRef.current;

  return (
    <svg ref={svgRef} width="100%" height={h} style={{ background: 'var(--bg)', display: 'block' }}>
      {/* Edges */}
      {edges.map((e, i) => {
        const s = positions.get(e.source);
        const t = positions.get(e.target);
        if (!s || !t) return null;
        const mx = (s.x + t.x) / 2;
        const my = (s.y + t.y) / 2;
        return (
          <g key={`e-${i}`}>
            <line x1={s.x} y1={s.y} x2={t.x} y2={t.y}
              stroke="#D8D9D2" strokeWidth={1} opacity={0.7} />
            <text x={mx} y={my} textAnchor="middle" fontSize={9} fill="var(--text-muted)"
              style={{ pointerEvents: 'none' }}>
              {EDGE_LABELS[e.type] || e.type}
            </text>
          </g>
        );
      })}

      {/* Nodes */}
      {nodes.map((n) => {
        const p = positions.get(n.id);
        if (!p) return null;
        const color = NODE_COLORS[n.type] || '#999';
        const isSelected = selectedNodeId === n.id;
        const label = n.label.length > 16 ? n.label.slice(0, 16) + '..' : n.label;
        return (
          <g key={n.id} onClick={() => onNodeClick(n)} style={{ cursor: 'pointer' }}>
            <circle cx={p.x} cy={p.y} r={isSelected ? 22 : 16}
              fill={color} fillOpacity={0.15} stroke={color}
              strokeWidth={isSelected ? 3 : 2} />
            <circle cx={p.x} cy={p.y} r={6} fill={color} />
            <text x={p.x} y={p.y + 30} textAnchor="middle" fontSize={10}
              fill="var(--text-secondary)" style={{ pointerEvents: 'none' }}>
              {label}
            </text>
            <text x={p.x} y={p.y + 42} textAnchor="middle" fontSize={9}
              fill="var(--text-muted)" style={{ pointerEvents: 'none' }}>
              {NODE_LABELS[n.type] || n.type}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
