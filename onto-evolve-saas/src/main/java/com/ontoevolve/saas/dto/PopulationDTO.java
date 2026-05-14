package com.ontoevolve.saas.dto;

import java.util.List;

public class PopulationDTO {
    private String conceptIri;
    private String conceptLabel;
    private int size;
    private int generation;
    private List<AssignmentDTO> assignments;

    public String getConceptIri() { return conceptIri; }
    public void setConceptIri(String conceptIri) { this.conceptIri = conceptIri; }

    public String getConceptLabel() { return conceptLabel; }
    public void setConceptLabel(String conceptLabel) { this.conceptLabel = conceptLabel; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }

    public List<AssignmentDTO> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentDTO> assignments) { this.assignments = assignments; }

    public static class AssignmentDTO {
        private String iri;
        private String decisionIri;
        private String decisionName;
        private List<String> steps;
        private double[] scoreVector;
        private String status;
        private int trials;
        private int generation;

        public String getIri() { return iri; }
        public void setIri(String iri) { this.iri = iri; }

        public String getDecisionIri() { return decisionIri; }
        public void setDecisionIri(String decisionIri) { this.decisionIri = decisionIri; }

        public String getDecisionName() { return decisionName; }
        public void setDecisionName(String decisionName) { this.decisionName = decisionName; }

        public List<String> getSteps() { return steps; }
        public void setSteps(List<String> steps) { this.steps = steps; }

        public double[] getScoreVector() { return scoreVector; }
        public void setScoreVector(double[] scoreVector) { this.scoreVector = scoreVector; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getTrials() { return trials; }
        public void setTrials(int trials) { this.trials = trials; }

        public int getGeneration() { return generation; }
        public void setGeneration(int generation) { this.generation = generation; }
    }
}
