package it.unibz.inf.ontop.iq.optimizer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.node.*;
import it.unibz.inf.ontop.model.atom.DistinctVariableOnlyDataAtom;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static it.unibz.inf.ontop.DependencyTestDBMetadata.PK_TABLE1_AR2;
import static it.unibz.inf.ontop.OptimizationTestingTools.*;
import static junit.framework.TestCase.assertEquals;

public class DistinctTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistinctTest.class);

    @Test
    public void testDistinctConstructionConstant1() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR1_PREDICATE, A);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of());

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(A, ONE));

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                IQ_FACTORY.createDistinctNode(),
                IQ_FACTORY.createUnaryIQTree(
                        constructionNode,
                        dataNode));

        IQTree expectedTree = IQ_FACTORY.createUnaryIQTree(
                constructionNode,
                IQ_FACTORY.createUnaryIQTree(
                        IQ_FACTORY.createSliceNode(0, 1),
                        dataNode));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    @Test
    public void testDistinctConstructionConstant2() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR2_PREDICATE, A, B);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of(1, B));

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(A, ONE));

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                IQ_FACTORY.createDistinctNode(),
                IQ_FACTORY.createUnaryIQTree(
                        constructionNode,
                        dataNode));

        IQTree expectedTree = IQ_FACTORY.createUnaryIQTree(
                constructionNode,
                IQ_FACTORY.createUnaryIQTree(
                        IQ_FACTORY.createDistinctNode(),
                        dataNode));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    @Test
    public void testDistinctUnion1() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR1_PREDICATE, A);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of(1, B));

        FilterNode filterNode = IQ_FACTORY.createFilterNode(TERM_FACTORY.getDBStartsWith(ImmutableList.of(B, ONE_STR)));

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(A, ONE_STR));

        IQTree filterSubTree = IQ_FACTORY.createUnaryIQTree(filterNode, dataNode);
        IQTree subTree1 = IQ_FACTORY.createUnaryIQTree(constructionNode, filterSubTree);

        ValuesNode valuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        UnionNode unionNode = IQ_FACTORY.createUnionNode(projectionAtom.getVariables());
        DistinctNode distinctNode = IQ_FACTORY.createDistinctNode();

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                distinctNode,
                IQ_FACTORY.createNaryIQTree(
                        unionNode,
                        ImmutableList.of(subTree1, valuesNode)));

        IQTree newSubTree1 = IQ_FACTORY.createUnaryIQTree(
                constructionNode,
                IQ_FACTORY.createUnaryIQTree(
                        IQ_FACTORY.createSliceNode(0, 1),
                        filterSubTree));

        ValuesNode newValuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        IQTree expectedTree = IQ_FACTORY.createNaryIQTree(
                        unionNode,
                        ImmutableList.of(newSubTree1, newValuesNode));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    @Test
    public void testDistinctUnion2() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR1_PREDICATE, A);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of(1, B));

        FilterNode filterNode = IQ_FACTORY.createFilterNode(TERM_FACTORY.getDBStartsWith(ImmutableList.of(B, ONE_STR)));

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(
                        A,
                        // Non-deterministic -> blocks
                        TERM_FACTORY.getDBRowUniqueStr()));

        IQTree filterSubTree = IQ_FACTORY.createUnaryIQTree(filterNode, dataNode);
        IQTree subTree1 = IQ_FACTORY.createUnaryIQTree(constructionNode, filterSubTree);

        ValuesNode valuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        UnionNode unionNode = IQ_FACTORY.createUnionNode(projectionAtom.getVariables());
        DistinctNode distinctNode = IQ_FACTORY.createDistinctNode();

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                distinctNode,
                IQ_FACTORY.createNaryIQTree(
                        unionNode,
                        ImmutableList.of(subTree1, valuesNode)));

        ValuesNode newValuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        IQTree expectedTree = IQ_FACTORY.createUnaryIQTree(
                distinctNode,
                IQ_FACTORY.createNaryIQTree(
                        unionNode,
                        ImmutableList.of(subTree1, newValuesNode)));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    @Test
    public void testDistinctUnion3() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR1_PREDICATE, A);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of(1, B));

        FilterNode subFilterNode = IQ_FACTORY.createFilterNode(TERM_FACTORY.getDBStartsWith(ImmutableList.of(B, ONE_STR)));

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(A, ONE_STR));

        IQTree filterSubTree = IQ_FACTORY.createUnaryIQTree(subFilterNode, dataNode);
        IQTree subTree1 = IQ_FACTORY.createUnaryIQTree(constructionNode, filterSubTree);

        ValuesNode valuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        UnionNode unionNode = IQ_FACTORY.createUnionNode(projectionAtom.getVariables());

        FilterNode topFilterNode = IQ_FACTORY.createFilterNode(
                TERM_FACTORY.getDBNonStrictNumericEquality(TERM_FACTORY.getDBRand(UUID.randomUUID()), ONE));

        DistinctNode distinctNode = IQ_FACTORY.createDistinctNode();

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                distinctNode,
                IQ_FACTORY.createUnaryIQTree(
                    topFilterNode,
                    IQ_FACTORY.createNaryIQTree(
                            unionNode,
                            ImmutableList.of(subTree1, valuesNode))));

        IQTree newSubTree1 = IQ_FACTORY.createUnaryIQTree(
                constructionNode,
                IQ_FACTORY.createUnaryIQTree(
                        IQ_FACTORY.createSliceNode(0, 1),
                        filterSubTree));

        ValuesNode newValuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        IQTree expectedTree = IQ_FACTORY.createUnaryIQTree(
                topFilterNode,
                IQ_FACTORY.createNaryIQTree(
                    unionNode,
                    ImmutableList.of(newSubTree1, newValuesNode)));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    @Test
    public void testDistinctUnion4() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR1_PREDICATE, A);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of(1, B));

        FilterNode subFilterNode = IQ_FACTORY.createFilterNode(TERM_FACTORY.getDBStartsWith(ImmutableList.of(B, ONE_STR)));

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(A, ONE_STR));

        IQTree filterSubTree = IQ_FACTORY.createUnaryIQTree(subFilterNode, dataNode);
        IQTree subTree1 = IQ_FACTORY.createUnaryIQTree(constructionNode, filterSubTree);

        ValuesNode valuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        UnionNode unionNode = IQ_FACTORY.createUnionNode(projectionAtom.getVariables());

        FilterNode topFilterNode = IQ_FACTORY.createFilterNode(
                TERM_FACTORY.getDBNonStrictNumericEquality(TERM_FACTORY.getDBRand(UUID.randomUUID()), ONE));

        OrderByNode orderByNode = IQ_FACTORY.createOrderByNode(
                ImmutableList.of(IQ_FACTORY.createOrderComparator(A, true)));

        DistinctNode distinctNode = IQ_FACTORY.createDistinctNode();

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                distinctNode,
                IQ_FACTORY.createUnaryIQTree(
                    orderByNode,
                    IQ_FACTORY.createUnaryIQTree(
                            topFilterNode,
                            IQ_FACTORY.createNaryIQTree(
                                    unionNode,
                                    ImmutableList.of(subTree1, valuesNode)))));

        IQTree newSubTree1 = IQ_FACTORY.createUnaryIQTree(
                constructionNode,
                IQ_FACTORY.createUnaryIQTree(
                        IQ_FACTORY.createSliceNode(0, 1),
                        filterSubTree));

        ValuesNode newValuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        IQTree expectedTree = IQ_FACTORY.createUnaryIQTree(
                orderByNode,
                IQ_FACTORY.createUnaryIQTree(
                    topFilterNode,
                    IQ_FACTORY.createNaryIQTree(
                            unionNode,
                            ImmutableList.of(newSubTree1, newValuesNode))));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    @Test
    public void testDistinctUnion5() {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS1_AR1_PREDICATE, A);

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(PK_TABLE1_AR2, ImmutableMap.of(1, B));

        FilterNode subFilterNode = IQ_FACTORY.createFilterNode(TERM_FACTORY.getDBStartsWith(ImmutableList.of(B, ONE_STR)));

        ConstructionNode constructionNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables(),
                SUBSTITUTION_FACTORY.getSubstitution(A, ONE_STR));

        IQTree filterSubTree = IQ_FACTORY.createUnaryIQTree(subFilterNode, dataNode);
        IQTree subTree1 = IQ_FACTORY.createUnaryIQTree(constructionNode, filterSubTree);

        ValuesNode valuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        UnionNode unionNode = IQ_FACTORY.createUnionNode(projectionAtom.getVariables());

        OrderByNode orderByNode = IQ_FACTORY.createOrderByNode(
                ImmutableList.of(IQ_FACTORY.createOrderComparator(A, true)));

        DistinctNode distinctNode = IQ_FACTORY.createDistinctNode();

        IQTree initialTree = IQ_FACTORY.createUnaryIQTree(
                distinctNode,
                IQ_FACTORY.createUnaryIQTree(
                        orderByNode,
                        IQ_FACTORY.createNaryIQTree(
                                unionNode,
                                ImmutableList.of(subTree1, valuesNode))));

        IQTree newSubTree1 = IQ_FACTORY.createUnaryIQTree(
                constructionNode,
                IQ_FACTORY.createUnaryIQTree(
                        IQ_FACTORY.createSliceNode(0, 1),
                        filterSubTree));

        ValuesNode newValuesNode = IQ_FACTORY.createValuesNode(ImmutableList.of(A),
                ImmutableList.of(
                        ImmutableList.of(TWO_STR),
                        ImmutableList.of(THREE_STR)));

        IQTree expectedTree = IQ_FACTORY.createUnaryIQTree(
                orderByNode,
                IQ_FACTORY.createNaryIQTree(
                        unionNode,
                        ImmutableList.of(newSubTree1, newValuesNode)));

        normalizeAndCompare(initialTree, expectedTree, projectionAtom);
    }

    private static void normalizeAndCompare(IQTree initialTree, IQTree expectedTree, DistinctVariableOnlyDataAtom projectionAtom) {
        normalizeAndCompare(
                IQ_FACTORY.createIQ(projectionAtom, initialTree),
                IQ_FACTORY.createIQ(projectionAtom, expectedTree));
    }

    private static void normalizeAndCompare(IQ initialIQ, IQ expectedIQ) {
        LOGGER.info("Initial IQ: " + initialIQ );
        LOGGER.info("Expected IQ: " + expectedIQ);

        IQ normalizedIQ = initialIQ.normalizeForOptimization();
        LOGGER.info("Normalized IQ: " + normalizedIQ);

        assertEquals(expectedIQ, normalizedIQ);
    }
}
