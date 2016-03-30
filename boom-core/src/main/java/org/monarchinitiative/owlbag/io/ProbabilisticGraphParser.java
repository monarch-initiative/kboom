package org.monarchinitiative.owlbag.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.monarchinitiative.owlbag.model.ProbabilisticEdge;
import org.monarchinitiative.owlbag.model.ProbabilisticGraph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Parses probability tables
 * 
 * Format:
 * <code>
 * ClassA ClassB Pr(ASubClassOfB) Pr(BSubClassOfA) Pr(AEquivalentToB) Pr(ANotInLineageWithB)
 * </code>
 * 
 * Note that here SubClassOf denotes both proper (ie not equivalent) and entailed/ancestors
 * 
 * 
 * 
 * @author cjm
 *
 */
public class ProbabilisticGraphParser {

	private static Logger LOG = Logger.getLogger(ProbabilisticGraphParser.class);
	private OWLOntology ontology;

	public ProbabilisticGraphParser(OWLOntology ontology) {
		super();
		this.ontology = ontology;
	}

	public ProbabilisticGraph parse(String fn) throws IOException {
		File file = new File(fn);
		return parse(file);
	}
	
	public ProbabilisticGraph parse(File file) throws IOException {
		ProbabilisticGraph pg = new ProbabilisticGraph();
		List<String> lines = FileUtils.readLines(file, "UTF-8");
		OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
		for (String line : lines) {
			String[] vs = line.split("\\t");
			IRI siri = IDTools.getIRIByIdentifier(vs[0]);
			IRI tiri = IDTools.getIRIByIdentifier(vs[1]);
			OWLClass s = df.getOWLClass(siri);
			OWLClass t = df.getOWLClass(tiri);
			
			if (s == null || t == null) {
				LOG.error("MISSING: "+line+" S="+s+" T="+t);
				continue;
			}
			ProbabilisticEdge e = new ProbabilisticEdge(s, t, 
					Double.parseDouble(vs[2]),
					Double.parseDouble(vs[3]),
					Double.parseDouble(vs[4]),
					Double.parseDouble(vs[5]));
			pg.getEdges().add(e);
					
		}
		pg.collapseReciprocals();
		return pg;
	}

}
