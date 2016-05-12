package it.unibz.inf.ontop.pivotalrepr.impl;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import fj.P;
import fj.P2;
import it.unibz.inf.ontop.owlrefplatform.core.basicoperations.*;
import it.unibz.inf.ontop.pivotalrepr.impl.tree.DefaultIntermediateQueryBuilder;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.pivotalrepr.*;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unibz.inf.ontop.owlrefplatform.core.basicoperations.ImmutableUnificationTools.computeMGUS;

/**
 * TODO: explain
 */
public class SubQueryUnificationTools {

    /**
     * TODO: explain
     */
    public static class SubQueryUnificationException extends Exception {
        protected SubQueryUnificationException(String message) {
            super(message);
        }
    }

    /**
     * TODO: explain
     *
     * TODO: better integrate in the Ontop exception hierarchy
     */
    public static class UnificationException extends Exception {

        protected  UnificationException() {
            super();
        }

        protected  UnificationException(String message) {
            super(message);
        }
    }


    /**
     * TODO: find a better name
     */
    public static class NewSubstitutionPair {
        public final ImmutableSubstitution<ImmutableTerm> bindings;
        public final ImmutableSubstitution<? extends ImmutableTerm> propagatedSubstitution;

        private NewSubstitutionPair(ImmutableSubstitution<ImmutableTerm> bindings,
                                   ImmutableSubstitution<? extends ImmutableTerm> propagatedSubstitution) {
            this.bindings = bindings;
            this.propagatedSubstitution = propagatedSubstitution;
        }
    }

    /**
     * TODO: explain
     */
    private static class AtomSubstitutionSplit {

        private final ImmutableList<InjectiveVar2VarSubstitution> renamingSubstitutions;
        private final ImmutableSubstitution<VariableOrGroundTerm> constraintSubstitution;


        /**
         * TODO: explain
         */
        protected AtomSubstitutionSplit(ImmutableSubstitution<VariableOrGroundTerm> atomSubstitution) {
            ImmutableMap<Variable, VariableOrGroundTerm> originalMap = atomSubstitution.getImmutableMap();
            ImmutableMap.Builder<Variable, VariableOrGroundTerm> constraintMapBuilder = ImmutableMap.builder();
            Set<Variable> originalVariablesToRename = new HashSet<>();

            /**
             * Extracts var-to-ground-term constraints and collects original variables that will be renamed
             */
            for (Map.Entry<Variable, VariableOrGroundTerm> entry : originalMap.entrySet()) {
                VariableOrGroundTerm targetTerm = entry.getValue();
                Variable originalVariable = entry.getKey();

                if (targetTerm instanceof GroundTerm) {
                    constraintMapBuilder.put(originalVariable, targetTerm);
                }
                else {
                    originalVariablesToRename.add(originalVariable);
                }
            }

            /**
             * Extracts the injective renaming substitutions and some additional constraints.
             */
            P2<ImmutableList<InjectiveVar2VarSubstitution>, ImmutableMap<Variable, VariableOrGroundTerm>> extractedPair
                    = extractRenamingSubstitutions(originalMap, originalVariablesToRename);
            renamingSubstitutions = extractedPair._1();
            constraintMapBuilder.putAll(extractedPair._2());

            constraintSubstitution = new ImmutableSubstitutionImpl<>(constraintMapBuilder.build());
        }

        public ImmutableList<InjectiveVar2VarSubstitution> getRenamingSubstitutions() {
            return renamingSubstitutions;
        }

        public ImmutableSubstitution<VariableOrGroundTerm> getConstraintSubstitution() {
            return constraintSubstitution;
        }
    }

    /**
     * TODO: explain
     */
    public static class ConstructionNodeUnification {
        private final ConstructionNode unifiedNode;
        private final ImmutableSubstitution<VariableOrGroundTerm> substitutionToPropagate;

        protected ConstructionNodeUnification(
                ConstructionNode unifiedNode,
                ImmutableSubstitution<VariableOrGroundTerm> substitutionToPropagate) {
            this.unifiedNode = unifiedNode;
            this.substitutionToPropagate = substitutionToPropagate;
        }

        public final ConstructionNode getUnifiedNode() {
            return unifiedNode;
        }

        public final ImmutableSubstitution<VariableOrGroundTerm> getSubstitutionToPropagate() {
            return substitutionToPropagate;
        }
    }



