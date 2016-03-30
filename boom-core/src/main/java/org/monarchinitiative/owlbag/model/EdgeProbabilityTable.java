package org.monarchinitiative.owlbag.model;

import java.util.HashMap;
import java.util.Map;

public class EdgeProbabilityTable {

	private Map<EdgeType, Double> typeProbabilityMap;

	public EdgeProbabilityTable(double pSUB, double pSUP, double pEQ, double pNULL) {
		super();
		setTypeProbabilityMap(new HashMap<EdgeType, Double>());
		getTypeProbabilityMap().put(EdgeType.SUBCLASS_OF, pSUB);
		getTypeProbabilityMap().put(EdgeType.SUPERCLASS_OF, pSUP);
		getTypeProbabilityMap().put(EdgeType.EQUIVALENT_TO, pEQ);
		getTypeProbabilityMap().put(EdgeType.NONE, pNULL);
	}

	public Map<EdgeType, Double> getTypeProbabilityMap() {
		return typeProbabilityMap;
	}

	public void setTypeProbabilityMap(Map<EdgeType, Double> typeProbabilityMap) {
		this.typeProbabilityMap = typeProbabilityMap;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Map<EdgeType, Double> tpm = getTypeProbabilityMap();
		return 
				" Sub: " + tpm.get(EdgeType.SUBCLASS_OF) +
				" Sup: " + tpm.get(EdgeType.SUPERCLASS_OF) +
				" Eqv: " + tpm.get(EdgeType.EQUIVALENT_TO) +
				" N/a: " + tpm.get(EdgeType.NONE) +
			 "]";
	}
	
	
	
	
}
