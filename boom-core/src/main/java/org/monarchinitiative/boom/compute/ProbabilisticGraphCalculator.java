package org.monarchinitiative.boom.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.monarchinitiative.boom.io.LabelProvider;
import org.monarchinitiative.boom.model.CliqueSolution;
import org.monarchinitiative.boom.model.EdgeType;
import org.monarchinitiative.boom.model.ProbabilisticEdge;
import org.monarchinitiative.boom.model.ProbabilisticGraph;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

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

    ProbabilisticGraph probabilisticGraph;
    OWLOntology sourceOntology;
    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    OWLObjectRenderer renderer;
    Set<OWLClass> filterOnClasses;
    public boolean isExperimental = false; // TEMPORARY


    //OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();

    /**
     * Number of probabilistic edges after which a clique is broken down using heuristics
     */
    public int maxProbabilisticEdges = 10;

    /**
     * 
     */
    public int cliqueSplitSize = 6;

    /**
     * Number of probabilistic edges below which no attempt will be made to resolve clique
     */
    public int minProbabilisticEdges = 0;

    public ProbabilisticGraphCalculator(OWLOntology sourceOntology) {
        super();
        this.sourceOntology = sourceOntology;
        LabelProvider provider = new LabelProvider(sourceOntology);
        renderer = 
                new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
    }


    public ProbabilisticGraphCalculator(OWLOntology owlOntology,
            ProbabilisticGraph pg) {
        this(owlOntology);
        setProbabilisticGraph(pg);
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


    /**
     * @return ontology manager
     */
    public OWLOntologyManager getOWLOntologyManager() {
        return sourceOntology.getOWLOntologyManager();
    }
    /**
     * @return data factory
     */
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
     * @return the cliqueSplitSize
     */
    public int getCliqueSplitSize() {
        return cliqueSplitSize;
    }


    /**
     * @param cliqueSplitSize the cliqueSplitSize to set
     */
    public void setCliqueSplitSize(int cliqueSplitSize) {
        this.cliqueSplitSize = cliqueSplitSize;
    }


    /**
     * @param filterOnClasses the filterOnClasses to set
     */
    public void setFilterOnClasses(Set<OWLClass> filterOnClasses) {
        this.filterOnClasses = filterOnClasses;
    }


    /**
     * @return the graph
     */
    public ProbabilisticGraph getProbabilisticGraph() {
        return probabilisticGraph;
    }

    /**
     * @param graph the graph to set
     */
    public void setProbabilisticGraph(ProbabilisticGraph graph) {
        this.probabilisticGraph = graph;
    }


    /**
     * @param baseOntology
     * @param parentOntology
     */
    public void addImport(OWLOntology baseOntology, OWLOntology parentOntology) {
        Optional<IRI> ontologyIRI = baseOntology.getOntologyID().getOntologyIRI();
        OWLImportsDeclaration d = getOWLDataFactory().getOWLImportsDeclaration(ontologyIRI.orNull());
        AddImport ai = new AddImport(parentOntology, d);
        getOWLOntologyManager().applyChange(ai);

    }

    /**
     * For all DisjointClasses(A B),
     * materialize DisjointClasses(a b), for a in Sub(A) and b in Sub(B)
     * 
     * This ensures that module extraction will have disjointness axioms even when parents omitted
     */
    public void propagateDownDisjointnessAxioms() {

        LOG.info("Propagating down disjointness axioms...");
        OWLReasoner reasoner = getReasonerFactory().createReasoner(sourceOntology);
        LOG.info("Created reasoner...");
        
        Set<OWLDisjointClassesAxiom> newAxioms = new HashSet<>();
        for (OWLDisjointClassesAxiom a : sourceOntology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
            Set<OWLClass> cs = a.getClassExpressions().stream().
                    filter(x -> !x.isAnonymous()).map(x -> x.asOWLClass()).collect(Collectors.toSet());
            for (OWLClass c : cs) {
                Set<OWLClass> csubs = reasoner.getSubClasses(c, false).getFlattened();
                csubs.add(c);
                csubs.remove(getOWLDataFactory().getOWLNothing());
                for (OWLClass d : cs) {

                    // symmetrical pairs: only perform once for each pair
                    if (c.compareTo(d) > 0) {
                        Set<OWLClass> dsubs = reasoner.getSubClasses(d, false).getFlattened();
                        dsubs.add(d);
                        dsubs.remove(getOWLDataFactory().getOWLNothing());

                        for (OWLClass csub : csubs) {
                            for (OWLClass dsub : dsubs) {
                                OWLDisjointClassesAxiom ax = getOWLDataFactory().getOWLDisjointClassesAxiom(csub, dsub);

                                //LOG.info("PROPAGATED: "+ax);
                                newAxioms.add(ax);
                            }
                        }


                    }
                }                
            }
        }
        LOG.info("Propagated disjointness axioms: "+newAxioms.size());
        getOWLOntologyManager().addAxioms(sourceOntology, newAxioms);

        reasoner.dispose();

    }

    /**
     * We find cliques by first assuming any probabilistic edge for (A,B) is set such that
     * Pr(Equiv(A,B)) == 1, and then use a reasoner to find cliques/nodes
     * 
     * Each such clique can be treated as a distinct subgraph and resolved independently
     * of the others
     * 
     * @return clique nodes
     * @throws OWLOntologyCreationException
     */
    public Set<Node<OWLClass>> findCliques() throws OWLOntologyCreationException {

        OWLOntology dynamicOntology;

        dynamicOntology = getOWLOntologyManager().createOntology();
        OWLOntology coreOntology = 
                getOWLOntologyManager().createOntology(IRI.create("http://x.org"));
        for ( OWLAxiom a : sourceOntology.getAxioms(Imports.INCLUDED)) {
            // we exclude disjoint axioms from the clique finding set;
            // this is because the clique strategy depends on assuming 
            // equivalence for every potential equivalence, likely violating
            // disjointness assumptions
            if (a instanceof OWLDisjointClassesAxiom) {
                continue;
            }
            getOWLOntologyManager().addAxiom(coreOntology, a);
        }
        addImport(coreOntology, dynamicOntology);
        OWLReasoner reasoner = getReasonerFactory().createReasoner(dynamicOntology);

        // first find any pr edges that are overridden by logical axiom entailments.
        // note for the mapping scenario, this should only happen when there are
        // asserted logical axioms across ontologies.
        // For the MonDO pipeline, this includes the OMIM clusters that are
        // later made equivalent with DO
        Set<ProbabilisticEdge> rmEdges = findEntailedProbabilisticEdges(probabilisticGraph, reasoner);
        if (rmEdges.size() > 0) {
            for (ProbabilisticEdge e : rmEdges) {
                LOG.info("Pruning edge with logic entailment: "+render(e));
            }
            if (isExperimental) {
                probabilisticGraph.getProbabilisticEdges().removeAll(rmEdges);
            }
            else {
                LOG.error("FIX Me!!");
            }
        }

        // assume every probabilistic edge is an equivalence axiom
        for (ProbabilisticEdge e : probabilisticGraph.getProbabilisticEdges()) {
            if (e.getProbabilityTable().getTypeProbabilityMap().get(EdgeType.NONE) < 1.0) {
                OWLEquivalentClassesAxiom ax = translateEdgeToEquivalenceAxiom(e);
                getOWLOntologyManager().addAxiom(dynamicOntology, ax);
            }
        }
        reasoner.flush();
        Set<Node<OWLClass>> cliques = new HashSet<>();
        for (OWLClass c : sourceOntology.getClassesInSignature()) {
            Node<OWLClass> n = reasoner.getEquivalentClasses(c);
            int size = n.getSize();
            if (size > 1) {
                cliques.add(n);
            }
        }

        LOG.info("|Cliques| = "+cliques.size()+" (first pass)");
        // split cliques
        Set<Node<OWLClass>> removedCliques = new HashSet<>();
        Set<Node<OWLClass>> newCliques = new HashSet<>();

        // iterate through cliques and heuristically split
        // any that are too large, for which removing a single connection
        // can result in two semantically non-overlapping sub-cliques.
        // the removed edge is a candidate error
        for (Node<OWLClass> node : cliques) {
            int nodeSize = node.getSize();
            // TODO - less arbitrary sizes
            if (nodeSize > cliqueSplitSize) {
                LOG.info("Candidate for splitting: "+render(node.getRepresentativeElement())+" |N|="+node.getSize());
                Set<OWLClass> clzs = node.getEntities();
                Set<ProbabilisticEdge> badEdges = new HashSet<>();
                for (ProbabilisticEdge e : probabilisticGraph.getProbabilisticEdges()) {
                    if (clzs.contains(e.getSourceClass())) {
                        OWLEquivalentClassesAxiom ax = translateEdgeToEquivalenceAxiom(e);								
                        getOWLOntologyManager().removeAxiom(dynamicOntology, ax);
                        reasoner.flush();
                        Node<OWLClass> newLeftNode = reasoner.getEquivalentClasses(e.getSourceClass());
                        Node<OWLClass> newRightNode = reasoner.getEquivalentClasses(e.getTargetClass());
                        // TODO - do not hardcode
                        if (newLeftNode.getSize() >= 5 && newRightNode.getSize() >= 5) {
                            double sim = getSimilarity(e.getSourceClass(), e.getTargetClass(), reasoner);
                            LOG.debug("Similarity = "+sim+" For: "+e);
                            if (sim < 0.25) { // TODO - no hardcoding
                                badEdges.add(e);
                                LOG.info(" Splitting: "+render(e)+"  Similarity="+sim+" subCliqueSizes: "+
                                        newLeftNode.getSize() +","+newRightNode.getSize());
                            }
                        }
                        getOWLOntologyManager().addAxiom(dynamicOntology, ax);
                    }
                }
                if (badEdges.size() > 0) {
                    for (ProbabilisticEdge e : badEdges) {
                        OWLEquivalentClassesAxiom ax = translateEdgeToEquivalenceAxiom(e);
                        getOWLOntologyManager().removeAxiom(dynamicOntology, ax);
                    }
                    reasoner.flush();
                    removedCliques.add(node);
                    for (OWLClass c : clzs) {
                        newCliques.add(reasoner.getEquivalentClasses(c));
                    }
                    LOG.info("prEdges (before splits):"+probabilisticGraph.getProbabilisticEdges().size());
                    probabilisticGraph.removeEdges(badEdges);
                    LOG.info("prEdges (after splits):"+probabilisticGraph.getProbabilisticEdges().size());
                }
            }
        }

        if (removedCliques.size() > 0) {
            LOG.info("REMOVING: "+removedCliques);
            LOG.info("ADDING: "+newCliques);
            cliques.removeAll(removedCliques);
            cliques.addAll(newCliques);
        }
        LOG.info("|Cliques| = "+cliques.size()+" (after splitting)");


        int maxCliqueSize = 0;
        for (Node<OWLClass> n : cliques) {
            if (n.getSize() > maxCliqueSize)
                maxCliqueSize = n.getSize();
        }


        reasoner.dispose();
        LOG.info("MaxCliqueSize=" + maxCliqueSize);
        LOG.info("|Cliques|=" + cliques.size());
        return cliques;
    }

    private double getSimilarity(OWLClass c, OWLClass d, OWLReasoner reasoner) {
        Set<OWLClass> ca = reasoner.getSuperClasses(c, false).getFlattened();
        Set<OWLClass> da = reasoner.getSuperClasses(d, false).getFlattened();
        return Sets.intersection(ca, da).size() / (double)(Sets.union(ca, da).size());
    }

    private OWLEquivalentClassesAxiom translateEdgeToEquivalenceAxiom(ProbabilisticEdge e) {
        return getOWLDataFactory().getOWLEquivalentClassesAxiom(e.getSourceClass(),
                e.getTargetClass());
    }

    /**
     * Solves all cliques and adds resulting axioms back into main ontology
     * @return clique solutions
     * 
     * @throws OWLOntologyCreationException
     */
    public Set<CliqueSolution> solveAllCliques() throws OWLOntologyCreationException {
        propagateDownDisjointnessAxioms();
        Set<Node<OWLClass>> cliques = findCliques();
        Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
        Set<CliqueSolution> rpts = new HashSet<CliqueSolution>();
        int ctr = 0;
        for (Node<OWLClass> n : cliques) {

            // user option: only process nodes with desired classes
            if (filterOnClasses != null && filterOnClasses.size() > 0) {
                if (n.getEntities().stream().filter( c -> filterOnClasses.contains(c)).count() == 0) {
                    LOG.info("Skipping: "+render(n.getEntities()));
                    continue;
                }
            }

            ctr++;
            LOG.info("NUM:"+ctr+"/"+cliques.size());
            long t1 = System.currentTimeMillis();
            CliqueSolution rpt = solveClique(n);
            long t2 = System.currentTimeMillis();
            rpt.timeToSolve = t2-t1;
            if (rpt != null) {
                rpts.add(rpt);
                if (rpt.axioms != null) {
                    axioms.addAll(rpt.axioms);
                }
            }
        }
        getOWLOntologyManager().addAxioms(sourceOntology, axioms);
        return rpts;
    }

    /**
     * Solves a clique (subgraph consisting of maximal set of nodes that are either
     * logically equivalent or have a probability of being equivalent)
     * 
     * The clique is solved by testing all logically permissible sets of combinations
     * of logical relationships between all members, and determining the probability of
     * each such combination.
     * 
     * @param n
     * @return clique solution
     * @throws OWLOntologyCreationException
     */
    public CliqueSolution solveClique(Node<OWLClass> n) throws OWLOntologyCreationException {
        LOG.info("SOLVING CLIQUE: "+n);

        // initialize
        OWLOntologyManager mgr = getOWLOntologyManager();
        Set<OWLClass> clzs = n.getEntities();
        ProbabilisticGraph subPrGraph = getProbabilisticGraphModule(n);

        // candidate solution
        CliqueSolution cliqSoln = new CliqueSolution();
        cliqSoln.cliqueId = n.getRepresentativeElement().getIRI().toString();
        cliqSoln.size = n.getEntities().size();
        cliqSoln.solved = false; // false by default, until a solution is found
        cliqSoln.classes = clzs;
        cliqSoln.members = 
                clzs.stream().map( (OWLClass c) -> c.getIRI().toString()).collect(Collectors.toSet());
        cliqSoln.subGraph = subPrGraph;
        cliqSoln.initialNumberOfProbabilisticEdges = subPrGraph.getProbabilisticEdges().size();

        if (cliqSoln.initialNumberOfProbabilisticEdges < minProbabilisticEdges) {
            LOG.info("<min probEdges: "+cliqSoln.initialNumberOfProbabilisticEdges+
                    " N="+n.getEntities().size());
            // TODO: if 1 edge, return the most probable
            return cliqSoln;
        }

        // note: we delay the calculation of this until now,
        // as the subgraph has passed initial filter tests
        subPrGraph.calculateEdgeProbabilityMatrix(getOWLDataFactory());

        // extendedOntology contains candidate axioms, to be tested for satisfiability
        //OWLOntology extendedOntology = mgr.createOntology();
        //addImport(logicalOntology, extendedOntology);
        //OWLReasoner reasoner = reasonerFactory.createReasoner(extendedOntology);


        Double initialProbability = 1.0;

        // ---
        // reduce combinatorial possibilities using greedy algorithm to turn
        // pr edges into logical edges
        // ---
        
        // logical axioms created as result of procedure
        Set<OWLAxiom> additionalLogicalAxioms = new HashSet<OWLAxiom>();
        
        if (cliqSoln.initialNumberOfProbabilisticEdges > maxProbabilisticEdges) {

            OWLOntology coreOntology = mgr.createOntology(subPrGraph.getLogicalEdges());
            OWLOntology extOntology = mgr.createOntology();
            addImport(coreOntology, extOntology);
            OWLReasoner reasoner = reasonerFactory.createReasoner(extOntology);
            Set<OWLAxiom> unsatisfiableAxioms = new HashSet<OWLAxiom>();

            int nReduced = 0;
            while (subPrGraph.getProbabilisticEdges().size() > maxProbabilisticEdges) {
                // TODO: write centroid for debugging purposes
                LOG.info("REDUCING PrEdges // |N|="+n.getEntities().size()+
                        " PR_EDGES: "+subPrGraph.getProbabilisticEdges().size()+
                        " LOG_EDGES: "+subPrGraph.getLogicalEdges().size());
                initialProbability *= 
                        reduceClique(subPrGraph, additionalLogicalAxioms, 
                                reasoner, unsatisfiableAxioms, 0);
                LOG.info(" New Pr: "+initialProbability);
                LOG.debug(" NEW_PR_EDGES: "+subPrGraph.getProbabilisticEdges().size()+" :: "+subPrGraph.getProbabilisticEdges());
                LOG.debug(" NEW_LOGICAL_EDGES: "+subPrGraph.getLogicalEdges().size()+" :: "+
                        render(subPrGraph.getLogicalEdges()));
                // must be recalculated
                subPrGraph.calculateEdgeProbabilityMatrix(getOWLDataFactory());
                nReduced++;
            }
            cliqSoln.messages.add("Used heuristic to estimate "+nReduced+" probabilistic edges - confidence may be negative. |Reduced| = "+nReduced);
            for (OWLAxiom ax : unsatisfiableAxioms) {
                cliqSoln.messages.add("ELIMINATED_UNSATISFIABLE: " + render(ax)); 
            }
            reasoner.dispose();
            // TODO: determine if we can split clique
        }

        List<ProbabilisticEdge> prEdges =  subPrGraph.getProbabilisticEdges();

        // the clique has a logical part, which stays constant,
        // and a probabilistic part, which we test all combinations of
        OWLOntology logicalOntology = mgr.createOntology(subPrGraph.getLogicalEdges());
        // probOntology contains candidate axioms, to be tested for satisfiability
        OWLOntology candidateOntology = mgr.createOntology();
        addImport(logicalOntology, candidateOntology);

        // we create a new reasoner instance for every clique
        OWLReasoner reasoner = reasonerFactory.createReasoner(candidateOntology);

        Map<OWLClass,Set<OWLClass>> assertedSuperClassesMap = new HashMap<>();
        for (OWLClass c : n.getEntities()) {
            assertedSuperClassesMap.put(c, reasoner.getSuperClasses(c, false).getFlattened());
        }		

        // for debug purposes
        Set<OWLDisjointClassesAxiom> disjointAxioms = 
                logicalOntology.getAxioms(AxiomType.DISJOINT_CLASSES);
        if (disjointAxioms.size() > 0) {
            LOG.info("Disjointness Axioms in module: "+disjointAxioms);
        }

        int N = prEdges.size();
        LOG.info("|WeightedEdges|="+N);
        EdgeType[] etypes = EdgeType.values();
        int M = etypes.length;  // currently M=4
        //LOG.info("M="+M);
        int NUM_STATES = (int) Math.pow(M, N);

        LOG.info("STATES="+NUM_STATES);
        // debug
        for (OWLClass c : n.getEntities()) {
            LOG.info("  CLIQUE MEMBER:"+render(c));
        }



        OWLAxiom[][] axiomIndex = subPrGraph.getAxiomIndex();
        double[][] probabilityIndex = subPrGraph.getProbabilityIndex();

        // test the probability of every combo of axioms
        // 4^N combinatorial states, each state representing an axiom state (<,>,=,null)
        double maxJointProbability = 0;
        double sumOfJointProbabilities = 0;
        Map<Integer, Double> stateScoreMap = new HashMap<Integer, Double>();
        Set<OWLAxiom> currentBestCombinationOfAxioms = new HashSet<OWLAxiom>();
        int numValidCombos = 0;
        //Set<OWLAxiom> prevAxioms = new HashSet<OWLAxiom>();

        // for reporting purposes
        int numBelowProbThreshold = 0;
        int numWithInvalidSubClass = 0;
        int numWithInvalidEquivClass = 0;
        int numEquivInSameOnt = 0;
        int numUnsats = 0;
        int numFilteredByProb = 0;
        int numViolatedSubClass = 0;
        int numMissingEquivClass = 0;
        int numReasonerCalls = 0;

        for (int s=0; s<NUM_STATES; s++) {
            // each combination can be represented as an N digit number in base M
            char[] states = Strings.padStart(Integer.toString(s, M), N, '0').toCharArray();
            if (s % 1000 == 0) {
                LOG.info("STATE:"+s+" / "+NUM_STATES+" :: "+new String(states)+
                        " max(p)="+maxJointProbability+
                        " V:"+numValidCombos+
                        " <p:"+numBelowProbThreshold+
                        " filtered:"+numFilteredByProb+
                        " inv(Sub):"+numWithInvalidSubClass+
                        " unsats:"+numUnsats+
                        " sameOnt:"+numEquivInSameOnt+
                        " xx:"+numViolatedSubClass);
            }

            // each state corresponds to a combination of axioms; initialize this
            Set<OWLAxiom> axiomCombo = new HashSet<OWLAxiom>();
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
                if (pr < 0.001 || jointProbability < maxJointProbability) {
                    // we do not progress any further if it is impossible to beat the
                    // current best, or if the end result will be below a threshold
                    isZeroProbability = true;
                    jointProbability = 0;
                    numFilteredByProb++;
                    //LOG.info("Skipping Pr="+pr);
                    break;
                }
                if (jointProbability < Math.pow(0.01, N)) {
                    isZeroProbability = true;
                    jointProbability = 0;
                    LOG.info("Skipping; Joint probability="+jointProbability);
                    numBelowProbThreshold++;
                    break;
                }
                OWLAxiom ax = axiomIndex[ei][j];
                if (ax != null)
                    axiomCombo.add(ax);
            }
            if (jointProbability == 0 || jointProbability < maxJointProbability) {
                continue;
            }
            // TEST CONSISTENCY: 

            // reset
            mgr.removeAxioms(candidateOntology, candidateOntology.getAxioms());

            mgr.addAxioms(candidateOntology, axiomCombo);
            //LOG.info("CANDIDATE COMBO:"+new String(states)+" "+axiomCombo+" Pr="+jointProbability);
            //prevAxioms = new HashSet<OWLAxiom>(axioms);
            reasoner.flush();


            // TODO: call testForInvalidEquivalencies() //DRY
            // first test to make sure that no classes from the same ontology
            // are inferred to be identical
            // TODO: make this a more generic procedure whereby any set of classes
            // can be declared to be in an all-different set
            for (OWLClass c : n.getEntities()) {
                for (OWLClass d : n.getEntities()) {
                    if (c.equals(d))
                        continue;

                    if (isInSameOntology(c, d)) {
                        // LOG.info("Same ontology: "+c+" "+d);
                        if (reasoner.getEquivalentClasses(c).contains(d)) {
                            //LOG.info("   XX INVALID: "+c+"=="+d);
                            isZeroProbability = true;
                            jointProbability = 0;
                            numEquivInSameOnt++;
                            break;
                        }
                        if (reasoner.getSuperClasses(c, false).containsEntity(d)) {
                            if (!assertedSuperClassesMap.get(c).contains(d)) {
                                // c and d are in the same ontology, and
                                // c is inferred to be a subclass of d in this configuration
                                // and this was not previously asserted
                                //LOG.info("  ENTAILED: "+c+" SubClassOf "+d);
                                // TODO - configure penalty by ontology;
                                // e.g. DOID may be expected to have missing links
                                jointProbability *= 0.9;                            
                            }
                        }						
                    }
                    if (assertedSuperClassesMap.get(c).contains(d)) {
                        if (reasoner.getEquivalentClasses(c).contains(d)) {
                            //LOG.info("   Cannot overide: "+c+" SubClassOf "+d);
                            isZeroProbability = true;
                            jointProbability = 0;
                            numWithInvalidEquivClass++;
                            break;
                        }

                    }
                    if (!reasoner.isSatisfiable(c)) {
                        //LOG.info("UNSAT:"+c+" in state: "+s);
                        isZeroProbability = true;
                        jointProbability = 0;
                        numUnsats++;
                        break;

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
                    new HashSet<OWLAxiom>(axiomCombo);
            candidateCombinationOfNewAxioms.addAll(additionalLogicalAxioms);

            for (OWLAxiom candidateAxiom : candidateCombinationOfNewAxioms) {
                //LOG.debug("CANDIDATE: "+candidateAxiom);
                if (candidateAxiom instanceof OWLSubClassOfAxiom) {
                    OWLClass c = (OWLClass) ((OWLSubClassOfAxiom) candidateAxiom).getSubClass();
                    OWLClass t = (OWLClass) ((OWLSubClassOfAxiom) candidateAxiom).getSuperClass();
                    Set<OWLClass> superClasses = reasoner.getSuperClasses(c, false).getFlattened();

                    if (!superClasses.contains(t)) {
                        LOG.debug("Pr["+candidateAxiom+"]=0 because InferredProperSupers("+c+") DOES NOT CONTAIN "+t+" // InferredProperSupers= "+superClasses);
                        //LOG.info("XX Pr["+candidateAxiom+"]=0 because InferredProperSupers("+c+") DOES NOT CONTAIN "+t+" // InferredProperSupers= "+superClasses);
                        isZeroProbability = true;
                        jointProbability = 0;
                        numViolatedSubClass++;
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
                    OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom)candidateAxiom;
                    List<OWLClassExpression> xs = eax.getClassExpressionsAsList();
                    // guaranteed to have 2
                    Set<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(xs.get(0)).getEntities();
                    if (!equivalentClasses.contains(xs.get(1))) {
                        // TODO - check, can this situation ever arise?
                        //LOG.info("XX FAILED "+xs.get(1)+" NOT IN "+equivalentClasses);
                        isZeroProbability = true;
                        jointProbability = 0;
                        numMissingEquivClass++;
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
            LOG.debug("MAX_PARENTS:"+maxParents+" p="+pMaxParents);
            jointProbability *= pMaxParents;

            jointProbability *= initialProbability;

            LOG.debug(" Prob="+jointProbability+" "+axiomCombo);
            stateScoreMap.put(s, jointProbability);
            sumOfJointProbabilities += jointProbability;
            if (jointProbability > maxJointProbability) {
                maxJointProbability = jointProbability;
                currentBestCombinationOfAxioms = new HashSet<OWLAxiom>(axiomCombo);
            }
            numValidCombos++;
        }

        LOG.info("NUM VALID COMBOS:"+numValidCombos+" / "+NUM_STATES);
        if (numValidCombos == 0) {
            LOG.error("UNSOLVABLE");
            cliqSoln.messages.add("UNSATISFIABLE");
            cliqSoln.solved = false;
            cliqSoln.probability = 0.0;
            reasoner.dispose();
            return cliqSoln;
        }

        sumOfJointProbabilities += (1-initialProbability);

        double finalPr = maxJointProbability / sumOfJointProbabilities;
        LOG.info("MAX_JOINT_PR:"+maxJointProbability);
        LOG.info("ADJUSTED:"+finalPr);
        LOG.info("BEST: "+render(currentBestCombinationOfAxioms));
        cliqSoln.probability = finalPr;
        List<Double> vals = new ArrayList<Double>(stateScoreMap.values());
        //Ordering<Integer> valueComparator = Ordering.natural().onResultOf(Functions.forMap(stateScoreMap));
        Collections.sort(vals, Collections.reverseOrder());
        if (vals.size() > 0)
            LOG.info("BEST/CHECK: "+vals.get(0));
        else
            LOG.error("NO COMBINATION");
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
            cliqSoln.axiomStrings.add(render(ax));
        }
        cliqSoln.solved =  true;

        // put sub-cliques into clique solution
        mgr.removeAxioms(candidateOntology, candidateOntology.getAxioms());
        mgr.addAxioms(candidateOntology, bestCombinationOfAxioms);
        reasoner.flush();
        cliqSoln.nodes = new HashSet<>();
        for (OWLClass c : cliqSoln.classes) {
            cliqSoln.nodes.add(reasoner.getEquivalentClasses(c));
        }

        for (OWLClass c : n.getEntities()) {
            for (OWLClass d : n.getEntities()) {
                if (c.equals(d))
                    continue;
                if (isInSameOntology(c, d)) {

                    if (reasoner.getSuperClasses(c, false).containsEntity(d)) {
                        if (!assertedSuperClassesMap.get(c).contains(d)) {
                            cliqSoln.messages.add("ENTAILED: "+render(c)+" SubClassOf "+render(d));
                        }
                    }
                }
            }
        }

        // always dispose of our reasoner
        reasoner.dispose();

        return cliqSoln;
    }

    /**
     * NOTE: some of this code is duplicated from solveClique,
     * but it cannot be replaced entirely, see below
     * 
     * @param candidateAxiom
     * @param classes
     * @param reasoner
     * @return
     */
    private boolean testForValidity(OWLAxiom candidateAxiom, Set<OWLClass> classes, OWLReasoner reasoner) {
        if (candidateAxiom == null) {
            return true;
        }
        boolean isValid = true;
        for (OWLClass c : classes) {
            for (OWLClass d : classes) {
                if (c.equals(d))
                    continue;

                if (isInSameOntology(c, d)) {
                    //LOG.info("Same ontology: "+c+" "+d);
                    if (reasoner.getEquivalentClasses(c).contains(d)) {
                        //LOG.info("   INVALID: "+c+"=="+d);
                        isValid = false;
                        break;
                    }
                }
            }				
        }
        if (isValid) {
            //LOG.info("CANDIDATE: "+candidateAxiom);
            if (candidateAxiom instanceof OWLSubClassOfAxiom) {
                OWLClass c = (OWLClass) ((OWLSubClassOfAxiom) candidateAxiom).getSubClass();
                OWLClass t = (OWLClass) ((OWLSubClassOfAxiom) candidateAxiom).getSuperClass();
                Set<OWLClass> superClasses = reasoner.getSuperClasses(c, false).getFlattened();

                if (!superClasses.contains(t)) {
                    LOG.info("Pr["+candidateAxiom+"]=0 because InferredProperSupers("+c+") DOES NOT CONTAIN "+t+" // InferredProperSupers= "+superClasses);
                    isValid = false;
                }
            }
            else {
                OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom)candidateAxiom;
                List<OWLClassExpression> xs = eax.getClassExpressionsAsList();
                // guaranteed to have 2
                Set<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(xs.get(0)).getEntities();
                if (!equivalentClasses.contains(xs.get(1))) {
                    //LOG.info("XX FAILED "+xs.get(1)+" NOT IN "+equivalentClasses);
                    isValid = false;
                }
            }			
        }
        return isValid;
    }

    /**
     * A subset of probabilistic edges may be entailed;
     * 
     * for example, an OMIM to DOID association may have a 0.75 probability of
     * being a SubClassOf
     * 
     * At the same time, the OMIM class may be entailed to be a SubClassOf,
     * based on axioms added by a curator (in this case, for MonDO, the axioms
     * may come from the omimclusters ontology).
     * 
     * This method will find entailed edges - these can then be used by the calling
     * code to alter the probabilistic graph
     * 
     * @param pg
     * @param reasoner
     * @return Any edges in the PrGraph for which the relationship between (s,t) can be entailed
     */
    public Set<ProbabilisticEdge> findEntailedProbabilisticEdges(ProbabilisticGraph pg, OWLReasoner reasoner) {
        Set<ProbabilisticEdge> rmEdges = new HashSet<>();
        for (ProbabilisticEdge e : pg.getProbabilisticEdges()) {
            OWLClass s = e.getSourceClass();
            OWLClass t = e.getTargetClass();
            if (reasoner.getEquivalentClasses(s).contains(t) ||
                    reasoner.getSuperClasses(s, false).containsEntity(t) ||
                    reasoner.getSuperClasses(t, false).containsEntity(s)) {
                rmEdges.add(e);
            }
        }
        return rmEdges;
    }

    /**
     * Computes the {@link ProbabilisticGraph.setProbabilityIndex()}
     * 
     * NOTE: if the probabilistic edge list changes, this must be recalculated
     * 
     * @param probabilisticGraph
     */
    public void OLDcalculateEdgeProbabilityMatrix(ProbabilisticGraph probabilisticGraph) {

        List<ProbabilisticEdge> prEdges = probabilisticGraph.getProbabilisticEdges();
        int N = prEdges.size();

        // both indices are keyed by the index of the prEdge list;
        // TODO - less dumb way of doing this
        OWLAxiom[][] axiomIndex = new OWLAxiom[N][4];
        double[][] probabilityIndex = new double[N][4];

        Map<OWLAxiom, Double> axiomPriorProbabilityMap = new HashMap<>();

        OWLDataFactory df = getOWLDataFactory();

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
        probabilisticGraph.setAxiomIndex(axiomIndex);
        probabilisticGraph.setProbabilityIndex(probabilityIndex);
        probabilisticGraph.setAxiomPriorProbabilityMap(axiomPriorProbabilityMap);
    }

    /**
     * Extract a probabilistic graph sub-module
     * 
     * @param seed node
     * @return probabilistic graph
     * @throws OWLOntologyCreationException 
     */
    public ProbabilisticGraph getProbabilisticGraphModule(Node<OWLClass> n) throws OWLOntologyCreationException {

        Set<OWLClass> clzs = n.getEntities();
        Set<ProbabilisticEdge> edges = new HashSet<ProbabilisticEdge>();
        Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();

        // extract H-graph module
        // TODO: improve efficiency
        for (ProbabilisticEdge e : probabilisticGraph.getProbabilisticEdges()) {
            if (clzs.contains(e.getSourceClass()) &&
                    clzs.contains(e.getTargetClass())) {
                edges.add(e);
            }
        }

        // EXPERIMENTAL:
        // include full SLME module; this slows down reasoning
        if (false) {
            ModuleType type = ModuleType.BOT;

            SyntacticLocalityModuleExtractor extractor =
                    new SyntacticLocalityModuleExtractor(
                            getOWLOntologyManager(),
                            sourceOntology,
                            type);
            Set<OWLEntity> entities = 
                    clzs.stream().map(x -> x).collect(Collectors.toSet());
            OWLOntology slmeOntology = getOWLOntologyManager().createOntology(
                    extractor.extract(entities),
                    IRI.create("http://foo.org"));
            Set<OWLAxiom> extractedAxioms = slmeOntology.getAxioms().stream().filter(x -> x.isLogicalAxiom()).collect(Collectors.toSet());
            axioms.addAll(extractedAxioms);
            getOWLOntologyManager().removeOntology(slmeOntology);
            LOG.info("SLME: "+extractedAxioms);

        }

        // extract A-graph module
        for (OWLSubClassOfAxiom ax : sourceOntology.getAxioms(AxiomType.SUBCLASS_OF)) {
            if (clzs.contains(ax.getSubClass()) &&
                    clzs.contains(ax.getSuperClass())) {
                axioms.add(ax);
            }       
        }
        for (OWLDisjointClassesAxiom ax : sourceOntology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
            for ( OWLClassExpression x : ax.getClassExpressions() ) {
                if (clzs.contains(x)) {
                    axioms.add(ax);
                    LOG.info(" ADDING: "+ax);
                    break;
                }
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
        sg.setProbabilisticEdges(new ArrayList<ProbabilisticEdge>(edges));
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

    /**
     * Reduce the edge size of the clique by eliminating a single probabilistic edge,
     * by assuming it to be true
     * 
     * Uses greedy algorithm:
     * 
     * Finds the probabilistic edge E that has a state with the highest probability, and
     * assumes it is true (regardless of how this effects the joint probability),
     * translating E from a probabilistic edge to a logical edge.
     * 
     * If E is invalid it is added to the unsatisfiable list, and the next best one
     * is chosen
     * 
     * @param sg
     * @param addedLogicalAxioms - accumulates all logical axioms added
     * @param reasoner 
     * @param unsatisfiableAxioms - accumulates all unsatisfiable axioms
     * @return Pr[reducedState]
     */
    public Double reduceClique(ProbabilisticGraph sg, Set<OWLAxiom> addedLogicalAxioms, OWLReasoner reasoner, 
            Set<OWLAxiom> unsatisfiableAxioms, int recursionDepth) {
        LOG.info("Reducing clique, depth="+recursionDepth+", Unsat:"+render(unsatisfiableAxioms));
        Double pr = 1.0;
        List<ProbabilisticEdge> newEdges = new ArrayList<ProbabilisticEdge>();
        int maxi = -1;
        double maxp = 0.0;
        OWLAxiom bestAxiom = null;
        //LOG.info("FINDING BEST AXIOM FROM : "+sg.getProbabilisticEdges().size());
        for (int i=0; i<sg.getProbabilisticEdges().size(); i++) {

            double[] prt = sg.getProbabilityIndex()[i];
            for (int j=0; j<4; j++) {
                if (prt[j] > maxp) {
                    OWLAxiom candidateAxiom = sg.getAxiomIndex()[i][j];
                    //LOG.info("Testing if "+candidateAxiom +" in "+unsatisfiableAxioms.size());
                    if (!unsatisfiableAxioms.contains(candidateAxiom)) {
                        //LOG.info("  BEST SO FAR: "+candidateAxiom);
                        maxp = prt[j];
                        maxi = i;
                        bestAxiom = candidateAxiom;
                    }
                    else {
                        //LOG.info("DISCOUNTING!");
                    }
                }
            }
        }
        if (maxi == -1) {
            LOG.info("Greedy algorithm leads to unsatisfiable state");
            return 0.0;
        }

        if (bestAxiom == null) {
            // bestAxiom can be null under two circumstances:
            //  1. the next most likely edge probability is for the 00 case between two classes
            //  2. there are no possibilities that lead to a coherent ontology.
            //
            // in the case of the former, the candidate ontology does not change,
            // so we do not need to run additional validity tests;
            // we account for the second case above (maxi==-1)

        }
        else {
            
            // add most likely candidate axiom to ontology
            OWLOntology ont = reasoner.getRootOntology();
            ont.getOWLOntologyManager().addAxiom(ont, bestAxiom);
            reasoner.flush();
            
            boolean isValid = testForValidity(bestAxiom, ont.getClassesInSignature(), reasoner);
            if (isValid) {
                for (OWLAxiom ax : addedLogicalAxioms) {
                    isValid = testForValidity(ax, ont.getClassesInSignature(), reasoner);
                    if (!isValid) {
                        break;
                    }
                }
            }
            if (!isValid) {
                LOG.info("Eliminating invalid axiom: "+render(bestAxiom)+ " Pr="+maxp);
                unsatisfiableAxioms.add(bestAxiom);
                ont.getOWLOntologyManager().removeAxiom(ont, bestAxiom);
                // recursion; call depth limited by number of axioms
                return reduceClique(sg, addedLogicalAxioms, reasoner,
                        unsatisfiableAxioms, recursionDepth+1);
            }
        }

        pr *= maxp;
        if (bestAxiom != null) {
            addedLogicalAxioms.add(bestAxiom);
        }
        ProbabilisticEdge e = sg.getProbabilisticEdges().get(maxi);
        newEdges.add(e);
        LOG.info("Translating Probabilistic Edge: " + e + " ==> LOGICAL EDGE: "+render(bestAxiom)+" p="+maxp);
        if (bestAxiom != null) {
            sg.getLogicalEdges().add(bestAxiom); // translate the probabilistic edge to a logical edge
            sg.getProbabilisticEdgeReplacementMap().put(bestAxiom, e);
            Set<ProbabilisticEdge> rmEdges = findEntailedProbabilisticEdges(sg, reasoner);
            rmEdges.remove(e); // will already be removed
            if (rmEdges.size() >0) {
                LOG.info("  Eliminating entailed edges: "+rmEdges);
                // TODO: get join probability of all axioms 
                if (isExperimental) {
                    LOG.error("TODO: calc probability");
                    sg.getProbabilisticEdges().removeAll(rmEdges);
                }
            }
        }
        else {
            sg.getEliminatedEdges().add(e);
        }
        sg.getProbabilisticEdges().remove(e);
        return pr;
    }

    public String render(ProbabilisticEdge e) {
        return renderer.render(e.getSourceClass()) +" -> "+renderer.render(e.getTargetClass()) +
                " PT: "+e.getProbabilityTable();
    }

    public String render(OWLObject obj) {
        if (obj == null)
            return "NULL";
        return renderer.render(obj);
    }

    public String render(Set<? extends OWLObject> objs) {
        return objs.stream().map(obj -> render(obj)).collect(Collectors.joining(", "));
    }

}
