package org.monarchinitiative.owlbag.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.monarchinitiative.owlbag.model.CliqueSolution;
import org.monarchinitiative.owlbag.model.EdgeType;
import org.monarchinitiative.owlbag.model.ProbabilisticEdge;
import org.monarchinitiative.owlbag.model.ProbabilisticGraph;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.google.common.base.Strings;

/**
 * 
 * Calculates the most probably graph, given a set of edge probabilities, and
 * a set of logical edges
 * 
 * @author cjm
 *
 */
public class ProbabilisticGraphCalculator {

	private static Logger LOG = Logger.getLogger(ProbabilisticGraphCalculator.class);

	ProbabilisticGraph graph;
	OWLOntology sourceOntology;
	OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
	//OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
	public int maxProbabilisticEdges = 8;
	public int minProbabilisticEdges = 1;

	public ProbabilisticGraphCalculator(OWLOntology sourceOntology) {
		super();
		this.sourceOntology = sourceOntology;
	}


	/**
	 * @return the sourceOntology
	 */
	public OWLOntology getSourceOntology() {
		return sourceOntology;
	}


	/**
	 * @param sourceOntology the sourceOntology to set
	 */
	public void setSourceOntology(OWLOntology sourceOntology) {
		this.sourceOntology = sourceOntology;
	}


	public OWLOntologyManager getOWLOntologyManager() {
		return sourceOntology.getOWLOntologyManager();
	}
	public OWLDataFactory getOWLDataFactory() {
		return getOWLOntologyManager().getOWLDataFactory();
	}

	/**
	 * @return the reasonerFactory
	 */
	public OWLReasonerFactory getReasonerFactory() {
		return reasonerFactory;
	}

