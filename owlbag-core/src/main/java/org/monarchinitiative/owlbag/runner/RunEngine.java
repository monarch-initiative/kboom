package org.monarchinitiative.owlbag.runner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.monarchinitiative.owlbag.compute.ProbabilisticGraphCalculator;
import org.monarchinitiative.owlbag.io.OWLLoader;
import org.monarchinitiative.owlbag.io.ProbabilisticGraphParser;
import org.monarchinitiative.owlbag.model.CliqueSolution;
import org.monarchinitiative.owlbag.model.ProbabilisticGraph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RunEngine {

	@Parameter(names = { "-v",  "--verbose" }, description = "Level of verbosity")
	private Integer verbose = 1;

	@Parameter(names = { "-o", "--out"}, description = "output ontology file")
	private String outpath;

	@Parameter(names = { "-j", "--json"}, description = "output json report file")
	private String jsonOutPath;

	@Parameter(names = { "-t", "--table"}, description = "Path to TSV of probability table")
	private String ptableFile;

	@Parameter(names = { "-n", "--new"}, description = "Make new ontology")
	private Boolean isMakeNewOntology = false;

	@Parameter(description = "Files")
	private List<String> files = new ArrayList<>();


	public static void main(String ... args) throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
		RunEngine main = new RunEngine();
		new JCommander(main, args);
		main.run();
	}

	public void run() throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
		
		//System.out.printf("%s %d %s", groups, verbose, debug);
		OWLLoader loader = new OWLLoader();
		OWLOntology sourceOntology;
		sourceOntology  = loader.load(files.get(0));
		ProbabilisticGraphParser parser = 
				new ProbabilisticGraphParser(sourceOntology);

		ProbabilisticGraph pg = 
				parser.parse(ptableFile);

		ProbabilisticGraphCalculator pgc = new ProbabilisticGraphCalculator(sourceOntology);
		
		pgc.setGraph(pg);
		Set<CliqueSolution> rpts = pgc.breakCliques();
		
		OWLOntology outputOntology;
		if (isMakeNewOntology) {
			outputOntology = sourceOntology.getOWLOntologyManager().createOntology();
			for (CliqueSolution cs : rpts) {
				sourceOntology.getOWLOntologyManager().addAxioms(outputOntology, cs.axioms);
			}
		}
		else {
			outputOntology = pgc.getSourceOntology();
		}

		
		Gson w = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
		String s = w.toJson(rpts);
		if (jsonOutPath == null)
			System.out.println(s);
		else
			FileUtils.writeStringToFile(new File(jsonOutPath), s);
		
		if (outpath == null)
			outpath = "foo.owl";
		
		File file = new File(jsonOutPath);
		sourceOntology.getOWLOntologyManager().saveOntology(outputOntology, IRI.create(file));
		


	}
}
