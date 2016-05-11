package org.monarchinitiative.boom.runner;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.monarchinitiative.boom.compute.ProbabilisticGraphCalculator;
import org.monarchinitiative.boom.io.CliqueSolutionDotWriter;
import org.monarchinitiative.boom.io.LabelProvider;
import org.monarchinitiative.boom.model.CliqueSolution;
import org.monarchinitiative.boom.model.LabelUtil;
import org.monarchinitiative.boom.model.ProbabilisticGraph;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
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
	OWLObjectRenderer renderer;
	
	public MarkdownRunner(OWLOntology ontology, ProbabilisticGraph pg) {
		super();
		this.ontology = ontology;
		this.pg = pg;
		pg.calculateEdgeProbabilityMatrix(ontology.getOWLOntologyManager().getOWLDataFactory());
		
		LabelProvider provider = new LabelProvider(ontology);
		renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
		renderer.setShortFormProvider(provider);

	}



	public Set<CliqueSolution> runAll() throws OWLOntologyCreationException, IOException {
		ProbabilisticGraphCalculator pgp = new ProbabilisticGraphCalculator(ontology);
		return runAll(pgp);
	}
	
	public Set<CliqueSolution> runAll(ProbabilisticGraphCalculator pgp ) throws OWLOntologyCreationException, IOException {
		
		pgp.setProbabilisticGraph(pg);
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
		CliqueSolutionDotWriter dw = new CliqueSolutionDotWriter(cs, ontology, pg);
		String png;
		try {
			png = dw.renderToFile(imageFilesPath);
			String header = "\n\n## " + cs.cliqueId + "\n\n";
			String prStats = " * __Pr(G)__=" + cs.probability+" CONFIDENCE=" + cs.confidence+" Success:" + cs.solved;
			String stats = " * __SIZE__=" + cs.size+" ("+cs.axioms.size()+" new axioms) ";
			String link = "[img]("+png+")";
			//String axioms = cs.axioms.stream().map( (ax) -> " * " + LabelUtil.render(ax, ontology) + "\n" ).collect(Collectors.joining(""));
			String messages = cs.messages.stream().map( (m) -> " * MESSAGE: " + m + "\n" ).collect(Collectors.joining(""));
			String members = cs.classes.stream().map( (c) -> " * MEMBER: " + renderer.render(c) + "\n" ).collect(Collectors.joining(""));
			String axioms = cs.axioms.stream().map( (ax) -> " * " + 
					renderer.render(ax) + 
					" Pr= " + pg.getAxiomPriorProbability(ax) +
					"\n" ).collect(Collectors.joining(""));
			
			return header + prStats + "\n" + stats + "\n" + link + "\n" + members + messages + axioms;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}

	}

}
