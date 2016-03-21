package org.monarchinitiative.owlbag.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.monarchinitiative.owlbag.model.CliqueSolution;
import org.monarchinitiative.owlbag.model.LabelUtil;
import org.monarchinitiative.owlbag.model.ProbabilisticEdge;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * Write a clique as a dot file
 * 
 * @author cjm
 *
 */
public class CliqueSolutionDotWriter {

	private CliqueSolution cs;
	private OWLOntology ontology;


	public CliqueSolutionDotWriter(CliqueSolution cs, OWLOntology ontology) {
		super();
		this.cs = cs;
		this.ontology = ontology;
	}

	public String render() {
		return "digraph g {\n" + renderNodes() + 
				renderPriorLogicalEdges() + 
				renderPriorEdges() + 
				renderEdges() + 
				"}";
	}

	public String renderToFile(String dirname) throws IOException {
		String base = dirname + getId(cs.cliqueId);
		String fn = base + ".dot";
		String png = base + ".png";
		
		File f = new File(fn);
		FileUtils.writeStringToFile(f, render());
		// TODO - make path configurable
		Runtime.getRuntime().exec("/opt/local/bin/dot -Grankdir=BT -T png " + " -o " + png + " " + fn);
		return png;
	}

	


	private String renderNodes() {
		return cs.classes.stream().map( (OWLClass c) -> renderClass(c)).collect(Collectors.joining("\n"));
	}
	private String renderClass(OWLClass c) {
		return getId(c) + " [ label=\"" + getLabel(c) + "\" ];";
	}

	private String renderEdges() {
		return cs.axioms.stream().map( (OWLAxiom a) -> renderEdge(a, "blue")).collect(Collectors.joining("\n"));
	}
	private String renderPriorLogicalEdges() {
		return cs.subGraph.getLogicalEdges().stream().map( (OWLAxiom a) -> renderEdge(a, "yellow")).collect(Collectors.joining("\n"));
	}

	private String renderPriorEdges() {
		return cs.subGraph.getEdges().stream().map( (ProbabilisticEdge e) -> renderEdge(e)).collect(Collectors.joining("\n"));
	}

	private String renderEdge(ProbabilisticEdge e) {
		return renderEdge(
				getId(getId(e.getSourceClass())), 
				getId(getId(e.getTargetClass())), 
				"none",
				"dotted",
				"blue",
				1,
				""); // TODO
	}

	private String renderEdge(OWLAxiom ax, String color) {
		if (ax instanceof OWLSubClassOfAxiom) {
			OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom)ax;
			return renderEdge(
					getId((OWLClass) sca.getSubClass()), 
					getId((OWLClass) sca.getSuperClass()),
					"normal",
					"solid",
					color,
					10,
					"");
		}
		else if (ax instanceof OWLEquivalentClassesAxiom) {
			OWLEquivalentClassesAxiom eca = (OWLEquivalentClassesAxiom)ax;
			List<OWLClassExpression> xs = eca.getClassExpressionsAsList();
			return renderEdge(
					getId((OWLClass) xs.get(0)), 
					getId((OWLClass) xs.get(1)),
					"ediamond",
					"solid",
					"red",
					5,
					", arrowtail=ediamond, dir=both");
		}
		return null;
	}

	private String renderEdge(String s, String t, String arrowhead, 
			String style, String color, Integer penwidth, String extra) {
		
		return s + " -> " + t +" [ arrowhead = " + arrowhead +", penwidth="+penwidth+
				", color="+color+", style="+style+extra+"]\n";
	}

	private String getId(OWLClass c) {
		return c.getIRI().getFragment();
	}

	private String getId(String c) {
		return IRI.create(c).getFragment();
	}


	private String getLabel(OWLClass c) {
		String label = LabelUtil.getLabel(c, ontology);
		label = label.replaceAll(" ", "\n");
		label = label.replaceAll("\"", "'");
		return c.getIRI().getFragment() + " " + label;
	}

}
