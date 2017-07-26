package it.unibz.inf.ontop.docker.mysql;

/*
 * #%L
 * ontop-test
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.injection.OntopSystemConfiguration;
import it.unibz.inf.ontop.owlrefplatform.core.abox.MaterializationParams;
import it.unibz.inf.ontop.owlrefplatform.owlapi.MaterializedGraphOWLResultSet;
import it.unibz.inf.ontop.owlrefplatform.owlapi.OntopOWLAPIMaterializer;
import it.unibz.inf.ontop.rdf4j.MaterializationGraphQuery;
import it.unibz.inf.ontop.rdf4j.RDF4JMaterializer;
import junit.framework.TestCase;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.n3.N3Writer;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriter;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.WriterDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.*;
import java.util.function.Function;

public class RDF4JMaterializerCmdTest extends TestCase {

	private static final String ONTOLOGY_FILE_PATH = "src/test/resources/mysql/materializer/MaterializeTest.owl";
	/**
	 * Necessary for materialize large RDF graphs without
	 * storing all the SQL results of one big query in memory.
	 */
	private static boolean DO_STREAM_RESULTS = true;
	
	public void testModelN3() throws Exception {
		runRDF4JTestWithoutOntology("src/test/resources/mysql/materializer/materializeN3.N3",
				N3Writer::new);
	}
	
	public void testModelTurtle() throws Exception {
		runRDF4JTestWithoutOntology("src/test/resources/mysql/materializer/materializeTurtle.ttl",
				TurtleWriter::new);
	}

	public void testModelRdfXml() throws Exception {
		runRDF4JTestWithoutOntology("src/test/resources/mysql/materializer/materializeRdf.owl",
				RDFXMLWriter::new);
	}
	
	public void testModelOntoN3() throws Exception {
		runRDF4JTestWithOntology("src/test/resources/mysql/materializer/materializeN3.N3",
				N3Writer::new);
	}

	public void testModelOntoTurtle() throws Exception {
		runRDF4JTestWithOntology("src/test/resources/mysql/materializer/materializeTurtle.ttl",
				TurtleWriter::new);
	}

	public void testModelOntoRdfXml() throws Exception {
		runRDF4JTestWithOntology("src/test/resources/mysql/materializer/materializeRdf.owl",
				RDFXMLWriter::new);
	}
	
	public void testOWLApiModel() throws Exception {
		OntopSQLOWLAPIConfiguration configuration = createConfigurationBuilder()
				.build();

		runOWLAPITest("src/test/resources/mysql/materializer/materializeOWL.owl",
				27, 3, configuration);
	}
	
	public void testOWLApiModeOnto() throws Exception {
		OntopSQLOWLAPIConfiguration configuration = createConfigurationWithOntology();

		runOWLAPITest("src/test/resources/mysql/materializer/materializeOWL2.owl",
				51, 5, configuration);
	}

	private static OntopSQLOWLAPIConfiguration.Builder<? extends OntopSQLOWLAPIConfiguration.Builder> createConfigurationBuilder() {

		String obdaFileName =  RDF4JMaterializerCmdTest.class.getResource("/mysql/materializer/MaterializeTest.obda").toString();
		String propertyFileName =  RDF4JMaterializerCmdTest.class.getResource("/mysql/materializer/MaterializeTest.properties").toString();
		return OntopSQLOWLAPIConfiguration.defaultBuilder()
				.nativeOntopMappingFile(obdaFileName)
				.propertyFile(propertyFileName)
				.enableTestMode();
	}

	private void runRDF4JTestWithoutOntology(String filePath, Function<Writer, RDFHandler> handlerConstructor)
			throws IOException {
		OntopSQLOWLAPIConfiguration configuration = createConfigurationBuilder()
				.build();
		runRDF4JTest(filePath, handlerConstructor, 27, 3, configuration);
	}

	private void runRDF4JTestWithOntology(String filePath, Function<Writer, RDFHandler> handlerConstructor)
			throws IOException {
		OntopSQLOWLAPIConfiguration configuration = createConfigurationWithOntology();
		runRDF4JTest(filePath, handlerConstructor, 51, 5, configuration);
	}

	private void runRDF4JTest(String filePath, Function<Writer, RDFHandler> handlerConstructor,
							  long expectedTripleCount, int expectedVocabularySize,
							  OntopSystemConfiguration configuration) throws IOException {
		// output
		File out = new File(filePath);
		Writer writer = null;
		try {
			String outfile = out.getAbsolutePath();
			System.out.println(outfile);

			MaterializationParams materializationParams = MaterializationParams.defaultBuilder()
					.enableDBResultsStreaming(DO_STREAM_RESULTS)
					.build();

			RDF4JMaterializer materializer = RDF4JMaterializer.defaultMaterializer();
			MaterializationGraphQuery graphQuery = materializer.materialize(configuration, materializationParams);

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"));
			RDFHandler handler = handlerConstructor.apply(writer);
			graphQuery.evaluate(handler);

			assertEquals(expectedTripleCount, graphQuery.getTripleCountSoFar());
			assertEquals(expectedVocabularySize, graphQuery.getSelectedVocabulary().size());

		} finally {
			if (writer != null)
				writer.close();

			if (out.exists())
				out.delete();
		}
	}

	private void runOWLAPITest(String filePath, long expectedTripleCount, int expectedVocabularySize,
							   OntopSystemConfiguration configuration) throws IOException, OWLException {
		File out = new File(filePath);
		String outfile = out.getAbsolutePath();
		System.out.println(outfile);
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(out));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
		try {

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = manager.createOntology(IRI.create(out));
			manager = ontology.getOWLOntologyManager();

			MaterializationParams materializationParams = MaterializationParams.defaultBuilder()
					.enableDBResultsStreaming(DO_STREAM_RESULTS)
					.build();

			OntopOWLAPIMaterializer materializer = OntopOWLAPIMaterializer.defaultMaterializer();

			try(MaterializedGraphOWLResultSet graphResultSet = materializer.materialize(configuration,
					materializationParams)) {

				while (graphResultSet.hasNext())
					manager.addAxiom(ontology, graphResultSet.next());
				manager.saveOntology(ontology, new OWLXMLDocumentFormat(), new WriterDocumentTarget(writer));

				assertEquals(expectedTripleCount, graphResultSet.getTripleCountSoFar());
				assertEquals(expectedVocabularySize, graphResultSet.getSelectedVocabulary().size());
			}
		}
		finally {
			output.close();
			if (out.exists()) {
				out.delete();
			}
		}
	}


	private static OntopSQLOWLAPIConfiguration createConfigurationWithOntology() {
		return createConfigurationBuilder()
				.ontologyFile(ONTOLOGY_FILE_PATH)
				.build();
	}
}
