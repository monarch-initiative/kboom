package org.monarchinitiative.owlbag.io;

import org.semanticweb.owlapi.model.IRI;

public class IDTools {

	public static IRI getIRIByIdentifier(String id) {
		return IRI.create("http://purl.obolibrary.org/obo/"+id.replace(":", "_"));
	}
}
