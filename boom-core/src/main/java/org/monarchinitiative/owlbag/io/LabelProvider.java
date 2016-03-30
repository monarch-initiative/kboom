package org.monarchinitiative.owlbag.io;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class LabelProvider implements ShortFormProvider  {

	OWLOntology ontology;
	boolean hideIds = false;
	boolean quoteLabels = true;


	public LabelProvider(OWLOntology ont) {
		super();
		this.ontology = ont;
	}


	public String getShortForm(OWLEntity entity) {
		String label = getLabel(entity);
		if (label == null) {
			return getTruncatedId(entity);
		}
		else {
			if (hideIds) {
				return label;
			}
			return getTruncatedId(entity) + " "+ label;
		}
	}

	public String getLabel(OWLEntity entity) {

		for (OWLAnnotationAssertionAxiom a : ontology.getAnnotationAssertionAxioms(entity.getIRI())) {
			if (a.getProperty().isLabel()) {
				OWLAnnotationValue v = a.getValue();
				if (v instanceof OWLLiteral) {
					return ((OWLLiteral)v).getLiteral();
				}
			}
		}
		return null;
	}

	public String getTruncatedId(OWLEntity entity) {
		return entity.getIRI().getFragment();
	}


	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}



}