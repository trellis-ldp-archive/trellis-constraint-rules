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

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.apache.jena.riot.Lang.TURTLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import edu.amherst.acdc.trellis.spi.ConstraintService;
import edu.amherst.acdc.trellis.vocabulary.LDP;
import edu.amherst.acdc.trellis.vocabulary.Trellis;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdf.model.Model;

import org.junit.Test;

/**
 * @author acoburn
 */
public class LdpConstraintsTest {

    private static final JenaRDF rdf = new JenaRDF();

    private final String domain = "info:trellis/";

    private final ConstraintService svc = new LdpConstraints(domain);

    private final List<IRI> models = asList(LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.Resource,
            LDP.DirectContainer, LDP.IndirectContainer);

    @Test
    public void testInvalidContainsProperty() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, asGraph("/hasLdpContainsTriples.ttl", subject),
                    rdf.createIRI(subject));
            assertTrue(res.isPresent());
            assertEquals(of(Trellis.InvalidProperty), res);
        });
    }

    @Test
    public void testInvalidInsertedContentRelation() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, asGraph("/hasInsertedContent.ttl", subject),
                    rdf.createIRI(subject));
            if (type.equals(LDP.IndirectContainer)) {
                assertEquals(Optional.empty(), res);
                assertFalse(res.isPresent());
            } else {
                assertTrue(res.isPresent());
                assertEquals(of(Trellis.InvalidProperty), res);
            }
        });
    }

    @Test
    public void testInvalidLdpProps() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, asGraph("/basicContainer.ttl", subject),
                    rdf.createIRI(subject));
            if (type.equals(LDP.IndirectContainer) || type.equals(LDP.DirectContainer)) {
                assertEquals(Optional.empty(), res);
                assertFalse(res.isPresent());
            } else {
                assertTrue(res.isPresent());
                assertEquals(of(Trellis.InvalidProperty), res);
            }
        });
    }

    @Test
    public void testInvalidType() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, asGraph("/withLdpType.ttl", subject),
                    rdf.createIRI(subject));
            assertTrue(res.isPresent());
            assertEquals(of(Trellis.InvalidType), res);
        });
    }

    @Test
    public void testInvalidDomain() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, asGraph("/invalidDomain.ttl", subject),
                    rdf.createIRI(subject));
            assertTrue(res.isPresent());
            assertEquals(of(Trellis.InvalidRange), res);
        });
    }

    @Test
    public void testCardinality() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, asGraph("/invalidCardinality.ttl", subject),
                    rdf.createIRI(subject));
            assertTrue(res.isPresent());
            assertEquals(of(Trellis.InvalidCardinality), res);
        });
    }

    private Graph asGraph(final String resource, final String context) {
        final Model model = createDefaultModel();
        read(model, getClass().getResourceAsStream(resource), context, TURTLE);
        return rdf.asGraph(model);
    }
}
