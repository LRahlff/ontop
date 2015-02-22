package org.semanticweb.ontop.reformulation.tests;

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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.ontop.model.*;
import org.semanticweb.ontop.owlrefplatform.core.QuestConstants;
import org.semanticweb.ontop.owlrefplatform.core.QuestPreferences;
import org.semanticweb.ontop.owlrefplatform.owlapi3.QuestOWL;
import org.semanticweb.ontop.owlrefplatform.owlapi3.QuestOWLConnection;
import org.semanticweb.ontop.owlrefplatform.owlapi3.QuestOWLEmptyEntitiesChecker;
import org.semanticweb.ontop.owlrefplatform.owlapi3.QuestOWLFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use the class EmptiesAboxCheck to test the return of empty concepts and
 * roles, based on the mappings. Given ontology, which is connected to a
 * database via mappings, generate a suitable set of queries that test if there
 * are empty concepts, concepts that are no populated to anything.
 */
public class QuestOWLEmptyEntitiesCheckerTest {

	private QuestOWLConnection conn;
	private Connection connection;

	Logger log = LoggerFactory.getLogger(this.getClass());
	private OWLOntology ontology;

	final String owlfile = "src/test/resources/test/emptiesDatabase.owl";
	final String obdafile = "src/test/resources/test/emptiesDatabase.obda";

	// final String owlFileName =
	// "src/main/resources/testcases-scenarios/virtual-mode/stockexchange/simplecq/stockexchange.owl";
	// final String obdaFileName =
	// "src/main/resources/testcases-scenarios/virtual-mode/stockexchange/simplecq/stockexchange-mysql.obda";

	private List<Predicate> emptyConcepts = new ArrayList<Predicate>();
	private List<Predicate> emptyRoles = new ArrayList<Predicate>();

	private QuestOWL reasoner;

	@Before
	public void setUp() throws Exception {

		String driver = "org.h2.Driver";
		String url = "jdbc:h2:mem:questjunitdb;";
		String username = "sa";
		String password = "";

		connection = DriverManager.getConnection(url, username, password);
		Statement st = connection.createStatement();

		FileReader reader = new
				FileReader("src/test/resources/test/emptiesDatabase-h2.sql");
		BufferedReader in = new BufferedReader(reader);
		StringBuilder bf = new StringBuilder();
		String line = in.readLine();
		while (line != null) {
			bf.append(line);
			line = in.readLine();
		}

		st.executeUpdate(bf.toString());
		connection.commit();

		// Loading the OWL file
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));

		Properties p = new Properties();
		p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setProperty(QuestPreferences.OBTAIN_FULL_METADATA, QuestConstants.FALSE);
		// Creating a new instance of the reasoner
		QuestOWLFactory factory = new QuestOWLFactory(new File(obdafile), new QuestPreferences(p));

		reasoner = factory.createReasoner(ontology, new SimpleConfiguration());

		// Now we are ready for querying
		conn = reasoner.getConnection();

	}

	@After
	public void tearDown() throws Exception {
			dropTables();
			reasoner.dispose();
			connection.close();
	}

	private void dropTables() throws SQLException, IOException {

		Statement st = connection.createStatement();

		FileReader reader = new FileReader("src/test/resources/test/emptiesDatabase-drop-h2.sql");
		BufferedReader in = new BufferedReader(reader);
		StringBuilder bf = new StringBuilder();
		String line = in.readLine();
		while (line != null) {
			bf.append(line);
			line = in.readLine();
		}

		st.executeUpdate(bf.toString());
		st.close();
		connection.commit();
	}

	/**
	 * Test numbers of empty concepts
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmptyConcepts() throws Exception {

		QuestOWLEmptyEntitiesChecker empties = new QuestOWLEmptyEntitiesChecker(ontology, conn);
		emptyConcepts = empties.getEmptyConcepts();
		log.info("Empty concept/s: " + emptyConcepts);
		assertEquals(1, emptyConcepts.size());

	}

	/**
	 * Test numbers of empty roles
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmptyRoles() throws Exception {
		QuestOWLEmptyEntitiesChecker empties = new QuestOWLEmptyEntitiesChecker(ontology, conn);
		emptyRoles = empties.getEmptyRoles();
		log.info("Empty role/s: " + emptyRoles);
		assertEquals(2, emptyRoles.size());

	}

	/**
	 * Test numbers of empty concepts and roles
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmpties() throws Exception {

		QuestOWLEmptyEntitiesChecker empties = new QuestOWLEmptyEntitiesChecker(ontology, conn);
		emptyConcepts = empties.getEmptyConcepts();
		log.info(empties.toString());
		log.info("Empty concept/s: " + emptyConcepts);
		assertEquals(1, emptyConcepts.size());
		emptyRoles = empties.getEmptyRoles();
		log.info("Empty role/s: " + emptyRoles);
		assertEquals(2, emptyRoles.size());

	}

}
