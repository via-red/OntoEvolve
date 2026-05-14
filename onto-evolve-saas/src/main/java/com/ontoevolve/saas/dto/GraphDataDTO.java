package com.ontoevolve.saas.dto;

import java.util.List;
import java.util.Map;

public class GraphDataDTO {
    private List<GraphNodeDTO> nodes;
    private List<GraphEdgeDTO> edges;

    public List<GraphNodeDTO> getNodes() { return nodes; }
    public void setNodes(List<GraphNodeDTO> nodes) { this.nodes = nodes; }

    public List<GraphEdgeDTO> getEdges() { return edges; }
    public void setEdges(List<GraphEdgeDTO> edges) { this.edges = edges; }

    public static class GraphNodeDTO {
        private String id;
        private String label;
        private String conceptIri;
        private String group;
        private Map<String, Object> properties;
        private double[] scoreVector;
        private int size;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getConceptIri() { return conceptIri; }
        public void setConceptIri(String conceptIri) { this.conceptIri = conceptIri; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }

        public double[] getScoreVector() { return scoreVector; }
        public void setScoreVector(double[] scoreVector) { this.scoreVector = scoreVector; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    public static class GraphEdgeDTO {
        private String source;
        private String target;
        private String label;
        private String type;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
