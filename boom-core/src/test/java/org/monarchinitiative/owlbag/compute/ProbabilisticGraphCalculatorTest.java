package org.monarchinitiative.owlbag.compute;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.monarchinitiative.boom.compute.ProbabilisticGraphCalculator;
import org.monarchinitiative.boom.io.OWLLoader;
import org.monarchinitiative.boom.io.ProbabilisticGraphParser;
import org.monarchinitiative.boom.model.CliqueSolution;
import org.monarchinitiative.boom.model.ProbabilisticGraph;
import org.monarchinitiative.boom.runner.MarkdownRunner;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.monitoring.runtime.instrumentation.common.com.google.common.io.Resources;

public class ProbabilisticGraphCalculatorTest {

	private static Logger LOG = Logger.getLogger(ProbabilisticGraphCalculatorTest.class);

	OWLLoader loader = new OWLLoader();
	OWLOntology ontology;

	@Test
	public void testBasic() throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		// fake ontology. fake OMIM IDs in flat hierarchy.
		// Two additional hierarchies, X and Y

		runUsingResources("basic.obo", "ptable-basic.tsv", "basic-resolved.owl",
				// all OMIMs are mapped to Z:3, we expect these to be resolved as SubClassOfs.
				// note this also tests heuristic clique-breaking
				subclass("OMIM_001", "Z_3"),
				subclass("OMIM_002", "Z_3"),
				subclass("OMIM_003", "Z_3"),
				subclass("OMIM_004", "Z_3"),
				subclass("OMIM_005", "Z_3"),
				subclass("OMIM_006", "Z_3"),
				subclass("OMIM_007", "Z_3"),
				subclass("OMIM_008", "Z_3"),
				subclass("OMIM_009", "Z_3"),
				subclass("OMIM_010", "Z_3"),
				// note that even through the prior for equivalence here is high,
				// it should be resolved as SubClassOf, when the full network is considered
				subclass("OMIM_010", "X_3"),

				// the priors for equivalence for all 3 Zs to this one Y is high;
				// however, maximally one can be equivalent to be consistent.
				subclass("Z_2b1a", "Y_2b1"),
				subclass("Z_2b1b", "Y_2b1"),
				equiv("Z_2b1c", "Y_2b1"),  // prior for equivalence is slightly higher

				subclass("Z_2a", "X_2"),
				equiv("Y_1", "X_1"),
				equiv("Y_2", "X_2"),
				equiv("Y_2", "Z_2"),
				equiv("X_3", "Z_3")

				);
	}

	@Test
	public void testReciprocalConflict() throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		// Pr(X1<Y1) = 0.9 [row 1]
		// Pr(X1<Y1) = 0.05 [row 2]

		Set<CliqueSolution> solns = 
				runUsingResources("basic.obo", "ptable-reciprocal-conflict.tsv", "reciprocal-conflict-resolved.owl",
						subclass("X_1", "Y_1")
						);
		assertEquals(1, solns.size());
		CliqueSolution s = solns.iterator().next();
		assertTrue("low confidence expected", s.confidence < 0.1);
	}

	@Test
	public void testUnsatisfiable() throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		// Pr(X1<Y1) = 0.9 [row 1]
		// Pr(X1<Y1) = 0.05 [row 2]

		Set<CliqueSolution> solns = 
				runUsingResources("basic.obo", "ptable-unsatisfiable.tsv", "unsatisfiable-resolved.owl"
						);
		assertEquals(1, solns.size());
		CliqueSolution s = solns.iterator().next();
		assertTrue("this clique has no solution", !s.solved);
	}

	@Test
	public void testOneSolution() throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		// Pr(X1<Y1) = 0.9 [row 1]
		// Pr(X1<Y1) = 0.05 [row 2]

		Set<CliqueSolution> solns = 
				runUsingResources("basic.obo", "ptable-one-solution.tsv", "one-solution-resolved.owl"
						);
		assertEquals(1, solns.size());
		CliqueSolution s = solns.iterator().next();
		assertEquals("this clique has a single solution, which is to reject the proposed axiom",
				0, s.axioms.size());
	}

	@Test
	public void testFalsePositive() throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		// in this test, OMIM:1xx is aligned with Y
		// and OMIM:2xx is aligned with Z
		// fake join point
		// **OMIM:101	Z:2b1c	0.8	0.08	0.1	0.02

		Set<CliqueSolution> solns = 
				runUsingResources("basic-fp.obo", "ptable-false-positive.tsv", "fp-resolved.owl",
						subclass("OMIM_101", "Y_1a1"),
						subclass("OMIM_101", "X_2b")
						);
		assertEquals(3, solns.size());
	}

	@Test
	public void testFalsePositiveNoTieBreaker() throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		// same as previous test, we have a false link
		//   OMIM:101	Z:2b1c	0.8	0.08	0.1	0.02
		// but we lack X:2b to break the tie, resulting in 101 going into its own clique

		Set<CliqueSolution> solns = 
				runUsingResources("basic-fp.obo", "ptable-false-positive2.tsv", "fp-resolved.owl"
						);
		assertEquals(4, solns.size());
	}


	public IRI getIRI(String c) {
		return IRI.create("http://purl.obolibrary.org/obo/"+c);
	}

	public OWLDataFactory df() {
		return loader.getOWLOntologyManager().getOWLDataFactory();
	}



	public OWLAxiom equiv(String c1, String c2) {
		return df().getOWLEquivalentClassesAxiom(
				df().getOWLClass(getIRI(c1)),
				df().getOWLClass(getIRI(c2)));

	}
	public OWLAxiom subclass(String c1, String c2) {
		return df().getOWLSubClassOfAxiom(
				df().getOWLClass(getIRI(c1)),
				df().getOWLClass(getIRI(c2)));

	}

	public boolean contains(Set<CliqueSolution> cliques, OWLAxiom ea) {

		for (CliqueSolution c : cliques) {
			for (OWLAxiom a : c.axioms) {
				if (a.equals(ea))
					return true;
			}
		}
		return false;
	}

	public Set<CliqueSolution> runUsingResources(String ontFile, String ptableFile, String outpath,
			OWLAxiom... expectedAxioms) throws OWLOntologyCreationException, OBOFormatParserException, IOException, OWLOntologyStorageException {
		Set<CliqueSolution> cliques =
				runUsingPaths(Resources.getResource(ontFile).getFile(),
						getResource(ptableFile).getAbsolutePath(),
						"target/" + outpath
						);
		for (OWLAxiom a : expectedAxioms)  {
			assertTrue("does not contain: "+a,
					contains(cliques,a));
		}

		return cliques;
	}

	public Set<CliqueSolution> runUsingPaths(String ontFile, String ptablePath, String outpath) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		LOG.info("ONT: "+ontFile);
		LOG.info("PROBS: "+ptablePath);
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
		ontology = loader.loadOWL(ontFile);
		ProbabilisticGraphParser parser = 
				new ProbabilisticGraphParser(ontology);
		ProbabilisticGraph pg = 
				parser.parse(ptablePath);
		MarkdownRunner mdr = new MarkdownRunner(ontology, pg);
		ProbabilisticGraphCalculator pgp = new ProbabilisticGraphCalculator(ontology);
		//pgp.setReasonerFactory(new Hermi);
		pgp.setMaxProbabilisticEdges(5);
		Set<CliqueSolution> rpts = mdr.runAll(pgp);

		Gson w = new GsonBuilder().
				setPrettyPrinting().
				excludeFieldsWithoutExposeAnnotation().
				serializeSpecialFloatingPointValues().
				create();
		String s = w.toJson(rpts);
		System.out.println(s);

		for (CliqueSolution cs : rpts) {
			ontology.getOWLOntologyManager().addAxioms(ontology, cs.axioms);
			for (OWLAxiom a : cs.axioms) {
				System.err.println(pgp.render(a));
			}
		}



		File outfile = new File(outpath);
		FileOutputStream os = new FileOutputStream(outfile);
		//IOUtils.write("FOO", os);
		System.err.println("Saving to "+outfile+" "+os+" "+ontology.getAxiomCount());
		ontology.getOWLOntologyManager().saveOntology(ontology, new RDFXMLDocumentFormat(), os);
		os.close();
		return rpts;

	}


	protected static File getResource(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO replace this with a mechanism not relying on the relative path
		File file = new File("src/test/resources/"+name);
		assertTrue("Requested resource does not exists: "+file, file.exists());
		return file;
	}



}
