package org.monarchinitiative.owlbag.compute;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import org.junit.Test;
import org.monarchinitiative.owlbag.io.OWLLoader;
import org.monarchinitiative.owlbag.io.ProbabilisticGraphParser;
import org.monarchinitiative.owlbag.model.CliqueSolution;
import org.monarchinitiative.owlbag.model.ProbabilisticGraph;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.monitoring.runtime.instrumentation.common.com.google.common.io.Resources;

public class ProbabilisticGraphCalculatorTest {
	
	OWLOntologyManager manager;

	@Test
	public void test() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		OWLLoader loader = new OWLLoader();
		OWLOntology ontology = loader.loadOWL(Resources.getResource("basic.obo").getFile());
		ProbabilisticGraphParser parser = 
				new ProbabilisticGraphParser(ontology);
		ProbabilisticGraph pg = 
				parser.parse(getResource("ptable-basic.tsv").getAbsolutePath());
		ProbabilisticGraphCalculator pgp = new ProbabilisticGraphCalculator(ontology);
		
		pgp.setGraph(pg);
		Set<CliqueSolution> rpts = pgp.breakCliques();
		Gson w = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
		String s = w.toJson(rpts);
		System.out.println(s);
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
