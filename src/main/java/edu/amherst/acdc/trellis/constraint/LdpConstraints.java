/*
 * Copyright Amherst College
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.amherst.acdc.trellis.constraint;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Stream.empty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import edu.amherst.acdc.trellis.spi.ConstraintService;
import edu.amherst.acdc.trellis.vocabulary.LDP;
import edu.amherst.acdc.trellis.vocabulary.RDF;
import edu.amherst.acdc.trellis.vocabulary.Trellis;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;

/**
 * @author acoburn
 */
public class LdpConstraints implements ConstraintService {

    private static Predicate<Triple> indirectConstraints = triple ->
        triple.getPredicate().equals(LDP.contains);

    private static Predicate<Triple> directConstraints = indirectConstraints.or(triple ->
        triple.getPredicate().equals(LDP.insertedContentRelation));

    private static Predicate<Triple> basicConstraints = directConstraints.or(triple ->
        triple.getPredicate().equals(LDP.membershipResource) ||
        triple.getPredicate().equals(LDP.hasMemberRelation) ||
        triple.getPredicate().equals(LDP.isMemberOfRelation));

    private static Map<IRI, Predicate<Triple>> typeMap;

    static {
        final Map<IRI, Predicate<Triple>> types = new HashMap<>();
        types.put(LDP.BasicContainer, basicConstraints);
        types.put(LDP.Container, basicConstraints);
        types.put(LDP.DirectContainer, directConstraints);
        types.put(LDP.IndirectContainer, indirectConstraints);
        types.put(LDP.NonRDFSource, basicConstraints);
        types.put(LDP.RDFSource, basicConstraints);
        typeMap = unmodifiableMap(types);
    }

    // Ensure that any LDP properties are appropriate for the interaction model
    private static Predicate<Triple> propertyFilter(final IRI model) {
        return of(model).filter(typeMap::containsKey).map(typeMap::get).orElse(basicConstraints);
    }

    // Ensure that RDF graphs adhere to the single-subject rule
    private static Predicate<Triple> subjectFilter(final IRI context) {
        return triple -> of(triple).map(Triple::getSubject).map(BlankNodeOrIRI::ntriplesString)
            .filter(str -> !str.equals("<" + context + ">") && !str.startsWith("<" + context + "#")).isPresent();
    }

    // Don't allow LDP types to be set explicitly
    private static Predicate<Triple> typeFilter(final IRI model) {
        return triple -> of(triple).filter(t -> t.getPredicate().equals(RDF.type)).map(Triple::getObject)
            .map(RDFTerm::ntriplesString).filter(str -> !str.startsWith("<" + LDP.uri)).isPresent();
    }

    private Function<Triple, Stream<IRI>> checkConstraints(final IRI model, final IRI context) {
        requireNonNull(model, "The interaction model must not be null!");

        return triple -> of(triple).filter(propertyFilter(model)).map(t -> Stream.of(Trellis.InvalidProperty))
            .orElseGet(() -> of(triple).filter(subjectFilter(context)).map(t -> Stream.of(Trellis.InvalidSubject))
            .orElseGet(() -> of(triple).filter(typeFilter(model)).map(t -> Stream.of(Trellis.InvalidType))
            .orElse(empty())));
    }

    @Override
    public Optional<IRI> constrainedBy(final IRI model, final Graph graph, final IRI context) {
        return graph.stream().parallel().flatMap(checkConstraints(model, context)).findAny();
    }
}