    /**
     * TODO: explain
     *
     * Returns a new IntermediateQuery (the original one is untouched).
     */
    public static IntermediateQuery unifySubQuery(final IntermediateQuery originalSubQuery,
                                                  final DataAtom targetDataAtom,
                                                  final ImmutableSet<Variable> reservedVariables)
            throws SubQueryUnificationException {

        ConstructionNode originalRootNode = originalSubQuery.getRootConstructionNode();

        /**
         * Should have already been checked.
         */
        if (!originalRootNode.getProjectionAtom().hasSamePredicateAndArity(targetDataAtom)) {
            throw new IllegalArgumentException("The target data atom is not compatible with the query");
        }

        QueryNodeRenamer renamer = new QueryNodeRenamer(
                computeRenamingSubstitution(originalSubQuery, reservedVariables));

        ConstructionNodeUnification rootUnification = unifyConstructionNode(renamer.transform(originalRootNode),
                targetDataAtom);

        IntermediateQueryBuilder queryBuilder = new DefaultIntermediateQueryBuilder(originalSubQuery.getMetadata());
        queryBuilder.init(projectionAtom, rootUnification.unifiedNode);

        /**
         * TODO: explain
         */
        queryBuilder = propagateToChildren(queryBuilder, originalSubQuery, originalRootNode, rootUnification.unifiedNode,
                rootUnification.substitutionToPropagate, renamer);

        return queryBuilder.build();
    }

    /**
     * TODO: explain
     *
     * Recursive
     */
    private static IntermediateQueryBuilder propagateToChildren(IntermediateQueryBuilder queryBuilder,
                                                                IntermediateQuery originalSubQuery,
                                                                QueryNode originalParentNode,
                                                                QueryNode unifiedParentNode,
                                                                ImmutableSubstitution<? extends VariableOrGroundTerm> substitution,
                                                                QueryNodeRenamer renamer)
            throws IntermediateQueryBuilderException, SubQueryUnificationException {
        for(QueryNode originalChild : originalSubQuery.getChildren(originalParentNode)) {

            QueryNode renamedChild;
            try {
                renamedChild = originalChild.acceptNodeTransformer(renamer);
            } catch (NotNeededNodeException e) {
                throw new IllegalStateException("A renamer should not remove a node: " + e);
            }

            Optional<? extends QueryNode> optionalNewChild;
            Optional<? extends ImmutableSubstitution<? extends VariableOrGroundTerm>> optionalSubstitutionForChild;

            try {
                SubstitutionResults<? extends QueryNode> substitutionResults =
                        renamedChild.applyDescendentSubstitution(substitution);

                optionalNewChild = substitutionResults.getOptionalNewNode();
                optionalSubstitutionForChild = substitutionResults.getSubstitutionToPropagate();

            }
            /**
             * Unification rejected by a sub-construction node.
             */
            catch(QueryNodeSubstitutionException e) {
                throw new SubQueryUnificationException(e.getMessage());
            }

            /**
             * Stopping the propagation
             * TODO: support this case
             */
            if (!optionalSubstitutionForChild.isPresent()) {
                throw new RuntimeException("Stopping the propagation to children is not yet supported. TO BE DONE");
            }


            Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition = originalSubQuery.getOptionalPosition(originalParentNode,
                    originalChild);

            QueryNode nextOriginalParent;
            QueryNode nextNewParent;
            /**
             * Normal case: the new child becomes the new parent.
             */
            if (optionalNewChild.isPresent()) {
                QueryNode newChild = optionalNewChild.get();
                queryBuilder.addChild(unifiedParentNode, newChild, optionalPosition);
                nextOriginalParent = originalChild;
                nextNewParent =  newChild;
            }
            /**
             * No new child: keep the same parent
             */
            else {
                nextOriginalParent = originalParentNode;
                nextNewParent = unifiedParentNode;
            }

            // Recursive call
            queryBuilder = propagateToChildren(queryBuilder, originalSubQuery, nextOriginalParent, nextNewParent,
                    optionalSubstitutionForChild.get(), renamer);
        }
        return queryBuilder;
    }

