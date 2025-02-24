package it.unibz.inf.ontop.cli;


import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.OptionType;
import com.github.rvesse.airline.annotations.help.BashCompletion;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.github.rvesse.airline.help.cli.bash.CompletionBehaviour;

abstract class OntopMappingOntologyRelatedCommand extends AbstractOntopCommand implements OntopCommand {

    @Option(type = OptionType.COMMAND, name = {"-t", "--ontology"}, title = "ontology file",
            description = "OWL ontology file")
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String owlFile;

    @Option(type = OptionType.COMMAND, name = {"-m", "--mapping"}, title = "mapping file",
            description = "Mapping file in R2RML (.ttl) or in Ontop native format (.obda)")
    @Required
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String mappingFile;

    @Option(type = OptionType.COMMAND, name = {"-c", "--constraint"}, title = "constraint file",
            description = "User-supplied DB constraint file")
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String constraintFile;

    @Option(type = OptionType.COMMAND, name = {"-d", "--db-metadata"}, title = "db-metadata file",
            description = "User-supplied db-metadata file")
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String dbMetadataFile;

    @Option(type = OptionType.COMMAND, name = {"-l", "--lenses", "-v", "--ontop-views"}, title = "Lenses file",
            description = "User-supplied lenses file. Lenses were formerly named Ontop views.")
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String ontopLensesFile;

    @Option(type = OptionType.COMMAND, name = {"--sparql-rules"}, title = "SPARQL rules file",
            description = "User-supplied SPARQL rules file")
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String sparqlRulesFile;

    @Option(type = OptionType.COMMAND, name = {"-x", "--xml-catalog"}, title = "xml catalog file",
            description = "XML Catalog file (e.g. catalog-v001.xml generated by Protege) for redirecting ontologies imported by owl:imports")
    @BashCompletion(behaviour = CompletionBehaviour.FILENAMES)
    String xmlCatalogFile;

    @Option(type = OptionType.COMMAND, name = {"--enable-annotations"},
            description = "enable annotation properties defined in the ontology. Default: false")
    public boolean enableAnnotations = false;

    protected boolean isR2rmlFile(String mappingFile) {
        return !mappingFile.endsWith(".obda");
    }
}
