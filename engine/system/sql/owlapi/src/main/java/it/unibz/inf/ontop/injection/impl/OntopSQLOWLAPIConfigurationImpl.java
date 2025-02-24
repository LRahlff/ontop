package it.unibz.inf.ontop.injection.impl;

import it.unibz.inf.ontop.exception.OBDASpecificationException;
import it.unibz.inf.ontop.exception.InvalidOntopConfigurationException;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.injection.OntopStandaloneSQLSettings;
import it.unibz.inf.ontop.injection.impl.OntopMappingOntologyBuilders.OntopMappingOntologyOptions;
import it.unibz.inf.ontop.injection.impl.OntopMappingOntologyBuilders.StandardMappingOntologyBuilderFragment;
import it.unibz.inf.ontop.spec.OBDASpecification;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.util.Optional;


public class OntopSQLOWLAPIConfigurationImpl extends OntopStandaloneSQLConfigurationImpl
        implements OntopSQLOWLAPIConfiguration {


    private final OntopMappingOntologyConfigurationImpl mappingOWLConfiguration;

    OntopSQLOWLAPIConfigurationImpl(OntopStandaloneSQLSettings settings, OntopSQLOWLAPIOptions options) {
        super(settings, options.sqlOptions);
        mappingOWLConfiguration = new OntopMappingOntologyConfigurationImpl(settings, options.ontologyOptions);
    }

    @Override
    public OBDASpecification loadOBDASpecification() throws OBDASpecificationException {
        return loadSpecification(mappingOWLConfiguration::loadOntology);
    }

    @Override
    public Optional<OWLOntology> loadInputOntology() throws OWLOntologyCreationException {
        return mappingOWLConfiguration.loadInputOntology();
    }

    static class OntopSQLOWLAPIOptions {
        final OntopStandaloneSQLOptions sqlOptions;
        final OntopMappingOntologyOptions ontologyOptions;

        OntopSQLOWLAPIOptions(OntopStandaloneSQLOptions sqlOptions, OntopMappingOntologyOptions ontologyOptions) {
            this.sqlOptions = sqlOptions;
            this.ontologyOptions = ontologyOptions;
        }
    }

    protected static abstract class OntopSQLOWLAPIBuilderMixin<B extends OntopSQLOWLAPIConfiguration.Builder<B>>
            extends OntopStandaloneSQLBuilderMixin<B>
            implements OntopSQLOWLAPIConfiguration.Builder<B> {

        private final StandardMappingOntologyBuilderFragment<B> ontologyBuilderFragment;
        private boolean isOntologyDefined = false;

        protected OntopSQLOWLAPIBuilderMixin() {
            ontologyBuilderFragment = new StandardMappingOntologyBuilderFragment<>() {
                @Override
                protected B self() {
                    return OntopSQLOWLAPIBuilderMixin.this.self();
                }

                @Override
                protected void declareOntologyDefined() {
                    OntopSQLOWLAPIBuilderMixin.this.declareOntologyDefined();
                }
            };
        }

        @Override
        public B ontologyFile(@Nonnull String urlOrPath) {
            return ontologyBuilderFragment.ontologyFile(urlOrPath);
        }

        @Override
        public B xmlCatalogFile(@Nonnull String xmlCatalogFile) {
            return ontologyBuilderFragment.xmlCatalogFile(xmlCatalogFile);
        }

        @Override
        public B ontologyFile(@Nonnull URL url) {
            return ontologyBuilderFragment.ontologyFile(url);
        }

        @Override
        public B ontologyFile(@Nonnull File owlFile) {
            return ontologyBuilderFragment.ontologyFile(owlFile);
        }

        @Override
        public B ontologyReader(@Nonnull Reader reader) {
            return ontologyBuilderFragment.ontologyReader(reader);
        }

        protected final void declareOntologyDefined() {
            if (isOntologyDefined) {
                throw new InvalidOntopConfigurationException("Ontology already defined!");
            }
            isOntologyDefined = true;
        }

        protected final OntopSQLOWLAPIOptions generateSQLOWLAPIOptions() {
            OntopStandaloneSQLOptions standaloneSQLOptions = generateStandaloneSQLOptions();
            OntopMappingOntologyOptions mappingOntologyOptions = ontologyBuilderFragment.generateMappingOntologyOptions(
                    standaloneSQLOptions.mappingOptions.mappingSQLOptions.mappingOptions);

            return new OntopSQLOWLAPIOptions(standaloneSQLOptions, mappingOntologyOptions);
        }
    }


    public static class BuilderImpl extends OntopSQLOWLAPIBuilderMixin<BuilderImpl> {

        @Override
        public OntopSQLOWLAPIConfiguration build() {
            OntopStandaloneSQLSettings settings = new OntopStandaloneSQLSettingsImpl(generateProperties(), isR2rml());
            OntopSQLOWLAPIOptions options = generateSQLOWLAPIOptions();
            return new OntopSQLOWLAPIConfigurationImpl(settings, options);
        }

        @Override
        protected BuilderImpl self() {
            return this;
        }
    }
}