    /**
     * TODO: explain
     *
     */
    private static InjectiveVar2VarSubstitution computeRenamingSubstitution(IntermediateQuery subQuery,
                                                                            ImmutableSet<Variable> reservedVariables) {
        ImmutableSet<Variable> subQueryVariables = VariableCollector.collectVariables(subQuery.getNodesInTopDownOrder());
        ImmutableSet<Variable> allKnownVariables = ImmutableSet.<Variable>builder()
                .addAll(reservedVariables)
                .addAll(subQueryVariables)
                .build();
        VariableGenerator variableGenerator = new VariableGenerator(allKnownVariables);

        ImmutableMap.Builder<Variable, Variable> renamingBuilder = ImmutableMap.builder();

        for (Variable subQueryVariable : subQueryVariables) {
            /**
             * If there is a conflict: creates a new variable and
             * adds an entry in the renaming substitution
             */
            if (reservedVariables.contains(subQueryVariable)) {
                Variable newVariable = variableGenerator.generateNewVariableFromVar(subQueryVariable);
                renamingBuilder.put(subQueryVariable, newVariable);
            }
        }

        return new InjectiveVar2VarSubstitutionImpl(renamingBuilder.build());
    }

    /**
     * TODO: explain
     *
     * TODO: support query modifiers
     *
     */
    public static ConstructionNodeUnification unifyConstructionNode(ConstructionNode renamedConstructionNode,
                                                                    DataAtom targetAtom)
            throws SubQueryUnificationException{

        if (!haveDisjunctVariableSets(renamedConstructionNode, targetAtom)) {
            throw new IllegalArgumentException("The variable sets of the construction node and the target atom must " +
                    "be disjunct!");
        }

        ImmutableSubstitution<VariableOrGroundTerm> atomSubstitution = extractAtomSubstitution(
                renamedConstructionNode.getProjectionAtom(), targetAtom);

        AtomSubstitutionSplit atomSubstitutionSplit = new AtomSubstitutionSplit(atomSubstitution);

        Optional<ImmutableSubstitution<ImmutableTerm>> optionalConstraintUnifier = computeMGUS(
                atomSubstitutionSplit.constraintSubstitution, renamedConstructionNode.getSubstitution());

        if (!optionalConstraintUnifier.isPresent()) {
            // TODO: Is it an internal error?
            throw new SubQueryUnificationException("Constraints could not be unified");
        }
        ImmutableSubstitution<ImmutableTerm> constraintUnifier = optionalConstraintUnifier.get();

        ImmutableSubstitution<ImmutableTerm> filteredConstraintSubstitution = extractConstraintsNotEncodedInAtom(
                constraintUnifier, atomSubstitutionSplit.constraintSubstitution);

        ImmutableSubstitution<ImmutableTerm> newConstructionNodeSubstitution = ImmutableSubstitutionTools.renameSubstitution(
                filteredConstraintSubstitution, atomSubstitutionSplit.renamingSubstitutions);

        ImmutableSubstitution<VariableOrGroundTerm> substitutionToPropagate = extractSubstitutionToPropagate(
                atomSubstitutionSplit.renamingSubstitutions, constraintUnifier, filteredConstraintSubstitution);

        Optional<ImmutableQueryModifiers> newOptionalQueryModifiers = updateOptionalModifiers(
                renamedConstructionNode.getOptionalModifiers(), atomSubstitution, constraintUnifier);

        ConstructionNode newConstructionNode = new ConstructionNodeImpl(targetAtom, newConstructionNodeSubstitution,
                newOptionalQueryModifiers);
        return new ConstructionNodeUnification(newConstructionNode, substitutionToPropagate);
    }

    /**
     * TODO: explain
     */
    private static ImmutableSubstitution<VariableOrGroundTerm> extractSubstitutionToPropagate(
            ImmutableList<InjectiveVar2VarSubstitution> renamingSubstitutions,
            ImmutableSubstitution<ImmutableTerm> constraintUnifier,
            ImmutableSubstitution<ImmutableTerm> filteredConstraintSubstitution) {
        ImmutableMap.Builder<Variable, VariableOrGroundTerm> mapBuilder = ImmutableMap.builder();

        /**
         * Extracts renaming mappings that are not in the filtered constraint substitution
         */
        for(InjectiveVar2VarSubstitution renamingSubstitution : renamingSubstitutions) {
            for (Map.Entry<Variable, Variable> entry : renamingSubstitution.getImmutableMap().entrySet()) {
                Variable varToRename = entry.getKey();

                if (!filteredConstraintSubstitution.isDefining(varToRename)) {
                    mapBuilder.put(varToRename, entry.getValue());
                }
            }
        }

        /**
         * Extracts the mappings of the unifier that are not in the filtered constraint substitution
         */
        for (Map.Entry<Variable, ImmutableTerm> entry : constraintUnifier.getImmutableMap().entrySet()) {
            Variable varToRename = entry.getKey();

            if (!filteredConstraintSubstitution.isDefining(varToRename)) {
                ImmutableTerm newTerm =  entry.getValue();

                if (!(newTerm instanceof VariableOrGroundTerm)) {
                    throw new IllegalArgumentException("Inconsistent filteredConstraintSubstitution " +
                            "regarding the constraintUnifier");
                }
                mapBuilder.put(varToRename,(VariableOrGroundTerm) newTerm);
            }
        }

        return new ImmutableSubstitutionImpl<>(mapBuilder.build());
    }

