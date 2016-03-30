package org.monarchinitiative.boom.io;

import java.io.File;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.base.Preconditions;

/**
 * Object for loading OWL ontologies into a {@link BMKnowledgeBase}
 * 
 * Note that a KB consists of classes and individuals, both of which can be loaded
 * from an ontology
 * 
 * @author cjm
 *
 */
public class OWLLoader {
	private Logger LOG = Logger.getLogger(OWLLoader.class);

	OWLOntologyManager manager;
	OWLOntology owlOntology;

	/**
	 * @param iri
	 * @return OWL Ontology 
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology loadOWL(IRI iri) throws OWLOntologyCreationException {
		return getOWLOntologyManager().loadOntology(iri);
	}

	/**
	 * @param file
	 * @return OWL Ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology loadOWL(File file) throws OWLOntologyCreationException {
		IRI iri = IRI.create(file);
		return getOWLOntologyManager().loadOntologyFromOntologyDocument(iri);	    
	}

	/**
	 * Loads an OWL ontology from a URI or file
	 * 
	 * @param path
	 * @return OWL Ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology loadOWL(String path) throws OWLOntologyCreationException {
		if (path.startsWith("http")) {
			return loadOWL(IRI.create(path));
		}
		else {
			File file = new File(path);
			return loadOWL(file);
		}
	}

	/**
	 * @param iri
	 * @throws OWLOntologyCreationException
	 */
	public void load(IRI iri) throws OWLOntologyCreationException {
		owlOntology = getOWLOntologyManager().loadOntology(iri);
		Preconditions.checkNotNull(owlOntology);	    
	}

	/**
	 * @param file
	 * @throws OWLOntologyCreationException
	 */
	public void load(File file) throws OWLOntologyCreationException {
		owlOntology = loadOWL(file);
		Preconditions.checkNotNull(owlOntology);	    
	}



	/**
	 * Loads an OWL ontology from a URI or file
	 * 
	 * @param path
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology load(String path) throws OWLOntologyCreationException {
		owlOntology = loadOWL(path);
		Preconditions.checkNotNull(owlOntology);
		return owlOntology;
	}


	public OWLOntologyManager getOWLOntologyManager() {
		if (manager == null)
			manager = OWLManager.createOWLOntologyManager();
		return manager;
	}

	
}
