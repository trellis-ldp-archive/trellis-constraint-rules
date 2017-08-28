/*
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
package org.trellisldp.constraint;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.apache.jena.riot.Lang.TURTLE;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;
import static ch.qos.logback.classic.Level.DEBUG;

import org.trellisldp.spi.ConstraintService;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

import java.util.List;
import java.util.Optional;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdf.model.Model;

import org.junit.Test;
import ch.qos.logback.classic.Logger;

/**
 * @author acoburn
 */
public class LdpConstraintsTest {

    private static final JenaRDF rdf = new JenaRDF();

    private final String domain = "trellis:repository/";

    private final ConstraintService svc = new LdpConstraints();

    private final List<IRI> models = asList(LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.Resource,
            LDP.DirectContainer, LDP.IndirectContainer);

    @Test
    public void testInvalidAccessControlProperty() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/hasAccessControlTriples.ttl", subject));
            assertEquals(of(Trellis.InvalidProperty), res);
        });
    }

    @Test
    public void testInvalidContainsProperty() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/hasLdpContainsTriples.ttl", subject));
            assertEquals(of(Trellis.InvalidProperty), res);
        });
    }

    @Test
    public void testInvalidInsertedContentRelation() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/hasInsertedContent.ttl", subject));
            if (type.equals(LDP.IndirectContainer) || type.equals(LDP.DirectContainer)) {
                assertEquals(Optional.empty(), res);
            } else {
                assertEquals(of(Trellis.InvalidProperty), res);
            }
        });
    }

    @Test
    public void testInvalidLdpProps() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/basicContainer.ttl", subject));
            if (type.equals(LDP.DirectContainer) || type.equals(LDP.IndirectContainer)) {
                assertEquals(Optional.empty(), res);
            } else {
                assertEquals(of(Trellis.InvalidProperty), res);
            }
        });
    }

    @Test
    public void testInvalidType() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/withLdpType.ttl", subject));
            assertEquals(of(Trellis.InvalidType), res);
        });
    }

    @Test
    public void testInvalidDomain() {
        // While we're at it, test the debug logging case
        ((Logger) getLogger(LdpConstraints.class)).setLevel(DEBUG);
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/invalidDomain.ttl", subject));
            if (type.equals(LDP.DirectContainer) || type.equals(LDP.IndirectContainer)) {
                assertEquals(of(Trellis.InvalidRange), res);
            } else {
                assertEquals(of(Trellis.InvalidProperty), res);
            }
        });
    }

    @Test
    public void testInvalidInbox() {
        final Optional<IRI> res = svc.constrainedBy(LDP.RDFSource, domain,
                asGraph("/invalidInbox.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidRange), res);
    }

    @Test
    public void testTooManyMembershipTriples() {
        final Optional<IRI> res = svc.constrainedBy(LDP.IndirectContainer, domain,
                asGraph("/tooManyMembershipTriples.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidCardinality), res);
    }

    @Test
    public void testBasicConstraints1() {
        final Optional<IRI> res = svc.constrainedBy(LDP.Container, domain,
                asGraph("/invalidContainer1.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidProperty), res);
    }

    @Test
    public void testBasicConstraints2() {
        final Optional<IRI> res = svc.constrainedBy(LDP.Container, domain,
                asGraph("/invalidContainer2.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidProperty), res);
    }

    @Test
    public void testBasicConstraints3() {
        final Optional<IRI> res = svc.constrainedBy(LDP.Container, domain,
                asGraph("/invalidContainer3.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidProperty), res);
    }

    @Test
    public void testMembershipTriples1() {
        final Optional<IRI> res = svc.constrainedBy(LDP.IndirectContainer,
                domain, asGraph("/invalidMembershipTriple.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidRange), res);
    }

    @Test
    public void testMembershipTriples2() {
        final Optional<IRI> res = svc.constrainedBy(LDP.DirectContainer,
                domain, asGraph("/invalidMembershipTriple2.ttl", domain + "foo"));
        assertEquals(of(Trellis.InvalidRange), res);
    }

    @Test
    public void testCardinality() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<IRI> res = svc.constrainedBy(type, domain, asGraph("/invalidCardinality.ttl", subject));
            assertEquals(of(Trellis.InvalidCardinality), res);
        });
    }

    private Graph asGraph(final String resource, final String context) {
        final Model model = createDefaultModel();
        read(model, getClass().getResourceAsStream(resource), context, TURTLE);
        return rdf.asGraph(model);
    }
}
