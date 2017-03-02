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
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Stream.empty;

import edu.amherst.acdc.trellis.spi.ConstraintService;
import edu.amherst.acdc.trellis.vocabulary.ACL;
import edu.amherst.acdc.trellis.vocabulary.LDP;
import edu.amherst.acdc.trellis.vocabulary.OA;
import edu.amherst.acdc.trellis.vocabulary.RDF;
import edu.amherst.acdc.trellis.vocabulary.Trellis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;

/**
 * @author acoburn
 */
public class LdpConstraints implements ConstraintService {

    // Identify those predicates that are prohibited in the given ixn model
    private static final Predicate<Triple> indirectConstraints = triple ->
        triple.getPredicate().equals(LDP.contains);

    // Identify those predicates that are prohibited in the given ixn model
    private static final Predicate<Triple> directConstraints = indirectConstraints.or(triple ->
        triple.getPredicate().equals(LDP.insertedContentRelation));

    // Identify those predicates that are prohibited in the given ixn model
    private static final Predicate<Triple> basicConstraints = directConstraints.or(triple ->
        triple.getPredicate().equals(LDP.membershipResource) ||
        triple.getPredicate().equals(LDP.hasMemberRelation) ||
        triple.getPredicate().equals(LDP.isMemberOfRelation));

    private static final Map<IRI, Predicate<Triple>> typeMap = unmodifiableMap(new HashMap<IRI, Predicate<Triple>>() { {
        put(LDP.BasicContainer, basicConstraints);
        put(LDP.Container, basicConstraints);
        put(LDP.DirectContainer, directConstraints);
        put(LDP.IndirectContainer, indirectConstraints);
        put(LDP.NonRDFSource, basicConstraints);
        put(LDP.RDFSource, basicConstraints);
    }});

    private static final Set<IRI> propertiesWithInDomainRange = unmodifiableSet(new HashSet<IRI>() { {
        add(ACL.accessControl);
        add(LDP.membershipResource);
    }});

    private static final Set<IRI> propertiesWithUriRange = unmodifiableSet(new HashSet<IRI>() { {
        add(ACL.accessControl);
        add(LDP.membershipResource);
        add(LDP.hasMemberRelation);
        add(LDP.isMemberOfRelation);
        add(LDP.inbox);
        add(LDP.insertedContentRelation);
        add(OA.annotationService);
    }});

    // Ensure that any LDP properties are appropriate for the interaction model
    private static Predicate<Triple> propertyFilter(final IRI model) {
        return of(model).filter(typeMap::containsKey).map(typeMap::get).orElse(basicConstraints);
    }

    // Don't allow LDP types to be set explicitly
    private static final Predicate<Triple> typeFilter = triple ->
        of(triple).filter(t -> t.getPredicate().equals(RDF.type)).map(Triple::getObject)
            .map(RDFTerm::ntriplesString).filter(str -> str.startsWith("<" + LDP.uri)).isPresent();

    // Verify that the range of the property is an IRI (if the property is in the above set)
    private static Predicate<Triple> uriRangeFilter = triple -> {
        return propertiesWithUriRange.contains(triple.getPredicate()) && !(triple.getObject() instanceof IRI);
    };

    // Verify that the range of the property is in the repository domain
    private static Predicate<Triple> inDomainRangeFilter(final String domain) {
        return triple -> propertiesWithInDomainRange.contains(triple.getPredicate()) &&
            !triple.getObject().ntriplesString().startsWith("<" + domain);
    }

    private static Boolean hasMembershipProps(final Map<IRI, Long> data) {
        return data.containsKey(LDP.membershipResource) &&
            data.getOrDefault(LDP.hasMemberRelation, 0L) + data.getOrDefault(LDP.isMemberOfRelation, 0L) == 1L;
    }

    // Verify that the cardinality of the `propertiesWithUriRange` properties. Keep any whose cardinality is > 1
    private static Predicate<Graph> checkCardinality(final IRI model) {
        return graph -> {
            final Map<IRI, Long> cardinality = graph.stream()
                .filter(triple -> propertiesWithUriRange.contains(triple.getPredicate()))
                .collect(groupingBy(Triple::getPredicate, counting()));

            if (LDP.IndirectContainer.equals(model)) {
                if (isNull(cardinality.get(LDP.insertedContentRelation)) || !hasMembershipProps(cardinality)) {
                    return true;
                }
            } else if (LDP.DirectContainer.equals(model)) {
                if (!hasMembershipProps(cardinality)) {
                    return true;
                }
            }

            return cardinality.entrySet().stream().map(Map.Entry::getValue).anyMatch(val -> val > 1);
        };
    }

    private final String domain;

    /**
     * Create a LpdConstraints service
     * @param domain the repository domain
     */
    public LdpConstraints(final String domain) {
        this.domain = domain;
    }

    private Function<Triple, Stream<IRI>> checkModelConstraints(final IRI model) {
        requireNonNull(model, "The interaction model must not be null!");

        return triple -> of(triple).filter(propertyFilter(model)).map(t -> Stream.of(Trellis.InvalidProperty))
            .orElseGet(() -> of(triple).filter(typeFilter).map(t -> Stream.of(Trellis.InvalidType))
            .orElseGet(() -> of(triple).filter(uriRangeFilter).map(t -> Stream.of(Trellis.InvalidRange))
            .orElseGet(() -> of(triple).filter(inDomainRangeFilter(domain)).map(t -> Stream.of(Trellis.InvalidRange))
            .orElse(empty()))));
    }

    @Override
    public Optional<IRI> constrainedBy(final IRI model, final Graph graph) {
        return ofNullable(graph.stream().parallel().flatMap(checkModelConstraints(model)).findAny()
            .orElseGet(() -> of(graph).filter(checkCardinality(model)).map(t -> Trellis.InvalidCardinality)
            .orElse(null)));
    }
}
