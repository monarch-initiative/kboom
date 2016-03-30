package org.monarchinitiative.boom.io;

import org.semanticweb.owlapi.model.IRI;

public class IDTools {

	public static IRI getIRIByIdentifier(String id) {
		// TODO - use JSON-LD library and context
		return IRI.create("http://purl.obolibrary.org/obo/"+id.replace(":", "_"));
	}
}