    /**
     * TODO: explain
     */
    public static Optional<ImmutableQueryModifiers> updateOptionalModifiers(
            Optional<ImmutableQueryModifiers> optionalModifiers,
            ImmutableSubstitution<? extends ImmutableTerm> substitution1,
            ImmutableSubstitution<? extends ImmutableTerm> substitution2) {
        if (!optionalModifiers.isPresent()) {
            return Optional.empty();
        }
        ImmutableQueryModifiers previousModifiers = optionalModifiers.get();
        ImmutableList.Builder<OrderCondition> conditionBuilder = ImmutableList.builder();

        for (OrderCondition condition : previousModifiers.getSortConditions()) {
            ImmutableTerm newTerm = substitution2.apply(substitution1.apply(condition.getVariable()));
            /**
             * If after applying the substitution the term is still a variable,
             * "updates" the OrderCondition.
             *
             * Otherwise, forgets it.
             */
            if (newTerm instanceof Variable) {
                conditionBuilder.add(condition.newVariable((Variable) newTerm));
            }
        }
        return previousModifiers.newSortConditions(conditionBuilder.build());
    }

    /**
     * TODO: explain it
     */
    private static ImmutableSubstitution<ImmutableTerm> extractConstraintsNotEncodedInAtom(
            ImmutableSubstitution<ImmutableTerm> constraintUnifier,
            ImmutableSubstitution<VariableOrGroundTerm> constraintsFromAtoms) {
        if (constraintsFromAtoms.isEmpty())
            return constraintUnifier;

        ImmutableSet<Variable> variablesToFilterOut = constraintsFromAtoms.getImmutableMap().keySet();
        ImmutableMap<Variable, ImmutableTerm> constraintMap = constraintUnifier.getImmutableMap();

        ImmutableMap.Builder<Variable, ImmutableTerm> substitutionMapBuilder = ImmutableMap.builder();
        for (Variable variable : constraintMap.keySet()) {
            if (!variablesToFilterOut.contains(variable)) {
                substitutionMapBuilder.put(variable, constraintMap.get(variable));
            }
        }

        return new ImmutableSubstitutionImpl<>(substitutionMapBuilder.build());
    }

    /**
     * Creates the injective renaming substitutions and extracts additional constraints.
     *
     * TODO: Further explain
     *
     */
    private static P2<ImmutableList<InjectiveVar2VarSubstitution>, ImmutableMap<Variable, VariableOrGroundTerm>>
                extractRenamingSubstitutions(ImmutableMap<Variable, VariableOrGroundTerm> originalMap,
                                             Set<Variable> originalVariablesToRenameLater) {

        ImmutableMap.Builder<Variable, VariableOrGroundTerm> additionalConstraintMapBuilder = ImmutableMap.builder();

        ImmutableList.Builder<InjectiveVar2VarSubstitution> renamingListBuilder = ImmutableList.builder();

        while (!originalVariablesToRenameLater.isEmpty()) {
            Map<Variable, Variable> renamingMap = new HashMap<>();

            Set<Variable> originalVariablesToRenameNow = originalVariablesToRenameLater;
            originalVariablesToRenameLater = new HashSet<>();

            for (Variable originalVariable : originalVariablesToRenameNow) {
                Variable targetVariable = (Variable) originalMap.get(originalVariable);

                if (renamingMap.values().contains(targetVariable)) {
                    originalVariablesToRenameLater.add(originalVariable);
                    additionalConstraintMapBuilder.put(targetVariable, originalVariable);
                }
                else {
                    renamingMap.put(originalVariable, targetVariable);
                }
            }
            // Creates a new renaming substitution
            renamingListBuilder.add(new InjectiveVar2VarSubstitutionImpl(renamingMap));
        }

        return P.p(renamingListBuilder.build(), additionalConstraintMapBuilder.build());
    }



