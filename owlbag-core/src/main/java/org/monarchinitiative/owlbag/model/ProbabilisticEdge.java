package org.monarchinitiative.owlbag.model;

import org.semanticweb.owlapi.model.OWLClass;

public class ProbabilisticEdge {
	
	final OWLClass sourceClass;
	final OWLClass targetClass;
	final EdgeProbabilityTable probabilityTable;
	
	

	public ProbabilisticEdge(OWLClass sourceClass, OWLClass targetClass,
			EdgeProbabilityTable probabilityTable) {
		super();
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
		this.probabilityTable = probabilityTable;
	}

	public ProbabilisticEdge(OWLClass s, OWLClass t, double pSUB, double pSUP, double pEQ, double pNULL) {
		super();
		this.sourceClass = s;
		this.targetClass = t;
		probabilityTable = new EdgeProbabilityTable(pSUB, pSUP, pEQ, pNULL);

	}

	/**
	 * @return the sourceClass
	 */
	public OWLClass getSourceClass() {
		return sourceClass;
	}

	/**
	 * @return the targetClass
	 */
	public OWLClass getTargetClass() {
		return targetClass;
	}

	/**
	 * @return the probabilityTable
	 */
	public EdgeProbabilityTable getProbabilityTable() {
		return probabilityTable;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return sourceClass
				+ " -> " + targetClass + " Pr["
				+ probabilityTable + "]";
	}

	
	
	
	

}
