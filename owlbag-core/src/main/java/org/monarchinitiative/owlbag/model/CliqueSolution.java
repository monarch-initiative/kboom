package org.monarchinitiative.owlbag.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

import com.google.gson.annotations.Expose;

/**
 * A set of consistent axioms that can break a clique, and is considered the most
 * likely based on a set of prior probabilities
 * 
 * @author cjm
 *
 */
public class CliqueSolution {
	
	@Expose public String cliqueId;
	@Expose public Set<String> members;
	@Expose public int size;
	@Expose public int numberOfProbabilisticEdges;
	@Expose public Double probability;
	@Expose public Double confidence;
	@Expose public Boolean solved;
	@Expose public List<String> messages = new ArrayList<String>();
	
	public Set<OWLClass> classes = new HashSet<OWLClass>();
	public Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
	public ProbabilisticGraph subGraph;
	
	@Expose public Set<String> axiomStrings;

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CliqueReport [cliqueId=" + cliqueId + ", members=" + members
				+ ", probability=" + probability + ", confidence=" + confidence
				+ ", axioms=" + axioms + "]";
	}
	
	

}