    /**
     * TODO: explain
     *
     * This could have been implemented using a MGU
     * but we want to distinguish different cases where unification is not impossible.
     *
     * In one case, unification could be possible but only in an indirect manner.
     */
    private static ImmutableSubstitution<VariableOrGroundTerm> extractAtomSubstitution(DataAtom originalAtom,
                                                                                       DataAtom newAtom)
            throws SubQueryUnificationException {

        if(!originalAtom.hasSamePredicateAndArity(newAtom)) {
            throw new SubQueryUnificationException(originalAtom + " and " + newAtom
                    + " have different predicates and/or arities");
        }

        // ImmutableMap.Builder<Variable, VariableOrGroundTerm> mapBuilder = ImmutableMap.builder();
        Map<Variable, VariableOrGroundTerm> substitutionMap = new HashMap<>();

        ImmutableList<? extends VariableOrGroundTerm> originalArgs = originalAtom.getArguments();
        ImmutableList<? extends VariableOrGroundTerm> newArgs = newAtom.getArguments();

        for (int i = 0; i < originalArgs.size(); i++) {
            VariableOrGroundTerm originalArg = originalArgs.get(i);
            VariableOrGroundTerm newArg = newArgs.get(i);

            if (originalArg instanceof Variable) {
                Variable originalVar = (Variable) originalArg;

                /**
                 * Normal case: new variable to variable-or-ground-term.
                 * --> added to the map.
                 */
                if (!substitutionMap.containsKey(originalVar)) {
                    substitutionMap.put(originalVar, newArg);
                }
                /**
                 * Otherwise, we except this entry to be already present.
                 */
                else if (!substitutionMap.get(originalVar).equals(newArg)) {
                    /**
                     * TODO: throw a different exception when we will support
                     * indirect unification.
                     *
                     */
                    throw new SubQueryUnificationException(originalAtom + " and " + newAtom
                            + " are not DIRECTLY unifiable");
                }
            }
            /**
             * The original arg is a ground term --> should be equal
             */
            else if (!originalArg.equals(newArg)) {
                if (newArg instanceof Variable) {
                    throw new RuntimeException("Ground-term-to-variable unification is not yet supported.");
                }
                else {
                    throw new SubQueryUnificationException(originalAtom + " and " + newAtom
                            + " are not unifiable");
                }
            }
        }
        return new ImmutableSubstitutionImpl<>(ImmutableMap.copyOf(substitutionMap));
    }

    private static boolean haveDisjunctVariableSets(ConstructionNode constructionNode, DataAtom targetAtom) {

        Set<Variable> variableSet = new HashSet<>();

        /**
         * First put the target variables
         */
        for (VariableOrGroundTerm term : targetAtom.getArguments()) {
            if (term instanceof Variable)
                variableSet.add((Variable)term);
        }


        /**
         * Removes all the variables that are not used in the construction node.
         * Said differently, computes the intersection.
         */
        variableSet.retainAll(constructionNode.getVariables());

        return variableSet.isEmpty();

    }

    public static ImmutableSet<Variable> computeNewProjectedVariables(
            ImmutableSubstitution<? extends VariableOrGroundTerm> tau, ImmutableSet<Variable> projectedVariables) {
        ImmutableSet<Variable> tauDomain = tau.getDomain();

        Stream<Variable> remainingVariableStream = projectedVariables.stream()
                .filter(v -> !tauDomain.contains(v));

        Stream<Variable> newVariableStream = tau.getMap().values().stream()
                .filter(t -> t instanceof Variable)
                .map(t -> (Variable) t);

        return Stream.concat(newVariableStream, remainingVariableStream)
                .collect(ImmutableCollectors.toSet());
    }

