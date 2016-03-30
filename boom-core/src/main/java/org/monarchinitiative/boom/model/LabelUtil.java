package org.monarchinitiative.boom.model;

import java.util.Collection;
import java.util.List;

import org.monarchinitiative.boom.io.LabelProvider;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * TODO - switch to using {@link LabelProvider}
 * 
 * @author cjm
 *
 */
public class LabelUtil {

	public static String render(OWLAxiom axiom, OWLOntology ontology) {
		if (axiom instanceof OWLSubClassOfAxiom) {
			OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom)axiom;
			return getIdLabel((OWLClass) sca.getSubClass(), ontology) + " < " + 
			getIdLabel((OWLClass) sca.getSuperClass(), ontology);
		}
		else if (axiom instanceof OWLEquivalentClassesAxiom) {
			List<OWLClassExpression> xs = ((OWLEquivalentClassesAxiom)axiom).getClassExpressionsAsList();
			return getIdLabel((OWLClass) xs.get(0), ontology) + " == " +
			getIdLabel((OWLClass) xs.get(1), ontology);
		}
		else {
			return axiom.toString();
		}
	}

	public static String getIdLabel(OWLClass c, OWLOntology ontology) {
		return c.getIRI().getFragment() + " " + getLabel(c, ontology);
	}

	public static String getLabel(OWLClass c, OWLOntology ontology) {
		OWLAnnotationProperty p = ontology.getOWLOntologyManager().getOWLDataFactory().getRDFSLabel();
		Collection<OWLAnnotation> anns = EntitySearcher.getAnnotations(c, ontology);
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral && a.getProperty().equals(p)) {
				return ((OWLLiteral)a.getValue()).getLiteral().toString();
			}
		}
		return "";
	}


}
