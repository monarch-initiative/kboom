package org.monarchinitiative.owlbag.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * ProbabilisticGraph (PG) is a collection of (src, target) node pairs, each of which
 * is assigned a random variable representing 4 disjoint states.
 * 
 * 
 * 
 * @author cjm
 *
 */
public class ProbabilisticGraph {
	
	List<ProbabilisticEdge> edges = new ArrayList<ProbabilisticEdge>();
	Set<OWLAxiom> logicalEdges =  new HashSet<OWLAxiom>();
	
	private OWLAxiom[][] axiomIndex;
	private double[][] probabilityIndex;


	/**
	 * @return the edges
	 */
	public List<ProbabilisticEdge> getEdges() {
		return edges;
	}


	/**
	 * @param edges the edges to set
	 */
	public void setEdges(List<ProbabilisticEdge> edges) {
		this.edges = edges;
	}



	/**
	 * @return the logicalEdges
	 */
	public Set<OWLAxiom> getLogicalEdges() {
		return logicalEdges;
	}

	/**
	 * @param logicalEdges the logicalEdges to set
	 */
	public void setLogicalEdges(Set<OWLAxiom> logicalEdges) {
		this.logicalEdges = logicalEdges;
	}
	
	/**
	 * @param logicalEdges the logicalEdges to set
	 */
	public void addLogicalEdges(Set<OWLAxiom> logicalEdges) {
		this.logicalEdges.addAll(logicalEdges);
	}


	public OWLAxiom[][] getAxiomIndex() {
		return axiomIndex;
	}


	public void setAxiomIndex(OWLAxiom[][] axiomIndex) {
		this.axiomIndex = axiomIndex;
	}


	public double[][] getProbabilityIndex() {
		return probabilityIndex;
	}


	public void setProbabilityIndex(double[][] probabilityIndex) {
		this.probabilityIndex = probabilityIndex;
	}
	
	

}
