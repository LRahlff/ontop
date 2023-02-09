package it.unibz.inf.ontop.substitution;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.term.*;

/**
 * Declaration that the substitution is immutable and only refer to ImmutableTerms.
 *
 * See SubstitutionFactory for creating new instances
 *
 */
public interface Substitution<T extends ImmutableTerm>  {

    Stream<Map.Entry<Variable, T>> stream();

    boolean isDefining(Variable variable);

    ImmutableSet<Variable> getDomain();

    boolean rangeAllMatch(Predicate<T> predicate);

    boolean rangeAnyMatch(Predicate<T> predicate);

    ImmutableSet<T> getRangeSet();

    ImmutableSet<Variable> getRangeVariables();

    T get(Variable variable);

    boolean isEmpty();

    <S extends ImmutableTerm> Substitution<S> transform(Function<T, S> function);


    boolean isInjective();

    InjectiveSubstitution<T> injective();

    SubstitutionOperations<ImmutableTerm> onImmutableTerms();


    default ImmutableTerm apply(Variable variable) { return onImmutableTerms().apply(this, variable); }

    default ImmutableTerm applyToTerm(ImmutableTerm t) { return onImmutableTerms().applyToTerm(this, t); }

    default ImmutableFunctionalTerm apply(ImmutableFunctionalTerm term) { return onImmutableTerms().apply(this, term); }

    default ImmutableExpression apply(ImmutableExpression expression) { return onImmutableTerms().apply(this, expression); }

    default ImmutableList<ImmutableTerm> apply(ImmutableList<? extends Variable> variables) { return onImmutableTerms().apply(this, variables); }

    default ImmutableSet<ImmutableTerm> apply(ImmutableSet<? extends Variable> terms) { return onImmutableTerms().apply(this, terms); }

    default ImmutableList<ImmutableTerm> applyToTerms(ImmutableList<? extends ImmutableTerm> terms) { return onImmutableTerms().applyToTerms(this, terms); }

    default ImmutableMap<Integer, ImmutableTerm> applyToTerms(ImmutableMap<Integer, ? extends ImmutableTerm> argumentMap) { return onImmutableTerms().applyToTerms(this, argumentMap); }

    default Substitution<ImmutableTerm> compose(Substitution<? extends ImmutableTerm> f) { return onImmutableTerms().compose(this, f); }




    <S extends ImmutableTerm> Substitution<S> castTo(Class<S> type);

    Substitution<T> restrictDomainTo(Set<Variable> set);

    Substitution<T> removeFromDomain(Set<Variable> set);

    <S extends ImmutableTerm> Substitution<S> restrictRangeTo(Class<? extends S> type);

    ImmutableSet<Variable> preImage(Predicate<T> predicate);

    ImmutableMap<T, Collection<Variable>> inverseMap();

    Builder<T> builder();

    interface Builder<T extends ImmutableTerm> {
        Substitution<T> build();

        <S extends ImmutableTerm> Substitution<S> build(Class<S> type);

        Builder<T> restrictDomainTo(Set<Variable> set);

        Builder<T> removeFromDomain(Set<Variable> set);

        Builder<T> restrict(BiPredicate<Variable, T> predicate);

        Builder<T> restrictRange(Predicate<T> predicate);

        <S extends ImmutableTerm> Builder<S> restrictRangeTo(Class<? extends S> type);

        <U, S extends ImmutableTerm> Builder<S> transform(Function<Variable, U> lookup, BiFunction<T, U, S> function);

        <S extends ImmutableTerm> Builder<S> transform(Function<T, S> function);

        <U> Builder<T> transformOrRetain(Function<Variable, U> lookup, BiFunction<T, U, T> function);

        <U, S extends ImmutableTerm> Builder<S> transformOrRemove(Function<Variable, U> lookup, Function<U, S> function);

        <U> Builder<T> flatTransform(Function<Variable, U> lookup, Function<U, Substitution<T>> function);

        Stream<ImmutableExpression> toStrictEqualities();

        <S> Stream<S> toStream(BiFunction<Variable, T, S> transformer);

        <S> ImmutableMap<Variable, S> toMap(BiFunction<Variable, T, S> transformer);

        <S> ImmutableMap<Variable, S> toMapWithoutOptional(BiFunction<Variable, T, Optional<S>> transformer);
    }
}
