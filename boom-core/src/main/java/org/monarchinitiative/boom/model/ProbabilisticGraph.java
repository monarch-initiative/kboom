package org.monarchinitiative.boom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;

/**
 * A probabilistic graph/ontology consists of logical axioms and probabilistic axioms.
 * 
 * Here, probabilistic axioms are limited to {@link ProbabilisticEdge}s.
 * 
 * A PG can be used either for the set of all probabilistic logical relationships
 * in an ontology, or for a subgraph.
 * 
 * @author cjm
 *
 */
public class ProbabilisticGraph {
	
	private static Logger LOG = Logger.getLogger(ProbabilisticGraph.class);


	List<ProbabilisticEdge> probabilisticEdges = new ArrayList<ProbabilisticEdge>();
	Set<OWLAxiom> logicalEdges =  new HashSet<OWLAxiom>();

	private OWLAxiom[][] axiomIndex;
	private double[][] probabilityIndex;
	private Map<OWLAxiom, Double> axiomPriorProbabilityMap = new HashMap<>();
	private Map<OWLAxiom, ProbabilisticEdge> probabilisticEdgeReplacementMap = new HashMap<>();
	private Set<ProbabilisticEdge> eliminatedEdges = new HashSet<>();

	/**
	 * Note the edge lists is mutable; if it changes, axiomIndex must be recalculated
	 * 
	 * @return the edges
	 */
	public List<ProbabilisticEdge> getProbabilisticEdges() {
		return probabilisticEdges;
	}


	/**
	 * @param edges the edges to add
	 */
	public void setProbabilisticEdges(List<ProbabilisticEdge> edges) {
		this.probabilisticEdges = edges;
	}

	/**
	 * @param badEdges the edges to remove
	 */
	public void removeEdges(Set<ProbabilisticEdge> badEdges) {
		this.probabilisticEdges.removeAll(badEdges);
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


	/**
	 * TODO: use immutable lists for edges; this index must be recalculated if edges change
	 * 
	 * @return logical axiom index keyed by edge index and type
	 */
	public OWLAxiom[][] getAxiomIndex() {
		return axiomIndex;
	}


	public void setAxiomIndex(OWLAxiom[][] axiomIndex) {
		this.axiomIndex = axiomIndex;
	}


	/**
	 * TODO: use immutable lists for edges; this index must be recalculated if edges change
	 * 
	 * @return pr index keyed by edge index and type
	 */
	public double[][] getProbabilityIndex() {
		return probabilityIndex;
	}


	public void setProbabilityIndex(double[][] probabilityIndex) {
		this.probabilityIndex = probabilityIndex;
	}
	
	
	/**
	 * As a heuristic, some candidate edges may be trimmed and replaced by a logical axiom
	 * 
     * @return the probabilisticEdgeReplacementMap
     */
    public Map<OWLAxiom, ProbabilisticEdge> getProbabilisticEdgeReplacementMap() {
        return probabilisticEdgeReplacementMap;
    }


    /**
     * @param probabilisticEdgeReplacementMap the probabilisticEdgeReplacementMap to set
     */
    public void setProbabilisticEdgeReplacementMap(
            Map<OWLAxiom, ProbabilisticEdge> probabilisticEdgeReplacementMap) {
        this.probabilisticEdgeReplacementMap = probabilisticEdgeReplacementMap;
    }

    

    /**
     * @return the eliminatedEdges
     */
    public Set<ProbabilisticEdge> getEliminatedEdges() {
        return eliminatedEdges;
    }


    /**
     * @param eliminatedEdges the eliminatedEdges to set
     */
    public void setEliminatedEdges(Set<ProbabilisticEdge> eliminatedEdges) {
        this.eliminatedEdges = eliminatedEdges;
    }


    /**
	 * @param axiomPriorProbabilityMap the axiomPriorProbabilityMap to set
	 */
	public void setAxiomPriorProbabilityMap(
			Map<OWLAxiom, Double> axiomPriorProbabilityMap) {
		this.axiomPriorProbabilityMap = axiomPriorProbabilityMap;
	}


	public Double getAxiomPriorProbability(OWLAxiom axiom) {
		if (axiomPriorProbabilityMap.containsKey(axiom))
			return axiomPriorProbabilityMap.get(axiom);
		else
			return null;
	}
	
	/**
	 * Computes the {@link ProbabilisticGraph.setProbabilityIndex()}
	 * 
	 * NOTE: if the probabilistic edge list changes, this must be recalculated
	 * 
	 * @param probabilisticGraph
	 */
	public void calculateEdgeProbabilityMatrix(OWLDataFactory df) {

		List<ProbabilisticEdge> prEdges = getProbabilisticEdges();
		int N = prEdges.size();

		// both indices are keyed by the index of the prEdge list;
		// TODO - less dumb way of doing this
		OWLAxiom[][] axiomIndex = new OWLAxiom[N][4];
		double[][] probabilityIndex = new double[N][4];
		
		 Map<OWLAxiom, Double> axiomPriorProbabilityMap = new HashMap<>();

		// initialize
		for (int ei = 0; ei < N; ei++) {
			ProbabilisticEdge e = prEdges.get(ei);
			OWLClass sc = e.getSourceClass();
			OWLClass tc = e.getTargetClass();
			Map<EdgeType, Double> tpm = e.getProbabilityTable().getTypeProbabilityMap();
			for (int j=0; j<4; j++) {
				OWLAxiom ax;
				Double pr;
				if (j==1) {
					ax = df.getOWLSubClassOfAxiom(sc, tc);
					pr =tpm.get(EdgeType.SUBCLASS_OF);
				}
				else if (j==2) {
					ax = df.getOWLSubClassOfAxiom(tc, sc);
					pr = tpm.get(EdgeType.SUPERCLASS_OF);					
				}
				else if (j == 3) {
					ax = df.getOWLEquivalentClassesAxiom(sc, tc);
					pr = tpm.get(EdgeType.EQUIVALENT_TO);					
				}
				else {
					ax = null;
					pr = 1 - (tpm.get(EdgeType.EQUIVALENT_TO) + tpm.get(EdgeType.SUBCLASS_OF)	
							+ tpm.get(EdgeType.SUPERCLASS_OF));
				}
				//LOG.info("Pr["+ei+"]["+j+"]="+pr+" // "+ax);
				probabilityIndex[ei][j] = pr;
				axiomIndex[ei][j] = ax;
				axiomPriorProbabilityMap.put(ax, pr);
				if (axiomPriorProbabilityMap.get(ax) != pr) {
					LOG.error("ERROR: "+ax+" ***** "+pr);
				}
			}
		}
		setAxiomIndex(axiomIndex);
		setProbabilityIndex(probabilityIndex);
		setAxiomPriorProbabilityMap(axiomPriorProbabilityMap);
	}

	public int collapseReciprocals() {
		LOG.info("PRE-COLLAPSING RECIPROCALS:"+probabilisticEdges.size());
		List<ProbabilisticEdge> newEdges = new ArrayList<ProbabilisticEdge>();
		int n=0;
		for (ProbabilisticEdge e :probabilisticEdges) {
			OWLClass s = e.sourceClass;
			OWLClass t = e.targetClass;
			ProbabilisticEdge reciprocal = null;
			if (s.compareTo(t) == 1) {
				for (ProbabilisticEdge f :probabilisticEdges) {
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
		probabilisticEdges = newEdges;
		LOG.info("POST-COLLAPSING RECIPROCALS:"+probabilisticEdges.size());
		return n;
	}
	
	private double avg(double x, double y) {
		return (x+y)/2;
	}

}
