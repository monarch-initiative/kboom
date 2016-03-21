package org.monarchinitiative.owlbag.runner;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.monarchinitiative.owlbag.compute.ProbabilisticGraphCalculator;
import org.monarchinitiative.owlbag.io.CliqueSolutionDotWriter;
import org.monarchinitiative.owlbag.model.CliqueSolution;
import org.monarchinitiative.owlbag.model.LabelUtil;
import org.monarchinitiative.owlbag.model.ProbabilisticGraph;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Runs a PGP and writes markdown report per-clique;
 * also generates dot and png
 * 
 * @author cjm
 *
 */
public class MarkdownRunner {
	
	OWLOntology ontology;
	ProbabilisticGraph pg;
	String imageFilesPath = "target/img-";
	
	public MarkdownRunner(OWLOntology ontology, ProbabilisticGraph pg) {
		super();
		this.ontology = ontology;
		this.pg = pg;
	}



	public Set<CliqueSolution> runAll() throws OWLOntologyCreationException, IOException {
		ProbabilisticGraphCalculator pgp = new ProbabilisticGraphCalculator(ontology);
		return runAll(pgp);
	}
	
	public Set<CliqueSolution> runAll(ProbabilisticGraphCalculator pgp ) throws OWLOntologyCreationException, IOException {
		
		pgp.setGraph(pg);
		Set<CliqueSolution> rpts = pgp.solveAllCliques();
		
		for (CliqueSolution cs : rpts) {
			System.out.println(render(cs));
			//CliqueSolutionDotWriter dw = new CliqueSolutionDotWriter(cs, ontology);
			//dw.renderToFile(path);
		}
		return rpts;
		
	}

	public String render(Set<CliqueSolution> rpts) {
		return rpts.stream().map( (cs) -> render(cs) ).collect(Collectors.joining());
	}
	
	public String render(CliqueSolution cs) {
		CliqueSolutionDotWriter dw = new CliqueSolutionDotWriter(cs, ontology);
		String png;
		try {
			png = dw.renderToFile(imageFilesPath);
			String header = "\n\n## " + cs.cliqueId + "\n\n";
			String prStats = " * __PR__=" + cs.probability+" CONFIDENCE=" + cs.confidence;
			String stats = " * __SIZE__=" + cs.size;
			String link = "[img]("+png+")";
			String axioms = cs.axioms.stream().map( (ax) -> " * " + LabelUtil.render(ax, ontology) + "\n" ).collect(Collectors.joining(""));
			
			return header + prStats + "\n" + stats + "\n" + link + "\n" + axioms;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}

	}

}
