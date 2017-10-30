package org.monarchinitiative.boom.io;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;

public class IDTools {
	private static Logger LOG = Logger.getLogger(IDTools.class);
	
	public static IRI getIRIByIdentifier(String id) {
		// TODO - use JSON-LD library and context
	    if (id.startsWith("http")) {
	        return IRI.create(id);
	    }
	    else {
	        return IRI.create("http://purl.obolibrary.org/obo/"+id.replace(":", "_"));
	    }
	}
}