	/**
	 * @param reasonerFactory the reasonerFactory to set
	 */
	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}



	/**
	 * @return the maxProbabilisticEdges
	 */
	public int getMaxProbabilisticEdges() {
		return maxProbabilisticEdges;
	}

	/**
	 * @param maxProbabilisticEdges the maxProbabilisticEdges to set
	 */
	public void setMaxProbabilisticEdges(int maxProbabilisticEdges) {
		this.maxProbabilisticEdges = maxProbabilisticEdges;
	}



	/**
	 * @return the minProbabilisticEdges
	 */
	public int getMinProbabilisticEdges() {
		return minProbabilisticEdges;
	}


	/**
	 * @param minProbabilisticEdges the minProbabilisticEdges to set
	 */
	public void setMinProbabilisticEdges(int minProbabilisticEdges) {
		this.minProbabilisticEdges = minProbabilisticEdges;
	}


	/**
	 * @return the graph
	 */
	public ProbabilisticGraph getGraph() {
		return graph;
	}

	/**
	 * @param graph the graph to set
	 */
	public void setGraph(ProbabilisticGraph graph) {
		this.graph = graph;
	}


	public void addImport(OWLOntology baseOntology, OWLOntology parentOntology) {
		OWLImportsDeclaration d = getOWLDataFactory().getOWLImportsDeclaration(baseOntology.getOntologyID().getOntologyIRI());
		AddImport ai = new AddImport(parentOntology, d);
		getOWLOntologyManager().applyChange(ai);

	}



	public Set<Node<OWLClass>> findCliques() throws OWLOntologyCreationException {

		OWLOntology dynamicOntology;

		dynamicOntology = getOWLOntologyManager().createOntology();
		addImport(sourceOntology, dynamicOntology);
		OWLReasoner reasoner = getReasonerFactory().createReasoner(dynamicOntology);

		for (ProbabilisticEdge e : graph.getEdges()) {
			OWLEquivalentClassesAxiom ax = getOWLDataFactory().getOWLEquivalentClassesAxiom(e.getSourceClass(), e.getTargetClass());
			getOWLOntologyManager().addAxiom(dynamicOntology, ax);
		}
		reasoner.flush();
		int maxCliqueSize = 0;
		Set<Node<OWLClass>> cliques = new HashSet<Node<OWLClass>>();
		for (OWLClass c : sourceOntology.getClassesInSignature()) {
			Node<OWLClass> n = reasoner.getEquivalentClasses(c);
			int size = n.getSize();
			if (size > 1) {
				cliques.add(n);
			}
			if (size > maxCliqueSize)
				maxCliqueSize = size;
		}
		reasoner.dispose();
		LOG.info("MaxSize=" + maxCliqueSize);
		LOG.info("|Cliques|=" + cliques.size());
		return cliques;
	}


	/**
	 * Breaks all cliques and adds resulting axioms back into main ontology
	 * @return 
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public Set<CliqueSolution> breakCliques() throws OWLOntologyCreationException {
		Set<Node<OWLClass>> cliques = findCliques();
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		Set<CliqueSolution> rpts = new HashSet<CliqueSolution>();
		int ctr = 0;
		for (Node<OWLClass> n : cliques) {
			ctr++;
			LOG.info("NUM:"+ctr+"/"+cliques.size());
			CliqueSolution rpt = breakClique(n);
			if (rpt == null)
				continue;
			rpts.add(rpt);
			if (rpt.axioms != null) {
				axioms.addAll(rpt.axioms);
			}
		}
		getOWLOntologyManager().addAxioms(sourceOntology, axioms);
		return rpts;
	}

	public CliqueSolution breakClique(Node<OWLClass> n) throws OWLOntologyCreationException {
		LOG.info("BREAKING CLIQUE: "+n);

		// initialize
		OWLOntologyManager mgr = getOWLOntologyManager();
		Set<OWLClass> clzs = n.getEntities();
		ProbabilisticGraph sg = getSubGraph(n);

		// candidate solution
		CliqueSolution cliqSoln = new CliqueSolution();
		cliqSoln.cliqueId = n.getRepresentativeElement().getIRI().toString();
		cliqSoln.size = n.getEntities().size();
		cliqSoln.solved = false; // false by default, until a solution is found


		cliqSoln.numberOfProbabilisticEdges = sg.getEdges().size();

		if (cliqSoln.numberOfProbabilisticEdges < minProbabilisticEdges) {
			LOG.info("TOO FEW: E="+cliqSoln.numberOfProbabilisticEdges+" N="+n.getEntities().size());
			// TODO: if 1 edge, return the most probable
			return cliqSoln;
		}

		// note: we delay the calculation of this until now,
		// as the subgraph has passed initial filter tests
		calculateEdgeProbabilityMatrix(sg);

		Double initialProbability = 1.0;
		Set<OWLAxiom> additionalLogicalAxioms = new HashSet<OWLAxiom>();
		if (cliqSoln.numberOfProbabilisticEdges > maxProbabilisticEdges) {
			LOG.info("REDUCING: "+n.getEntities().size());
			initialProbability *= reduceClique(sg, additionalLogicalAxioms);
			LOG.info("INIT_PR: "+initialProbability);
			LOG.info("NEW_LOGICAL_EDGES: "+sg.getLogicalEdges().size()+" :: "+sg.getLogicalEdges());
			cliqSoln.messages.add("Used heuristic to estimate some probabilistic edges - confidence may be negative");
			if (sg.getEdges().size() > maxProbabilisticEdges) {
				LOG.info("STILL TOO MANY, GIVING UP: "+n.getEntities().size());
				return cliqSoln;
			}
			// must be recalculated
			calculateEdgeProbabilityMatrix(sg);
		}

		List<ProbabilisticEdge> edges =  sg.getEdges();

		// the clique has a logical part, which stays constant,
		// and a probabilistic part, which we test all combinations of
		OWLOntology logicalOntology = mgr.createOntology(sg.getLogicalEdges());

		int N = edges.size();
		LOG.info("N="+N);
		EdgeType[] etypes = EdgeType.values();
		int M = etypes.length;
		//LOG.info("M="+M);
		int NUM_STATES = (int) Math.pow(M, N);
		LOG.info("STATES="+NUM_STATES);
		OWLDataFactory df = getOWLDataFactory();

		// debug
		for (OWLClass c : n.getEntities()) {
			LOG.info("  SUBGRAPH_CLASS:"+c);
		}

		OWLOntology probOntology = mgr.createOntology();
		addImport(logicalOntology, probOntology);
		OWLReasoner reasoner = reasonerFactory.createReasoner(probOntology);


		OWLAxiom[][] axiomIndex = sg.getAxiomIndex();
		double[][] probabilityIndex = sg.getProbabilityIndex();

		// test the probability of every combo of axioms
		// 4^N combinatorial states, each state representing an axiom state (<,>,=,null)
		double maxJointProbability = 0;
		double sumOfJointProbabilities = 0;
		Map<Integer, Double> stateScoreMap = new HashMap<Integer, Double>();
		Set<OWLAxiom> currentBestCombinationOfAxioms = new HashSet<OWLAxiom>();
		//Set<OWLAxiom> prevAxioms = new HashSet<OWLAxiom>();
		for (int s=0; s<NUM_STATES; s++) {
			// each combination can be represented as an N digit number in base M
			char[] states = Strings.padStart(Integer.toString(s, M), N, '0').toCharArray();
			//LOG.info("STATE:"+s+" :: "+states);
			Set<OWLAxiom> candidateCombinationOfProbabilisticAxioms = new HashSet<OWLAxiom>();
			boolean isZeroProbability = false;
			double jointProbability = 1;
			for (int ei = 0; ei < N; ei++) {
				char sc = states[ei];
				int j = Character.getNumericValue(sc);
				double pr = probabilityIndex[ei][j];

				// TODO: refactor to use logs;
				// not urgent as we don't expect underflows
				jointProbability *= pr;

				// TODO: change threshold
				if (pr < 0.001 || jointProbability < maxJointProbability || jointProbability < 0.0001) {
					// we do not progress any further if it is impossible to beat the
					// current best, or if the end result will be below a threshold
					isZeroProbability = true;
					jointProbability = 0;
					break;
				}
				OWLAxiom ax = axiomIndex[ei][j];
				if (ax != null)
					candidateCombinationOfProbabilisticAxioms.add(ax);
			}
			if (jointProbability < maxJointProbability) {
				continue;
			}
			// TEST CONSISTENCY: 

			// reset
			mgr.removeAxioms(probOntology, probOntology.getAxioms());

			mgr.addAxioms(probOntology, candidateCombinationOfProbabilisticAxioms);
			//prevAxioms = new HashSet<OWLAxiom>(axioms);
			reasoner.flush();

			// first test to make sure that no classes from the same ontology
			// are inferred to be identical
			// TODO: make this a more generic procedure whereby any set of classes
			// can be declared to be in an all-different set
			for (OWLClass c : n.getEntities()) {
				for (OWLClass d : n.getEntities()) {
					if (c.equals(d))
						continue;

					if (isInSameOntology(c, d)) {
						//LOG.info("Same ontology: "+c+" "+d);
						if (reasoner.getEquivalentClasses(c).contains(d)) {
							//LOG.info("   INVALID: "+c+"=="+d);
							isZeroProbability = true;
							jointProbability = 0;
							break;
						}
					}
				}				
			}
			if (isZeroProbability) {
				continue;
			}

			int maxParents = 0;
			Map<OWLClass, Integer> parentCountMap = new HashMap<OWLClass, Integer>();
			
			// test for consistency.
			//
			// we first merge the current candidate probabilistic axioms
			// with the pre-materialized probabilistic axioms
			Set<OWLAxiom> candidateCombinationOfNewAxioms =
					new HashSet<OWLAxiom>(candidateCombinationOfProbabilisticAxioms);
			candidateCombinationOfNewAxioms.addAll(additionalLogicalAxioms);
			for (OWLAxiom ax : candidateCombinationOfNewAxioms) {
				if (ax instanceof OWLSubClassOfAxiom) {
					OWLClass c = (OWLClass) ((OWLSubClassOfAxiom) ax).getSubClass();
					OWLClass t = (OWLClass) ((OWLSubClassOfAxiom) ax).getSuperClass();
					Set<OWLClass> superClasses = reasoner.getSuperClasses(c, false).getFlattened();
//					if (c.getIRI().toString().contains("010")) {
//						LOG.info("candidateCombinationOfProbabilisticAxioms = "+candidateCombinationOfProbabilisticAxioms);
//						LOG.info("   REMOVEME: "+c+" << "+superClasses);
//					}

					if (!superClasses.contains(t)) {
						LOG.info(c+" NOT < "+t+" ACTUAL < "+superClasses);
						isZeroProbability = true;
						jointProbability = 0;
						break;
					}
					if (!parentCountMap.containsKey(c))
						parentCountMap.put(c, 1);
					else {
						int np = parentCountMap.get(c)+1;
						parentCountMap.put(c, np);
						if (np > maxParents) {
							maxParents = np;
						}
					}
				}
				else {
					OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom)ax;
					List<OWLClassExpression> xs = eax.getClassExpressionsAsList();
					// guaranteed to have 2
					Set<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(xs.get(0)).getEntities();
					if (!equivalentClasses.contains(xs.get(1))) {
						isZeroProbability = true;
						jointProbability = 0;
						break;
					}
				}
			}
			if (isZeroProbability) {
				continue;
			}

			// look at distribution of number of parents of leaf nodes.
			// for now we have a simplistic model that just looks at the maximum number of
			// parents for any leaf node, and penalizes this proportionally


			double pMaxParents = 0.3;
			if (maxParents > 1) {
				pMaxParents = 0.4 * (1.0 / Math.pow(2, maxParents-1));
			}
			LOG.info("MAX_PARENTS:"+maxParents+" p="+pMaxParents);
			jointProbability *= pMaxParents;

			jointProbability *= initialProbability;

			LOG.info(" Prob="+jointProbability+" "+candidateCombinationOfProbabilisticAxioms);
			stateScoreMap.put(s, jointProbability);
			sumOfJointProbabilities += jointProbability;
			if (jointProbability > maxJointProbability) {
				maxJointProbability = jointProbability;
				currentBestCombinationOfAxioms = new HashSet<OWLAxiom>(candidateCombinationOfProbabilisticAxioms);
			}
		}

		reasoner.dispose();

		sumOfJointProbabilities += (1-initialProbability);

		double finalPr = maxJointProbability / sumOfJointProbabilities;
		LOG.info("MAX_JOINT_PR:"+maxJointProbability);
		LOG.info("ADJUSTED:"+finalPr);
		LOG.info("BEST: "+currentBestCombinationOfAxioms);
		cliqSoln.probability = finalPr;
		List<Double> vals = new ArrayList<Double>(stateScoreMap.values());
		//Ordering<Integer> valueComparator = Ordering.natural().onResultOf(Functions.forMap(stateScoreMap));
		Collections.sort(vals, Collections.reverseOrder());
		LOG.info("BEST/CHECK: "+vals.get(0));
		//LOG.info("2nd BEST: "+vals.get(1));

		Set<OWLAxiom> bestCombinationOfAxioms = currentBestCombinationOfAxioms;
		if (additionalLogicalAxioms != null)
			bestCombinationOfAxioms.addAll(additionalLogicalAxioms);
		if (vals.size() > 1)
			cliqSoln.confidence = (maxJointProbability / vals.get(1))-1;
		else
			cliqSoln.confidence = (maxJointProbability /(1-initialProbability))-1;
		cliqSoln.axioms = 
				new HashSet<OWLAxiom>(bestCombinationOfAxioms);
		// TODO: annotate axioms

		cliqSoln.axiomStrings = new HashSet<String>();
		for (OWLAxiom ax : bestCombinationOfAxioms) {
//			if (owlpp != null)
//				cliqSoln.axiomStrings.add(owlpp.render(ax));
//			else
				cliqSoln.axiomStrings.add(ax.toString());
		}
		cliqSoln.solved =  true;
		return cliqSoln;
	}

	public void calculateEdgeProbabilityMatrix(ProbabilisticGraph sg) {

		List<ProbabilisticEdge> edges = sg.getEdges();
		int N = edges.size();
		OWLAxiom[][] axiomIndex = new OWLAxiom[N][4];
		double[][] probabilityIndex = new double[N][4];

		OWLDataFactory df = getOWLDataFactory();

		// initialize
		for (int ei = 0; ei < N; ei++) {
			ProbabilisticEdge e = edges.get(ei);
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
				probabilityIndex[ei][j] = pr;
				axiomIndex[ei][j] = ax;
			}
		}
		sg.setAxiomIndex(axiomIndex);
		sg.setProbabilityIndex(probabilityIndex);
	}

	public ProbabilisticGraph getSubGraph(Node<OWLClass> n) {

		Set<OWLClass> clzs = n.getEntities();
		Set<ProbabilisticEdge> edges = new HashSet<ProbabilisticEdge>();
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();

		// TODO: improve efficiency
		for (ProbabilisticEdge e : graph.getEdges()) {
			if (clzs.contains(e.getSourceClass()) &&
					clzs.contains(e.getTargetClass())) {
				edges.add(e);
			}
		}
		for (OWLSubClassOfAxiom ax : sourceOntology.getAxioms(AxiomType.SUBCLASS_OF)) {
			if (clzs.contains(ax.getSubClass()) &&
					clzs.contains(ax.getSuperClass())) {
				axioms.add(ax);
			}		
		}
		for (OWLEquivalentClassesAxiom ax : sourceOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
			for (OWLClassExpression x : ax.getClassExpressions()) {
				if (clzs.contains(x)) {
					axioms.add(ax);
					break;
				}
			}		
		}
		ProbabilisticGraph sg = new ProbabilisticGraph();
		sg.setEdges(new ArrayList<ProbabilisticEdge>(edges));
		sg.setLogicalEdges(axioms);
		return sg;
	}

	public boolean isInSameOntology(OWLClass c, OWLClass d) {
		return getClassPrefix(c).equals(getClassPrefix(d));
	}

	public String getClassPrefix(OWLClass c) {
		String frag = c.getIRI().toString().replace("http://purl.obolibrary.org/obo/", "");
		return frag.replaceAll("_.*", "");
	}

	// TODO: explore use of probability distribution P( NumOfDirectParents(C) | C in O).
	// What are currently prior probabilities could be conditional, based on this, but
	// it may actually be more challenging to estimate these.
	// alternatively, this could be another source node in the overall network.
	public void getDistributionOfNumberOfSuperclasses() {
		for (OWLClass c : sourceOntology.getClassesInSignature(true)) {
			sourceOntology.getSubClassAxiomsForSubClass(c).size();
		}
	}

	public Double reduceClique(ProbabilisticGraph sg, Set<OWLAxiom> addedLogicalAxioms) {
		Double pr = 1.0;
		List<ProbabilisticEdge> newEdges = new ArrayList<ProbabilisticEdge>();
		for (int i=0; i<sg.getEdges().size(); i++) {
			ProbabilisticEdge e = sg.getEdges().get(i);
			double[] prt = sg.getProbabilityIndex()[i];
			OWLAxiom ax = null;
			if (prt[1] > 0.5) {
				ax = sg.getAxiomIndex()[i][1];
				pr *= prt[1];
			}
			else if (prt[2] > 0.5) {
				ax = sg.getAxiomIndex()[i][2];				
				pr *= prt[2];
			}
			if (ax != null) {
				addedLogicalAxioms.add(ax);
			}
			else {
				newEdges.add(e);
			}
		}
		LOG.info("REDUCED:"+addedLogicalAxioms.size()+" :: "+addedLogicalAxioms);
		sg.getLogicalEdges().addAll(addedLogicalAxioms);
		sg.setEdges(newEdges);
		return pr;
	}

}
