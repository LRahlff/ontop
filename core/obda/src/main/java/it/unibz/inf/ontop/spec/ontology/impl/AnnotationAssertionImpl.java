package it.unibz.inf.ontop.spec.ontology.impl;

/*
 * #%L
 * ontop-obdalib-core
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
import it.unibz.inf.ontop.model.term.Constant;
import it.unibz.inf.ontop.model.term.ObjectConstant;
import it.unibz.inf.ontop.spec.ontology.AnnotationAssertion;
import it.unibz.inf.ontop.spec.ontology.AnnotationProperty;


/**
 * Represents AnnotationAssertion from the OWL 2 QL Specification
 *
 * AnnotationAssertion := 'AnnotationAssertion' '(' axiomAnnotations AnnotationProperty AnnotationSubject AnnotationValue ')'
 * AnnotationSubject := IRI | AnonymousIndividual
 * AnnotationValue := AnonymousIndividual | IRI | Literal
 *
 */

public class AnnotationAssertionImpl implements AnnotationAssertion {

	private final AnnotationProperty prop;
	private final ObjectConstant o1;
	private final Constant o2;


	AnnotationAssertionImpl(AnnotationProperty prop, ObjectConstant o1, Constant o2) {
		this.prop = prop;
		this.o1 = o1;
		this.o2 = o2;

	}

	@Override
	public AnnotationProperty getProperty() {
		return prop;
	}

	@Override
	public ObjectConstant getSubject() {
		return o1;
	}

	@Override
	public Constant getValue() {
		return o2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnnotationAssertionImpl) {
			AnnotationAssertionImpl other = (AnnotationAssertionImpl)obj;
			return prop.equals(other.prop) && o1.equals(other.o1) && o2.equals(other.o2);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return prop.hashCode() + o1.hashCode() + o2.hashCode();
	}
	
	@Override
	public String toString() {
		return prop + "(" + o1 + ", " + o2 + ")";
	}

}

