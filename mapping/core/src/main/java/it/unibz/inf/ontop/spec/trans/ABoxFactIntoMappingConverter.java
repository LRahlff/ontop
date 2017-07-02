package it.unibz.inf.ontop.spec.trans;

import it.unibz.inf.ontop.iq.tools.ExecutorRegistry;
import it.unibz.inf.ontop.mapping.Mapping;
import it.unibz.inf.ontop.ontology.Ontology;
import it.unibz.inf.ontop.utils.UriTemplateMatcher;

public interface ABoxFactIntoMappingConverter {

    Mapping convert(Ontology ontology, ExecutorRegistry executorRegistry, boolean isOntologyAnnotationQueryingEnabled, UriTemplateMatcher uriTemplateMatcher);
}
