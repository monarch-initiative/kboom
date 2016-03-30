package org.monarchinitiative.boom.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

/**
 * ProbabilisticGraph (PG) is a collection of (src, target) node pairs, each of which
 * is assigned a random variable representing 4 disjoint states.
 * 
 * A PG can be used either for the set of all probabilistic logical relationships
 * in an ontology, or for a subgraph.
 * 
 * @author cjm
 *
 */
public class ProbabilisticGraph {
	
	private static Logger LOG = Logger.getLogger(ProbabilisticGraph.class);


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
	 * @param edges the edges to add
	 */
	public void setEdges(List<ProbabilisticEdge> edges) {
		this.edges = edges;
	}

	/**
	 * @param badEdges the edges to remove
	 */
	public void removeEdges(Set<ProbabilisticEdge> badEdges) {
		this.edges.removeAll(badEdges);
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

	public int collapseReciprocals() {
		LOG.info("PRE-COLLAPSING RECIPROCALS:"+edges.size());
		List<ProbabilisticEdge> newEdges = new ArrayList<ProbabilisticEdge>();
		int n=0;
		for (ProbabilisticEdge e :edges) {
			OWLClass s = e.sourceClass;
			OWLClass t = e.targetClass;
			ProbabilisticEdge reciprocal = null;
			if (s.compareTo(t) == 1) {
				for (ProbabilisticEdge f :edges) {
					if (f.targetClass.equals(s) &&
							f.sourceClass.equals(t)) {
						reciprocal = f;
						break;
					}
				}
			}	
			if (reciprocal == null) {
				newEdges.add(e);
			}
			else {
				n++;
				// skip e, and make reciprocal = avg(e, reciprocal)
				LOG.warn("Merging reciprocal edges "+e+" <-> "+reciprocal);
				Map<EdgeType, Double> etm = e.getProbabilityTable().getTypeProbabilityMap();
				Map<EdgeType, Double> ftm = reciprocal.getProbabilityTable().getTypeProbabilityMap();
				
				// recall that ftm is the reciprocal, so for asymmetric relationships,
				// we use the reciprocal relationship type
				double p1 = 
						avg(ftm.get(EdgeType.SUBCLASS_OF),
								etm.get(EdgeType.SUPERCLASS_OF));
				double p2 = 
						avg(ftm.get(EdgeType.SUPERCLASS_OF),
								etm.get(EdgeType.SUBCLASS_OF));
				
				ftm.put(EdgeType.SUBCLASS_OF, p1);
				ftm.put(EdgeType.SUPERCLASS_OF, p2);
	
				ftm.put(EdgeType.EQUIVALENT_TO, 
						avg(ftm.get(EdgeType.EQUIVALENT_TO),
								etm.get(EdgeType.EQUIVALENT_TO)));
				ftm.put(EdgeType.NONE, 
						avg(ftm.get(EdgeType.NONE),
								etm.get(EdgeType.NONE)));

				LOG.warn("  MERGED: "+reciprocal);

			}
		}
		edges = newEdges;
		LOG.info("POST-COLLAPSING RECIPROCALS:"+edges.size());
		return n;
	}
	
	private double avg(double x, double y) {
		return (x+y)/2;
	}

}
