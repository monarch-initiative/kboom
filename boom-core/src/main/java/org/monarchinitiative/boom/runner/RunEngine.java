package org.monarchinitiative.boom.runner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.monarchinitiative.boom.compute.ProbabilisticGraphCalculator;
import org.monarchinitiative.boom.io.IDTools;
import org.monarchinitiative.boom.io.OWLLoader;
import org.monarchinitiative.boom.io.ProbabilisticGraphParser;
import org.monarchinitiative.boom.model.CliqueSolution;
import org.monarchinitiative.boom.model.ProbabilisticGraph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RunEngine {

    @Parameter(names = { "-v",  "--verbose" }, description = "Level of verbosity")
    private Integer verbose = 1;

    @Parameter(names = { "-o", "--out"}, description = "output ontology file")
    private String outpath;

    @Parameter(names = { "-c", "--classes"}, description = "Run only on cliques containing these classes")
    private List<String> classIds;

    @Parameter(names = { "-r", "--roots"}, description = "Run only on cliques traceable to these root superclasses")
    private List<String> rootClassIds;

    @Parameter(names = { "-j", "--json"}, description = "output json report file")
    private String jsonOutPath;

    @Parameter(names = { "-t", "--table"}, description = "Path to TSV of probability table")
    private String ptableFile;

    @Parameter(names = { "-m", "--markdown"}, description = "Path to output markdown file")
    private String mdOutputFile;

    @Parameter(names = { "-d", "--dir"}, description = "directory")
    private String dir = ".";

    @Parameter(names = { "-n", "--new"}, description = "Make new ontology")
    private Boolean isMakeNewOntology = false;

    @Parameter(names = { "--max" }, description = "Maximumum number of probabilistic edges in clique")
    private Integer maxProbabilisticEdges = 9;

    @Parameter(names = { "--splitSize" }, description = "Threshold at which heuristic procedure is used to split clique")
    private Integer cliqueSplitSize = 6;

    @Parameter(names = { "--experimental" }, description = "Experimental")
    private Boolean isExperimental = false;

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help = false;

    @Parameter(description = "Files")
    private List<String> files = new ArrayList<>();



    public static void main(String ... args) throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
        RunEngine main = new RunEngine();
        JCommander jCommander = new JCommander(main, args);
        if (main.help) {
            jCommander.usage();
            return;
        }
        main.run();
    }

    public void run() throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
        Logger.getLogger("org.semanticweb.elk").setLevel(Level.OFF);

        //System.out.printf("%s %d %s", groups, verbose, debug);
        OWLLoader loader = new OWLLoader();
        OWLOntology sourceOntology;
        sourceOntology  = loader.load(files.get(0));
        ProbabilisticGraphParser parser = 
                new ProbabilisticGraphParser(sourceOntology);

        ProbabilisticGraph pg = 
                parser.parse(ptableFile);
        MarkdownRunner mdr = new MarkdownRunner(sourceOntology, pg);

        ProbabilisticGraphCalculator pgc = new ProbabilisticGraphCalculator(sourceOntology);

        pgc.setProbabilisticGraph(pg);
        if (maxProbabilisticEdges != null)
            pgc.setMaxProbabilisticEdges(maxProbabilisticEdges);
        if (cliqueSplitSize != null)
            pgc.setCliqueSplitSize(cliqueSplitSize);
        pgc.isExperimental = isExperimental;
        if (isExperimental)
            System.out.println("EXPERIMENTAL MODE");

        if (classIds != null && classIds.size() > 0) {
            Set<OWLClass> filterOnClasses = 
                    classIds.stream().map( s -> 
                    pgc.getOWLDataFactory().getOWLClass(IDTools.getIRIByIdentifier(s))).collect(Collectors.toSet());
            pgc.setFilterOnClasses(filterOnClasses);
        }
        if (rootClassIds != null && rootClassIds.size() > 0) {
            Set<OWLClass> roots = 
                    rootClassIds.stream().map( s -> 
                    pgc.getOWLDataFactory().getOWLClass(IDTools.getIRIByIdentifier(s))).collect(Collectors.toSet());
            pgc.setFilterOnRootClasses(roots);
        }

        if (dir != null) {
            if (mdOutputFile == null) {
                mdOutputFile = dir + "/kboom-report.md";
            }
            if (jsonOutPath == null) {
                jsonOutPath = dir + "/kboom-report.json";
            }
            mdr.imageFilesPath = dir + "/images/";
        }

        Set<CliqueSolution> rpts = pgc.solveAllCliques();

        if (mdOutputFile != null) {
            FileUtils.writeStringToFile(new File(mdOutputFile), mdr.render(rpts));
        }

        OWLOntology outputOntology;
        if (isMakeNewOntology) {
            outputOntology = sourceOntology.getOWLOntologyManager().createOntology();
            for (CliqueSolution cs : rpts) {
                sourceOntology.getOWLOntologyManager().addAxioms(outputOntology, cs.axioms);
            }
        }
        else {
            outputOntology = pgc.getSourceOntology();
        }


        Gson w = new GsonBuilder().
                setPrettyPrinting().
                serializeSpecialFloatingPointValues().
                excludeFieldsWithoutExposeAnnotation().
                create();
        String s = w.toJson(rpts);
        if (jsonOutPath == null)
            System.out.println(s);
        else
            FileUtils.writeStringToFile(new File(jsonOutPath), s);

        if (outpath == null)
            outpath = "foo.owl";

        File file = new File(outpath);
        sourceOntology.getOWLOntologyManager().saveOntology(outputOntology, IRI.create(file));

    }
}
