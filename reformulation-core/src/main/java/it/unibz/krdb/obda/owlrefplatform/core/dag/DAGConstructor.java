package it.unibz.krdb.obda.owlrefplatform.core.dag;

/*
 * #%L
 * ontop-reformulation-core
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

import it.unibz.krdb.obda.ontology.Description;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.OntologyFactory;
import it.unibz.krdb.obda.ontology.PropertyExpression;
import it.unibz.krdb.obda.ontology.SomeValuesFrom;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
import it.unibz.krdb.obda.ontology.impl.OntologyImpl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;




@Deprecated
public class DAGConstructor {

	private static final OntologyFactory descFactory = OntologyFactoryImpl.getInstance();

	public static DAG getISADAG(Ontology ontology) {
		return new DAG(ontology);
	}

	public static DAG filterPureISA(DAG dag) {

		Map<Description, DAGNode> classes = new LinkedHashMap<Description, DAGNode>();
		Map<Description, DAGNode> roles = new LinkedHashMap<Description, DAGNode>();
		Map<Description, DAGNode> allnodes = new LinkedHashMap<Description, DAGNode>();

		for (DAGNode node : dag.getClasses()) {

			if (node.getDescription() instanceof SomeValuesFrom) {
				continue;
			}

			DAGNode newNode = classes.get(node.getDescription());
			if (newNode == null) {
				newNode = new DAGNode(node.getDescription());
				newNode.setIndex(node.getIndex());
				newNode.getRange().addRange(node.getRange());
				newNode.equivalents = new LinkedHashSet<DAGNode>(node.equivalents);
				classes.put(node.getDescription(), newNode);
				allnodes.put(node.getDescription(), newNode);
			}

			for (DAGNode child : node.getChildren()) {
				if (child.getDescription() instanceof SomeValuesFrom) {
					continue;
				}
				DAGNode newChild = classes.get(child.getDescription());
				if (newChild == null) {
					newChild = new DAGNode(child.getDescription());
					newChild.equivalents = new LinkedHashSet<DAGNode>(child.equivalents);
					classes.put(child.getDescription(), newChild);
					allnodes.put(child.getDescription(), newChild);
				}

				if (!newChild.getDescription().equals(newNode.getDescription())) {
					newChild.getParents().add(newNode);
					newNode.getChildren().add(newChild);
				}
			}
		}

		for (DAGNode node : dag.getRoles()) {
			PropertyExpression nodeDesc = (PropertyExpression) node.getDescription();

			if (nodeDesc.getPredicate().getName().toString().startsWith(OntologyImpl.AUXROLEURI)) {
				continue;
			}

			if (nodeDesc.isInverse()) {
				PropertyExpression posNode = descFactory.createProperty(nodeDesc.getPredicate().getName());
				DAGNode newNode = roles.get(posNode);
				if (newNode == null) {
					newNode = new DAGNode(posNode);
					roles.put(posNode, newNode);
					allnodes.put(posNode, newNode);
				}
				continue;
			}

			DAGNode newNode = roles.get(nodeDesc);

			if (newNode == null) {
				newNode = new DAGNode(nodeDesc);
				newNode.equivalents = new LinkedHashSet<DAGNode>(node.equivalents);
				roles.put(nodeDesc, newNode);
				allnodes.put(nodeDesc, newNode);
			}
			for (DAGNode child : node.getChildren()) {
				PropertyExpression childDesc = (PropertyExpression) child.getDescription();
				if (childDesc.getPredicate().getName().toString().startsWith(OntologyImpl.AUXROLEURI)) {
					continue;
				}
				if (childDesc.isInverse()) {
					PropertyExpression posChild = descFactory.createProperty(childDesc.getPredicate().getName());
					DAGNode newChild = roles.get(posChild);
					if (newChild == null) {
						newChild = new DAGNode(posChild);
						roles.put(posChild, newChild);
						allnodes.put(posChild, newChild);
					}
					continue;
				}

				DAGNode newChild = roles.get(childDesc);
				if (newChild == null) {
					newChild = new DAGNode(childDesc);
					newChild.equivalents = new LinkedHashSet<DAGNode>(child.equivalents);
					roles.put(childDesc, newChild);
					allnodes.put(childDesc, newChild);
				}
				if (!newChild.getDescription().equals(newNode.getDescription())) {
					newChild.getParents().add(newNode);
					newNode.getChildren().add(newChild);
				}
			}
		}
		Map<Description, Description> newEquivalentMappings = new HashMap<Description, Description>();
		for (Description desc : dag.equi_mappings.keySet()) {
			// Description key = makePositive(desc);
			// Description val = makePositive(dag.equi_mappings.get(desc));

			Description key = desc;
			Description val = dag.equi_mappings.get(desc);

			if (key != null && val != null)
				newEquivalentMappings.put(key, val);

		}
		DAG newDag = new DAG(classes, roles, newEquivalentMappings, allnodes);

		return newDag;
	}

}