    /**
     * TODO: find a proper name
     *
     * TODO: explain
     *
     */
    public static NewSubstitutionPair traverseConstructionNode(
            ImmutableSubstitution<? extends VariableOrGroundTerm> tau,
            ImmutableSubstitution<? extends ImmutableTerm> formerTheta,
            ImmutableSet<Variable> formerV) throws UnificationException {

        Var2VarSubstitution tauR = tau.getVar2VarFragment();
        ImmutableSubstitution<GroundTerm> tauG = tau.getVar2GroundTermFragment();

        Var2VarSubstitution tauEq = extractTauEq(tauR);

        ImmutableSubstitution<? extends ImmutableTerm> tauC = tauG.unionHeterogeneous(tauEq)
                .orElseThrow(() -> new IllegalStateException("Bug: dom(tauG) must be disjoint with dom(tauEq)"));


        ImmutableSubstitution<ImmutableTerm> eta = computeMGUS(formerTheta, tauC)
                .orElseThrow(UnificationException::new);

        ImmutableSubstitution<ImmutableTerm> etaB = extractEtaB(eta, formerV, tauC);

        ImmutableSubstitution<? extends ImmutableTerm> newTheta = tauR.applyToSubstitution(etaB)
                .orElseThrow(() -> new IllegalStateException("Bug: tauR does not rename etaB safely as excepted"));

        ImmutableSubstitution<? extends ImmutableTerm> delta = computeDelta(formerTheta, newTheta, eta, tauR, tauEq);

        return new NewSubstitutionPair(newTheta, delta);
    }

    /**
     * TODO: explain
     */
    private static Var2VarSubstitution extractTauEq(Var2VarSubstitution tauR) {
        int domainVariableCount = tauR.getDomain().size();
        if (domainVariableCount <= 1) {
            return tauR;
        }

        ImmutableMultimap<Variable, Variable> inverseMultimap = tauR.getImmutableMap().entrySet().stream()
                // Inverse
                .map(e -> (Map.Entry<Variable, Variable>) new AbstractMap.SimpleImmutableEntry<>(e.getValue(), e.getKey()))
                .collect(ImmutableCollectors.toMultimap());

        ImmutableMap<Variable, Variable> newMap = inverseMultimap.asMap().values().stream()
                // TODO: explain
                .filter(vars -> vars.size() <= 1)
                //
                .flatMap(vars -> {
                    List<Variable> sortedVariables = vars.stream()
                            .sorted()
                            .collect(Collectors.toList());
                    Variable largerVariable = sortedVariables.get(sortedVariables.size() - 1);
                    return sortedVariables.stream()
                            .limit(sortedVariables.size() - 1)
                            .map(v -> (Map.Entry<Variable, Variable>) new AbstractMap.SimpleEntry<>(v, largerVariable));
                })
                .collect(ImmutableCollectors.toMap());

        return new Var2VarSubstitutionImpl(newMap);
    }

    private static ImmutableSubstitution<ImmutableTerm> extractEtaB(ImmutableSubstitution<ImmutableTerm> eta,
                                                                    ImmutableSet<Variable> formerV,
                                                                    ImmutableSubstitution<? extends ImmutableTerm> tauC) {

        ImmutableSet<Variable> tauCDomain = tauC.getDomain();

        ImmutableMap<Variable, ImmutableTerm> newMap = eta.getImmutableMap().entrySet().stream()
                .filter(e -> formerV.contains(e.getKey()))
                .filter(e -> !tauCDomain.contains(e.getKey()))
                .collect(ImmutableCollectors.toMap());

        return new ImmutableSubstitutionImpl<>(newMap);
    }

    private static ImmutableSubstitution<? extends ImmutableTerm> computeDelta(
            ImmutableSubstitution<? extends VariableOrGroundTerm> formerTheta,
            ImmutableSubstitution<? extends ImmutableTerm> newTheta,
            ImmutableSubstitution<ImmutableTerm> eta, Var2VarSubstitution tauR,
            Var2VarSubstitution tauEq) {

        ImmutableSet<Map.Entry<Variable, Variable>> tauEqEntries = tauEq.getImmutableMap().entrySet();
        ImmutableSet<Variable> formerThetaDomain = formerTheta.getDomain();

        ImmutableMap<Variable, ImmutableTerm> newMap = Stream.concat(
                eta.getImmutableMap().entrySet().stream(),
                tauR.getImmutableMap().entrySet().stream())
                .filter(e -> !tauEqEntries.contains(e))
                .filter(e -> !formerThetaDomain.contains(e.getKey()))
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), newTheta.apply(e.getValue())))
                .collect(ImmutableCollectors.toMap());

        return new ImmutableSubstitutionImpl<>(newMap);
    }


}
