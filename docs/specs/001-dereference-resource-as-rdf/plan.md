# Implementation Plan: Feature 001, Dereference a resource as RDF

**Spec:** docs/specs/001-dereference-resource-as-rdf/spec.md
**Decisions:** ADR 0004, 0010, 0011, 0012, 0013, 0016, 0018 (docs/adr/)
**Created:** 2026-10-26

!!! warning "Outdated module layout"
    This plan was written for the earlier modules (`spiegel-core`, `spiegel-adapter-config`, `spiegel-adapter-sparql`, `spiegel-adapter-web`, `spiegel-app`). The modules have since been restructured (see [Module structure](../../analysis/05-architecture/module-structure.md) and ADR 0004, amendment of 2026-10-28). Revise the plan before executing it.

## Context

SPieGeL is still an empty skeleton: six Maven modules with only `package-info.java`, `AccessLevel` and an `ArchitectureTest`. Spec 001 (approved) asks for the walking skeleton of phase 1: an `id` URI answers `303` to its `doc` URI, and a `doc` URI returns the bounded description of the thing in Turtle, N-Triples, RDF/XML or JSON-LD, with problem details for every error. Everything comes from a tenant configuration in Turtle; nothing is specific to a domain. This plan builds the first path through core, configuration adapter, SPARQL adapter, web adapter and application.

## Global Constraints

- All code, comments, identifiers, log and error messages and test names in British English (CLAUDE.md).
- Java 21, Spring Boot 4.1.1, Apache Jena 6.2.0 (ADR 0004). No new runtime dependencies other than `jena-core` (core), `jena-arq` (web, sparql), `jena-shacl` (config).
- The core depends on `jena-core` only, never on `org.apache.jena.sparql..`, `query..`, `riot..`, `http..`, `rdflink..`, Spring or servlet APIs (ADR 0010, NFR-Q-01). Adapters never depend on each other.
- Values from a request enter a query only as bound terms, never as text (NFR-SEC-04).
- Every IRI and URL that is used or sent comes from the domain's configured base, never from the request (FR-1).
- Problem types under `https://w3id.org/spiegel/problem/`: `not-found` 404, `not-acceptable` 406, `invalid-request` 400, `method-not-allowed` 405, `source-error` 502, `source-timeout` 504, `internal-error` 500 (ADR 0016).
- `Cache-Control: no-store` on every `5xx`. No store message, stack trace or query text in an error body.
- Every response carries `Access-Control-Allow-Origin: *` and exposes `Link`, `Location`, `Content-Location`.
- Logs: one structured entry per request, with request identifier, status and duration; no IP address, no query values other than `_format` (NFR-PRIV-02).
- Defaults: blank-node depth 3, time-out per data source 10 seconds (AC-5.5).
- Requirement IDs are never renumbered. Each test class names the AC or FR it covers in `@DisplayName`, as `AccessLevelTest` does.

## Decisions made while planning

These follow from the spec and the ADRs, and were confirmed with the user where they touch the public configuration vocabulary.

1. **`spiegel:base`** (one IRI, such as `<https://data.example.org>`) replaces `spiegel:hostName` from the sketch in ADR 0012. It gives scheme and host together; the host name used to resolve the tenant is taken from it.
2. **Every `spiegel:ResourceType` declares both `api:itemTemplate` and `api:uriTemplate`**, with the same variables. The exception for `/doc/emissie/{id}` therefore also names `/id/emissie/{id}`.
3. **`spiegel:timeout`** (`xsd:duration`) is set on a `spiegel:DataSource` in the tenant Turtle, default `PT10S`; **`spiegel:blankNodeDepth`** on the tenant, default 3.
4. **A query names its data source with `spiegel:dataSource`**; without one it runs on the tenant's `spiegel:defaultDataSource`, which is required.
5. **SPieGeL's default description query is generated for the configured depth** by the configuration adapter: a `CONSTRUCT` with one `UNION` branch per level for the node's own triples and one for the label and type of its IRI objects. It was tried on Jena 6.2 with a chain of four blank nodes: levels 1 to 3 are returned, level 4 appears only as an object, and incoming links are absent.
6. **The core bounds every description**, whatever query produced it (`DescriptionBounds`): the subject's triples, blank nodes to the depth, and only label and type of linked IRIs. AC-2.2 to AC-2.4 therefore hold for tenant queries too.
7. **`SERVICE` is found by walking the algebra** (`Algebra.compile` and `Walker.walk`), not with `ElementWalker`: a test on Jena 6.2 showed that `ElementWalker` misses `SERVICE` inside `FILTER EXISTS`, while the algebra walk finds it in subqueries, `EXISTS`, `NOT EXISTS`, `BIND` and select expressions. ADR 0011, decision 2, is amended to say so.
8. **A raw path segment must consist of URI characters only** (unreserved, sub-delims, `:`, `@`, percent-encoding). A test showed that Jena's `substitution` writes an IRI containing `>` unescaped, so a segment such as `a%3E` is matched on its decoded value but the IRI is built from the raw, still-encoded segment.
9. **Descriptions are serialised to a byte array before the status is sent.** That makes the RDF/XML check and "never `200` then fail" simple; streaming arrives with the size ceiling of feature 002.
10. **RDF serialisation stays in the web adapter** with Jena RIOT. A `RendererPort` is introduced with HTML in feature 005, when a second renderer exists (Anti-Abstraction Gate).

## Pre-implementation gates

- **Simplicity:** the feature touches the four existing modules that phase 1 provides plus the application; no new module. One new dependency per adapter, all from Jena, which ADR 0004 and 0010 already chose. No caching, pools or limits (feature 002).
- **Anti-abstraction:** Spring MVC, Jena RIOT, Jena SHACL and Jena's HTTP query execution are used directly. Ports exist only where the hexagonal design requires them (`DataSourcePort`, `QueryInspector`, `TenantConfigurationPort`). One model: the core's records; no DTOs.
- **Integration first:** Phase 0 writes the HTTP contract (`contracts/api.md`) and an end-to-end contract test per acceptance criterion, before any production code.

## Goal

A running SPieGeL that, from one tenant configuration in Turtle and a SPARQL endpoint, answers `id` URIs with `303`, `doc` URIs with a bounded description in four RDF formats, and every error with problem details.

---
## Phase 0: Contracts and tests first

**Implements:** FR-1 to FR-4 (as contract) | **Satisfies:** writes the failing end-to-end test for AC-1.1 to AC-1.4, AC-1.6, AC-2.1 to AC-2.8, AC-3.1 to AC-3.16, AC-4.1 to AC-4.6
**Files:** `pom.xml`, `spiegel-core/pom.xml`, `spiegel-adapter-sparql/pom.xml`, `spiegel-adapter-config/pom.xml`, `spiegel-adapter-web/pom.xml`, `spiegel-app/pom.xml`, `spiegel-app/src/test/java/.../ArchitectureTest.java`, `spiegel-adapter-sparql/src/test/java/be/vlaanderen/omgeving/spiegel/adapter/sparql/testing/StubSparqlEndpoint.java`, `spiegel-app/src/test/java/be/vlaanderen/omgeving/spiegel/ContractTenant.java`, `spiegel-app/src/test/resources/contract-data.ttl`, `spiegel-app/src/test/java/be/vlaanderen/omgeving/spiegel/DereferenceContractTest.java`, `docs/specs/001-dereference-resource-as-rdf/contracts/api.md`
**Interfaces:** Consumes: nothing. Produces: `StubSparqlEndpoint.serving(Graph)`, `.servingAfter(Graph, Duration)`, `.failing()`, `.slow(Duration)`, `.url()`, `.requestCount()`, `.close()` (test-jar of `spiegel-adapter-sparql`); `ContractTenant.write(Path, String main, String second, String failing, String slow)`; the HTTP contract in `contracts/api.md`.

Lock down the HTTP behaviour and the build before any production code: dependencies per ADR 0010, the extra architecture rule, a stub SPARQL endpoint that every later phase reuses, and one end-to-end test per acceptance criterion that runs against the whole application.

- [ ] Parent `pom.xml`: add to `<dependencyManagement>`

  ```xml
  <dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-core</artifactId>
    <version>${jena.version}</version>
  </dependency>
  <dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-shacl</artifactId>
    <version>${jena.version}</version>
  </dependency>
  <dependency>
    <groupId>be.vlaanderen.omgeving.spiegel</groupId>
    <artifactId>spiegel-adapter-sparql</artifactId>
    <version>${project.version}</version>
    <type>test-jar</type>
  </dependency>
  ```
- [ ] `spiegel-core/pom.xml`: add `org.apache.jena:jena-core` (ADR 0010).
- [ ] `spiegel-adapter-config/pom.xml`: add `org.apache.jena:jena-shacl` (brings `jena-arq` for parsing Turtle and `.rq` files).
- [ ] `spiegel-adapter-web/pom.xml`: add `org.apache.jena:jena-arq` (RIOT writers) and, test scope, `org.springframework.boot:spring-boot-starter-webmvc-test`.
- [ ] `spiegel-adapter-sparql/pom.xml`: publish the test fixtures as a test-jar:

  ```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>test-jar</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  ```
- [ ] `spiegel-app/pom.xml`, test scope: `spring-boot-starter-webmvc-test`, and `spiegel-adapter-sparql` with `<type>test-jar</type>`.
- [ ] `ArchitectureTest`: add the rule of ADR 0010, decision 3.

  ```java
  @ArchTest
  static final ArchRule coreUsesJenaGraphLevelOnly = noClasses()
          .that().resideInAPackage("..spiegel.core..")
          .should().dependOnClassesThat().resideInAnyPackage(
                  "org.apache.jena.sparql..",
                  "org.apache.jena.query..",
                  "org.apache.jena.riot..",
                  "org.apache.jena.http..",
                  "org.apache.jena.rdflink..")
          .because("the core uses Jena's graph types but neither parses, runs nor writes RDF (ADR 0010)");
  ```
- [ ] Write `StubSparqlEndpoint` (test fixture, JDK `HttpServer`, no extra dependency). It answers `CONSTRUCT` and `DESCRIBE` on an in-memory graph in N-Triples, or fails, or answers too late.

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.sparql.testing;

  import com.sun.net.httpserver.HttpExchange;
  import com.sun.net.httpserver.HttpServer;
  import java.io.ByteArrayOutputStream;
  import java.io.IOException;
  import java.io.UncheckedIOException;
  import java.net.InetSocketAddress;
  import java.net.URLDecoder;
  import java.nio.charset.StandardCharsets;
  import java.time.Duration;
  import java.util.concurrent.Executors;
  import java.util.concurrent.atomic.AtomicInteger;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.query.Query;
  import org.apache.jena.query.QueryFactory;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFWriter;
  import org.apache.jena.sparql.exec.QueryExec;

  /**
   * A SPARQL endpoint for tests: answers {@code CONSTRUCT} and {@code DESCRIBE} queries on an
   * in-memory graph in N-Triples, or fails, or answers too late.
   */
  public final class StubSparqlEndpoint implements AutoCloseable {

      /** The message a failing endpoint sends, which must never reach a client. */
      public static final String STORE_MESSAGE = "Virtuoso 37000 Error SP030: internal store detail";

      private final HttpServer server;
      private final AtomicInteger requests = new AtomicInteger();

      private StubSparqlEndpoint(Graph data, boolean fail, Duration delay) {
          try {
              server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
          } catch (IOException e) {
              throw new UncheckedIOException(e);
          }
          server.createContext("/sparql", exchange -> answer(exchange, data, fail, delay));
          server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
          server.start();
      }

      public static StubSparqlEndpoint serving(Graph data) {
          return new StubSparqlEndpoint(data, false, Duration.ZERO);
      }

      /** Serves the data, but only after a delay, to measure what SPieGeL adds to a store's time. */
      public static StubSparqlEndpoint servingAfter(Graph data, Duration delay) {
          return new StubSparqlEndpoint(data, false, delay);
      }

      public static StubSparqlEndpoint failing() {
          return new StubSparqlEndpoint(null, true, Duration.ZERO);
      }

      public static StubSparqlEndpoint slow(Duration delay) {
          return new StubSparqlEndpoint(null, false, delay);
      }

      public String url() {
          return "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";
      }

      /** The number of queries received so far. */
      public int requestCount() {
          return requests.get();
      }

      @Override
      public void close() {
          server.stop(0);
      }

      private void answer(HttpExchange exchange, Graph data, boolean fail, Duration delay) throws IOException {
          requests.incrementAndGet();
          try (exchange) {
              if (!delay.isZero()) {
                  Thread.sleep(delay);
              }
              if (fail || data == null) {
                  send(exchange, 500, "text/plain", STORE_MESSAGE.getBytes(StandardCharsets.UTF_8));
                  return;
              }
              Query query = QueryFactory.create(queryText(exchange));
              QueryExec exec = QueryExec.graph(data).query(query).build();
              Graph result = query.isDescribeType() ? exec.describe() : exec.construct();
              var body = new ByteArrayOutputStream();
              RDFWriter.source(result).lang(Lang.NTRIPLES).output(body);
              send(exchange, 200, "application/n-triples", body.toByteArray());
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
          }
      }

      private static String queryText(HttpExchange exchange) throws IOException {
          String form = "GET".equals(exchange.getRequestMethod())
                  ? exchange.getRequestURI().getRawQuery()
                  : new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
          if (contentType != null && contentType.startsWith("application/sparql-query")) {
              return form;
          }
          for (String pair : form.split("&")) {
              if (pair.startsWith("query=")) {
                  return URLDecoder.decode(pair.substring("query=".length()), StandardCharsets.UTF_8);
              }
          }
          throw new IllegalArgumentException("no query parameter");
      }

      private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
          exchange.getResponseHeaders().set("Content-Type", type);
          exchange.sendResponseHeaders(status, body.length);
          exchange.getResponseBody().write(body);
      }
  }
  ```
- [ ] Write the test data `spiegel-app/src/test/resources/contract-data.ttl`:

  ```turtle
  @prefix org:  <https://data.example.org/id/organisatie/> .
  @prefix ves:  <https://data.example.org/id/vestiging/> .
  @prefix em:   <https://data.example.org/id/emissie/> .
  @prefix ex:   <https://example.org/ns/> .
  @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
  @prefix skos: <http://www.w3.org/2004/02/skos/core#> .

  org:42 a ex:Organisatie ;
      rdfs:label "Organisatie 42"@nl ;
      ex:partner org:7 ;
      ex:adres [ ex:niveau2 [ ex:niveau3 [ ex:niveau4 [ ex:teDiep "niet tonen" ] ;
                                           ex:gemeente org:9 ] ] ] .
  org:7 a ex:Organisatie ; rdfs:label "Organisatie 7" ; ex:geheim "niet tonen" .
  org:9 skos:prefLabel "Gemeente 9" ; ex:geheim "niet tonen" .
  org:8 ex:partner org:42 .
  org:43 rdfs:label "Organisatie 43" ; <https://example.org/ns/eigenschap/123> "geen RDF/XML" .
  em:5 rdfs:label "Emissie 5" .
  ves:1 rdfs:label "Vestiging 1" .
  ```

  In this data, `ex:adres` points to level 1 and `ex:niveau4` from level 3 to level 4, whose own triple `ex:teDiep` must not appear (AC-2.2).
- [ ] Write `ContractTenant`, which writes the tenant configuration of the contract test into a directory:

  ```java
  package be.vlaanderen.omgeving.spiegel;

  import java.io.IOException;
  import java.nio.file.Files;
  import java.nio.file.Path;

  /** Writes the tenant configuration that the contract tests run against. */
  final class ContractTenant {

      private ContractTenant() {
      }

      static Path write(Path dir, String main, String second, String failing, String slow) throws IOException {
          Files.createDirectories(dir.resolve("queries"));
          Files.writeString(dir.resolve("tenant.ttl").toAbsolutePath(), """
                  @prefix spiegel: <https://w3id.org/spiegel/config#> .
                  @prefix api:     <http://purl.org/linked-data/api/vocab#> .
                  @prefix sh:      <http://www.w3.org/ns/shacl#> .
                  @prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .

                  <#tenant> a spiegel:Tenant ;
                      spiegel:base <https://data.example.org> ;
                      spiegel:defaultDataSource <#main> ;
                      spiegel:resourceType <#generic>, <#emissie>, <#vestiging>, <#fout>, <#traag> .

                  <#main>    a spiegel:DataSource ; spiegel:endpoint <%s> .
                  <#second>  a spiegel:DataSource ; spiegel:endpoint <%s> .
                  <#failing> a spiegel:DataSource ; spiegel:endpoint <%s> .
                  <#slow>    a spiegel:DataSource ; spiegel:endpoint <%s> ;
                      spiegel:timeout "PT1S"^^xsd:duration .

                  <#generic> a spiegel:ResourceType ;
                      api:itemTemplate "/id/{concept}/{id}" ;
                      api:uriTemplate  "/doc/{concept}/{id}" ;
                      spiegel:variable [ spiegel:name "concept" ; sh:pattern "^[a-z]+$" ] ,
                                       [ spiegel:name "id" ; sh:pattern "^[0-9]+$" ] .

                  <#emissie> a spiegel:ResourceType ;
                      api:itemTemplate "/id/emissie/{id}" ;
                      api:uriTemplate  "/doc/emissie/{id}" ;
                      spiegel:query [ spiegel:file "queries/emissie.rq" ] .

                  <#vestiging> a spiegel:ResourceType ;
                      api:itemTemplate "/id/vestiging/{id}" ;
                      api:uriTemplate  "/doc/vestiging/{id}" ;
                      spiegel:query [ spiegel:file "queries/describe.rq" ] ,
                                    [ spiegel:file "queries/describe.rq" ; spiegel:dataSource <#second> ] .

                  <#fout> a spiegel:ResourceType ;
                      api:itemTemplate "/id/fout/{id}" ;
                      api:uriTemplate  "/doc/fout/{id}" ;
                      spiegel:query [ spiegel:file "queries/describe.rq" ] ,
                                    [ spiegel:file "queries/describe.rq" ; spiegel:dataSource <#failing> ] .

                  <#traag> a spiegel:ResourceType ;
                      api:itemTemplate "/id/traag/{id}" ;
                      api:uriTemplate  "/doc/traag/{id}" ;
                      spiegel:query [ spiegel:file "queries/describe.rq" ; spiegel:dataSource <#slow> ] .
                  """.formatted(main, second, failing, slow));
          Files.writeString(dir.resolve("queries/describe.rq"), """
                  CONSTRUCT { ?resource ?p ?o } WHERE { ?resource ?p ?o }
                  """);
          Files.writeString(dir.resolve("queries/emissie.rq"), """
                  CONSTRUCT { ?resource ?p ?o . ?resource <https://example.org/ns/viaQuery> "emissie" }
                  WHERE { ?resource ?p ?o }
                  """);
          return dir;
      }
  }
  ```
- [ ] Write `DereferenceContractTest`: one test per acceptance criterion, against the whole application with MockMvc and four stub endpoints.

  ```java
  package be.vlaanderen.omgeving.spiegel;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.hamcrest.Matchers.startsWith;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import be.vlaanderen.omgeving.spiegel.adapter.sparql.testing.StubSparqlEndpoint;
  import java.io.IOException;
  import java.nio.charset.StandardCharsets;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.time.Duration;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFParser;
  import org.apache.jena.vocabulary.RDF;
  import org.apache.jena.vocabulary.RDFS;
  import org.apache.jena.vocabulary.SKOS;
  import org.junit.jupiter.api.AfterAll;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Nested;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.CsvSource;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
  import org.springframework.http.HttpMethod;
  import org.springframework.test.context.DynamicPropertyRegistry;
  import org.springframework.test.context.DynamicPropertySource;
  import org.springframework.test.web.servlet.MockMvc;
  import org.springframework.test.web.servlet.MvcResult;
  import org.springframework.test.web.servlet.ResultActions;

  @SpringBootTest
  @AutoConfigureMockMvc
  @DisplayName("Feature 001: dereference a resource as RDF")
  class DereferenceContractTest {

      static final String SITE = "http://data.example.org";
      static final String BASE = "https://data.example.org";
      static final String PROBLEM = "https://w3id.org/spiegel/problem/";
      static final String EX = "https://example.org/ns/";

      static final StubSparqlEndpoint MAIN = StubSparqlEndpoint.serving(load("/contract-data.ttl"));
      static final StubSparqlEndpoint SECOND = StubSparqlEndpoint.serving(RDFParser.fromString(
              "<https://data.example.org/id/vestiging/1> <https://example.org/ns/bron> \"tweede bron\" .",
              Lang.TURTLE).toGraph());
      static final StubSparqlEndpoint FAILING = StubSparqlEndpoint.failing();
      static final StubSparqlEndpoint SLOW = StubSparqlEndpoint.slow(Duration.ofSeconds(3));

      @DynamicPropertySource
      static void tenant(DynamicPropertyRegistry registry) throws IOException {
          Path dir = ContractTenant.write(Files.createTempDirectory("tenant"),
                  MAIN.url(), SECOND.url(), FAILING.url(), SLOW.url());
          registry.add("spiegel.configuration.location", dir::toString);
      }

      @AfterAll
      static void stopStores() {
          MAIN.close();
          SECOND.close();
          FAILING.close();
          SLOW.close();
      }

      @Autowired
      MockMvc mvc;

      @Nested
      @DisplayName("Story 1: look up a thing by its identifier")
      class Story1 {

          @Test
          @DisplayName("AC-1.1 an id URI redirects with 303 to the absolute doc URI, whatever Accept says")
          void idRedirectsToDoc() throws Exception {
              for (String accept : new String[] {"text/turtle", "text/html", "*/*", "application/ld+json"}) {
                  mvc.perform(get(SITE + "/id/organisatie/42").header("Accept", accept))
                          .andExpect(status().isSeeOther())
                          .andExpect(header().string("Location", BASE + "/doc/organisatie/42"));
              }
          }

          @Test
          @DisplayName("AC-1.2 the redirect needs no data and sends no query")
          void redirectSendsNoQuery() throws Exception {
              int before = MAIN.requestCount();
              mvc.perform(get(SITE + "/id/organisatie/999"))
                      .andExpect(status().isSeeOther())
                      .andExpect(header().string("Location", BASE + "/doc/organisatie/999"));
              assertThat(MAIN.requestCount()).isEqualTo(before);
          }

          @Test
          @DisplayName("AC-1.3 the redirect carries over the query string")
          void redirectKeepsQueryString() throws Exception {
              mvc.perform(get(SITE + "/id/organisatie/42?_format=ttl"))
                      .andExpect(header().string("Location", BASE + "/doc/organisatie/42?_format=ttl"));
          }

          @Test
          @DisplayName("AC-1.4 a value that does not match its pattern gives 404 without a query")
          void invalidValueIsNotFound() throws Exception {
              int before = MAIN.requestCount();
              expectProblem(mvc.perform(get(SITE + "/id/organisatie/abc")), 404, "not-found");
              assertThat(MAIN.requestCount()).isEqualTo(before);
          }

          @Test
          @DisplayName("AC-1.6 a trailing slash gives 404")
          void trailingSlashIsNotFound() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/id/organisatie/42/")), 404, "not-found");
          }
      }

      @Nested
      @DisplayName("Story 2: get the description of a thing in RDF")
      class Story2 {

          @Test
          @DisplayName("AC-2.1 the description holds every triple with the subject as subject")
          void subjectTriples() throws Exception {
              Graph g = turtle(SITE + "/doc/organisatie/42");
              Node s = iri(BASE + "/id/organisatie/42");
              assertThat(g.contains(s, RDF.Nodes.type, iri(EX + "Organisatie"))).isTrue();
              assertThat(g.contains(s, RDFS.Nodes.label, NodeFactory.createLiteralLang("Organisatie 42", "nl"))).isTrue();
              assertThat(g.contains(s, iri(EX + "partner"), iri(BASE + "/id/organisatie/7"))).isTrue();
          }

          @Test
          @DisplayName("AC-2.2 blank nodes to depth 3; level 4 only as an object")
          void blankNodeDepth() throws Exception {
              Graph g = turtle(SITE + "/doc/organisatie/42");
              assertThat(g.find(Node.ANY, iri(EX + "niveau4"), Node.ANY).toList()).hasSize(1);
              assertThat(g.find(Node.ANY, iri(EX + "teDiep"), Node.ANY).toList()).isEmpty();
          }

          @Test
          @DisplayName("AC-2.3 label and type of linked resources, also inside blank nodes, and nothing else")
          void labelsOfLinkedResources() throws Exception {
              Graph g = turtle(SITE + "/doc/organisatie/42");
              Node seven = iri(BASE + "/id/organisatie/7");
              Node nine = iri(BASE + "/id/organisatie/9");
              assertThat(g.contains(seven, RDFS.Nodes.label, Node.ANY)).isTrue();
              assertThat(g.contains(seven, RDF.Nodes.type, Node.ANY)).isTrue();
              assertThat(g.contains(nine, SKOS.prefLabel.asNode(), Node.ANY)).isTrue();
              assertThat(g.find(Node.ANY, iri(EX + "geheim"), Node.ANY).toList()).isEmpty();
          }

          @Test
          @DisplayName("AC-2.4 no incoming relations")
          void noIncomingRelations() throws Exception {
              Graph g = turtle(SITE + "/doc/organisatie/42");
              assertThat(g.contains(iri(BASE + "/id/organisatie/8"), Node.ANY, Node.ANY)).isFalse();
          }

          @Test
          @DisplayName("AC-2.5 foaf:primaryTopic and foaf:page link document and thing")
          void documentLinks() throws Exception {
              Graph g = turtle(SITE + "/doc/organisatie/42");
              Node thing = iri(BASE + "/id/organisatie/42");
              Node doc = iri(BASE + "/doc/organisatie/42");
              assertThat(g.contains(doc, iri("http://xmlns.com/foaf/0.1/primaryTopic"), thing)).isTrue();
              assertThat(g.contains(thing, iri("http://xmlns.com/foaf/0.1/page"), doc)).isTrue();
          }

          @Test
          @DisplayName("AC-2.6 an empty description gives 404")
          void emptyDescriptionIsNotFound() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/doc/organisatie/999")), 404, "not-found");
          }

          @Test
          @DisplayName("AC-2.7 two queries on two data sources are merged")
          void twoSourcesMerged() throws Exception {
              Graph g = turtle(SITE + "/doc/vestiging/1");
              Node s = iri(BASE + "/id/vestiging/1");
              assertThat(g.contains(s, RDFS.Nodes.label, Node.ANY)).isTrue();
              assertThat(g.contains(s, iri(EX + "bron"), Node.ANY)).isTrue();
          }

          @Test
          @DisplayName("AC-2.8 a host name that is not configured gives 404")
          void unknownHostIsNotFound() throws Exception {
              expectProblem(mvc.perform(get("http://other.example.org/doc/organisatie/42")), 404, "not-found");
          }

          @Test
          @DisplayName("AC-5.1, AC-5.2 a specific template uses its own query; the generic one the default query")
          void ownQueryForSpecificTemplate() throws Exception {
              Node viaQuery = iri(EX + "viaQuery");
              assertThat(turtle(SITE + "/doc/emissie/5").contains(iri(BASE + "/id/emissie/5"), viaQuery, Node.ANY)).isTrue();
              assertThat(turtle(SITE + "/doc/organisatie/42").contains(Node.ANY, viaQuery, Node.ANY)).isFalse();
          }
      }

      @Nested
      @DisplayName("Story 3: choose the format")
      class Story3 {

          @ParameterizedTest
          @CsvSource({"text/turtle, TURTLE", "application/n-triples, N-TRIPLES",
                      "application/rdf+xml, RDF/XML", "application/ld+json, JSON-LD"})
          @DisplayName("AC-3.1 each format by Accept, same graph")
          void formatByAccept(String type, String lang) throws Exception {
              MvcResult r = mvc.perform(get(SITE + "/doc/organisatie/42").header("Accept", type))
                      .andExpect(status().isOk())
                      .andExpect(header().string("Content-Type", startsWith(type)))
                      .andReturn();
              assertThat(parse(r, lang).isIsomorphicWith(turtle(SITE + "/doc/organisatie/42"))).isTrue();
          }

          @ParameterizedTest
          @CsvSource({"ttl, text/turtle", "nt, application/n-triples",
                      "rdf, application/rdf+xml", "jsonld, application/ld+json"})
          @DisplayName("AC-3.2 a format suffix wins over Accept")
          void formatBySuffix(String suffix, String type) throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42." + suffix).header("Accept", "text/turtle"))
                      .andExpect(status().isOk())
                      .andExpect(header().string("Content-Type", startsWith(type)));
          }

          @Test
          @DisplayName("AC-3.3 _format wins over Accept")
          void formatByParameter() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42?_format=nt").header("Accept", "text/turtle"))
                      .andExpect(header().string("Content-Type", startsWith("application/n-triples")));
          }

          @Test
          @DisplayName("AC-3.4 no Accept, or */*, gives Turtle")
          void defaultIsTurtle() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42"))
                      .andExpect(header().string("Content-Type", startsWith("text/turtle")));
              mvc.perform(get(SITE + "/doc/organisatie/42").header("Accept", "*/*"))
                      .andExpect(header().string("Content-Type", startsWith("text/turtle")));
          }

          @Test
          @DisplayName("AC-3.5 quality values are weighed")
          void qualityValues() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42")
                              .header("Accept", "application/ld+json;q=0.5, text/turtle;q=0.9"))
                      .andExpect(header().string("Content-Type", startsWith("text/turtle")));
          }

          @Test
          @DisplayName("AC-3.6 equal quality follows SPieGeL's preference")
          void preferenceOrder() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42").header("Accept", "application/rdf+xml, application/ld+json"))
                      .andExpect(header().string("Content-Type", startsWith("application/ld+json")));
          }

          @Test
          @DisplayName("AC-3.7 a negotiated response has Vary and Content-Location")
          void negotiatedHeaders() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42").header("Accept", "text/turtle"))
                      .andExpect(header().string("Vary", "Accept"))
                      .andExpect(header().string("Content-Location", BASE + "/doc/organisatie/42.ttl"));
          }

          @Test
          @DisplayName("AC-3.8 a format-specific URL has neither Vary nor Content-Location")
          void formatSpecificHeaders() throws Exception {
              for (String url : new String[] {"/doc/organisatie/42.ttl", "/doc/organisatie/42?_format=ttl"}) {
                  mvc.perform(get(SITE + url))
                          .andExpect(header().doesNotExist("Vary"))
                          .andExpect(header().doesNotExist("Content-Location"));
              }
          }

          @Test
          @DisplayName("AC-3.9 Link names every format")
          void alternateLinks() throws Exception {
              String link = mvc.perform(get(SITE + "/doc/organisatie/42")).andReturn().getResponse().getHeader("Link");
              assertThat(link)
                      .contains("<" + BASE + "/doc/organisatie/42.ttl>; rel=\"alternate\"; type=\"text/turtle\"")
                      .contains("<" + BASE + "/doc/organisatie/42.nt>; rel=\"alternate\"; type=\"application/n-triples\"")
                      .contains("<" + BASE + "/doc/organisatie/42.rdf>; rel=\"alternate\"; type=\"application/rdf+xml\"")
                      .contains("<" + BASE + "/doc/organisatie/42.jsonld>; rel=\"alternate\"; type=\"application/ld+json\"");
          }

          @Test
          @DisplayName("AC-3.10 HEAD gives the status and headers of GET, without a body")
          void headLikeGet() throws Exception {
              for (String url : new String[] {"/doc/organisatie/42", "/id/organisatie/42", "/doc/organisatie/999"}) {
                  var getResponse = mvc.perform(get(SITE + url)).andReturn().getResponse();
                  var headResponse = mvc.perform(head(SITE + url)).andReturn().getResponse();
                  assertThat(headResponse.getStatus()).isEqualTo(getResponse.getStatus());
                  assertThat(headResponse.getContentType()).isEqualTo(getResponse.getContentType());
                  assertThat(headResponse.getHeader("Link")).isEqualTo(getResponse.getHeader("Link"));
                  assertThat(headResponse.getHeader("Location")).isEqualTo(getResponse.getHeader("Location"));
                  assertThat(headResponse.getContentAsByteArray()).isEmpty();
              }
          }

          @Test
          @DisplayName("AC-3.11 only HTML or only JSON gives 406")
          void htmlOrJsonNotAcceptable() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/doc/organisatie/42").header("Accept", "text/html")), 406, "not-acceptable");
              expectProblem(mvc.perform(get(SITE + "/doc/organisatie/42").header("Accept", "application/json")), 406, "not-acceptable");
          }

          @Test
          @DisplayName("AC-3.12 an unknown _format gives 406")
          void unknownFormatNotAcceptable() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/doc/organisatie/42?_format=xyz")), 406, "not-acceptable");
          }

          @Test
          @DisplayName("AC-3.13 a suffix and a _format that disagree give 400")
          void conflictingFormats() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/doc/organisatie/42.ttl?_format=nt")), 400, "invalid-request");
          }

          @Test
          @DisplayName("AC-3.14 RDF/XML asked for explicitly but impossible gives 406 with a detail")
          void rdfXmlImpossible() throws Exception {
              for (var builder : new org.springframework.test.web.servlet.RequestBuilder[] {
                      get(SITE + "/doc/organisatie/43.rdf"),
                      get(SITE + "/doc/organisatie/43?_format=rdf"),
                      get(SITE + "/doc/organisatie/43").header("Accept", "application/rdf+xml")}) {
                  expectProblem(mvc.perform(builder), 406, "not-acceptable")
                          .andExpect(jsonPath("$.detail").value("The description cannot be expressed in RDF/XML."));
              }
          }

          @Test
          @DisplayName("AC-3.15 RDF/XML impossible but negotiated falls back to the next format")
          void rdfXmlFallsBack() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/43").header("Accept", "application/rdf+xml, text/turtle;q=0.5"))
                      .andExpect(status().isOk())
                      .andExpect(header().string("Content-Type", startsWith("text/turtle")));
          }

          @Test
          @DisplayName("AC-3.16 media type parameters are ignored")
          void profileParameterIgnored() throws Exception {
              mvc.perform(get(SITE + "/doc/organisatie/42")
                              .header("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\""))
                      .andExpect(status().isOk())
                      .andExpect(header().string("Content-Type", startsWith("application/ld+json")));
          }
      }

      @Nested
      @DisplayName("Story 4: understand what went wrong")
      class Story4 {

          @Test
          @DisplayName("AC-4.1 an error is problem+json with type, title, status and a urn:uuid instance, whatever was asked")
          void problemShape() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/doc/organisatie/999").header("Accept", "text/turtle")), 404, "not-found")
                      .andExpect(jsonPath("$.title").isString());
          }

          @Test
          @DisplayName("AC-4.2 and AC-4.4 a failing data source gives 502, without its message or the query, and no partial description")
          void sourceError() throws Exception {
              String body = expectProblem(mvc.perform(get(SITE + "/doc/fout/1")), 502, "source-error")
                      .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
              assertThat(body).doesNotContain(StubSparqlEndpoint.STORE_MESSAGE).doesNotContain("CONSTRUCT");
          }

          @Test
          @DisplayName("AC-4.3 a data source that does not answer in time gives 504")
          void sourceTimeout() throws Exception {
              expectProblem(mvc.perform(get(SITE + "/doc/traag/1")), 504, "source-timeout");
          }

          @Test
          @DisplayName("AC-4.5 a 502 or 504 is not stored")
          void noStoreOnGatewayErrors() throws Exception {
              mvc.perform(get(SITE + "/doc/fout/1")).andExpect(header().string("Cache-Control", "no-store"));
              mvc.perform(get(SITE + "/doc/traag/1")).andExpect(header().string("Cache-Control", "no-store"));
          }

          @ParameterizedTest
          @CsvSource({"POST", "PUT", "PATCH", "DELETE"})
          @DisplayName("AC-4.6 other methods give 405 with Allow")
          void methodNotAllowed(String method) throws Exception {
              expectProblem(mvc.perform(request(HttpMethod.valueOf(method), SITE + "/doc/organisatie/42")), 405, "method-not-allowed")
                      .andExpect(header().string("Allow", "GET, HEAD"));
          }
      }

      @Test
      @DisplayName("NFR security: every response carries CORS headers")
      void corsOnEveryResponse() throws Exception {
          for (String url : new String[] {"/id/organisatie/42", "/doc/organisatie/42", "/doc/organisatie/999"}) {
              mvc.perform(get(SITE + url))
                      .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                      .andExpect(header().string("Access-Control-Expose-Headers", "Link, Location, Content-Location"));
          }
      }

      private ResultActions expectProblem(ResultActions actions, int status, String type) throws Exception {
          return actions.andExpect(status().is(status))
                  .andExpect(content().contentType("application/problem+json"))
                  .andExpect(jsonPath("$.type").value(PROBLEM + type))
                  .andExpect(jsonPath("$.status").value(status))
                  .andExpect(jsonPath("$.instance").value(startsWith("urn:uuid:")));
      }

      private Graph turtle(String url) throws Exception {
          MvcResult r = mvc.perform(get(url).header("Accept", "text/turtle")).andExpect(status().isOk()).andReturn();
          return parse(r, "TURTLE");
      }

      private static Graph parse(MvcResult r, String lang) throws Exception {
          Lang l = org.apache.jena.riot.RDFLanguages.nameToLang(lang);
          return RDFParser.fromString(r.getResponse().getContentAsString(StandardCharsets.UTF_8), l).toGraph();
      }

      private static Graph load(String resource) {
          return RDFParser.source(DereferenceContractTest.class.getResource(resource).toString()).toGraph();
      }

      private static Node iri(String iri) {
          return NodeFactory.createURI(iri);
      }
  }
  ```
- [ ] Check that `docs/specs/001-dereference-resource-as-rdf/contracts/api.md` matches the tests; it was written with this plan.
- [ ] Run: `mvn -q -pl spiegel-app -am verify -Dtest=DereferenceContractTest -Dsurefire.failIfNoSpecifiedTests=false` — expect: FAIL (the context does not start: `spiegel.configuration.location` is not used, no controller exists).
- [ ] Run: `mvn -q -pl spiegel-app -am test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false` — expect: PASS (the core has no Jena classes yet).
- [ ] Commit: `test: add the contract and contract tests of feature 001`

---
## Phase 1: Tenant and URI templates in the core

**Implements:** FR-1 | **Satisfies:** AC-1.1, AC-1.3 (core part), AC-1.4, AC-1.6, AC-2.8 (core part), AC-5.2 (template choice)
**Files:** `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/domain/{DataSource,QuerySpec,UriTemplate,ResourceType,Tenant,TemplateKind,TemplateMatch}.java`, `spiegel-core/src/test/java/be/vlaanderen/omgeving/spiegel/core/domain/{UriTemplateTest,TenantTest}.java`
**Interfaces:** Consumes: nothing. Produces:
`record DataSource(String name, URI endpoint, Duration timeout)`;
`record QuerySpec(String name, String text, DataSource dataSource)`;
`final class UriTemplate { static UriTemplate parse(String, Map<String, String> patterns); Optional<Map<String, String>> match(String rawPath); String expand(Map<String, String> rawValues); Set<String> variables(); int literalLength(); int variableCount(); String text() }`;
`record ResourceType(String name, UriTemplate idTemplate, UriTemplate docTemplate, List<QuerySpec> queries)`;
`record Tenant(URI base, int blankNodeDepth, QuerySpec defaultQuery, List<ResourceType> resourceTypes) { boolean serves(String host); Optional<TemplateMatch> match(String rawPath); List<QuerySpec> queriesFor(ResourceType) }`;
`enum TemplateKind { ID, DOC }`;
`record TemplateMatch(URI base, ResourceType type, TemplateKind kind, Map<String, String> rawValues) { URI thingIri(); URI documentUri() }`.

The rules that need no data: which template matches a path, and which IRIs follow from it. Matching compares the decoded value with the variable's pattern, but builds IRIs from the raw segment, which must consist of URI characters only (decision 8).

- [ ] Write `UriTemplateTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import java.util.Map;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-1: URI templates")
  class UriTemplateTest {

      private final UriTemplate generic = UriTemplate.parse("/id/{concept}/{id}", Map.of("id", "^[0-9]+$"));

      @Test
      void matchesAndBindsRawValues() {
          assertThat(generic.match("/id/organisatie/42")).contains(Map.of("concept", "organisatie", "id", "42"));
      }

      @Test
      @DisplayName("AC-1.4 a value that does not match its pattern does not match")
      void patternIsChecked() {
          assertThat(generic.match("/id/organisatie/abc")).isEmpty();
      }

      @Test
      @DisplayName("AC-1.6 a trailing slash is a different path")
      void trailingSlashDoesNotMatch() {
          assertThat(generic.match("/id/organisatie/42/")).isEmpty();
      }

      @Test
      void patternIsCheckedAgainstTheDecodedValue() {
          UriTemplate t = UriTemplate.parse("/id/{concept}/{id}", Map.of("id", "^a b$"));
          assertThat(t.match("/id/x/a%20b")).contains(Map.of("concept", "x", "id", "a%20b"));
      }

      @Test
      @DisplayName("NFR-SEC-04 a segment with characters outside a URI does not match")
      void unsafeSegmentDoesNotMatch() {
          assertThat(generic.match("/id/x>/1")).isEmpty();
          assertThat(generic.match("/id/x\"/1")).isEmpty();
          assertThat(generic.match("/id/%zz/1")).isEmpty();
      }

      @Test
      void defaultPatternRefusesAnEncodedSlash() {
          assertThat(generic.match("/id/a%2Fb/1")).isEmpty();
      }

      @Test
      void literalsMustBeEqual() {
          assertThat(generic.match("/doc/organisatie/42")).isEmpty();
      }

      @Test
      void expandsWithRawValues() {
          assertThat(generic.expand(Map.of("concept", "organisatie", "id", "42"))).isEqualTo("/id/organisatie/42");
      }

      @Test
      void specificity() {
          UriTemplate specific = UriTemplate.parse("/id/emissie/{id}", Map.of());
          assertThat(specific.literalLength()).isGreaterThan(generic.literalLength());
          assertThat(generic.variableCount()).isEqualTo(2);
          assertThat(generic.variables()).containsExactly("concept", "id");
      }

      @Test
      void onlyWholeSegmentVariables() {
          assertThatThrownBy(() -> UriTemplate.parse("/id/x{id}", Map.of()))
                  .isInstanceOf(IllegalArgumentException.class);
      }
  }
  ```
- [ ] Write `TenantTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import static org.assertj.core.api.Assertions.assertThat;

  import java.net.URI;
  import java.time.Duration;
  import java.util.List;
  import java.util.Map;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-1: resolving a path within a tenant")
  class TenantTest {

      static final DataSource MAIN = new DataSource("main", URI.create("http://store/sparql"), Duration.ofSeconds(10));
      static final QuerySpec DEFAULT = new QuerySpec("default", "CONSTRUCT WHERE { ?resource ?p ?o }", MAIN);
      static final QuerySpec EMISSIE = new QuerySpec("queries/emissie.rq", "CONSTRUCT WHERE { ?resource ?p ?o }", MAIN);

      static final ResourceType GENERIC = new ResourceType("generic",
              UriTemplate.parse("/id/{concept}/{id}", Map.of()),
              UriTemplate.parse("/doc/{concept}/{id}", Map.of()), List.of());
      static final ResourceType EMISSIE_TYPE = new ResourceType("emissie",
              UriTemplate.parse("/id/emissie/{id}", Map.of()),
              UriTemplate.parse("/doc/emissie/{id}", Map.of()), List.of(EMISSIE));

      static final Tenant TENANT = new Tenant(URI.create("https://data.example.org"), 3, DEFAULT,
              List.of(GENERIC, EMISSIE_TYPE));

      @Test
      @DisplayName("AC-2.8 a tenant serves only the host of its base")
      void servesItsHostOnly() {
          assertThat(TENANT.serves("data.example.org")).isTrue();
          assertThat(TENANT.serves("DATA.example.org")).isTrue();
          assertThat(TENANT.serves("other.example.org")).isFalse();
          assertThat(TENANT.serves(null)).isFalse();
      }

      @Test
      @DisplayName("AC-1.1 an id path yields the thing IRI and the absolute doc URI")
      void idMatch() {
          TemplateMatch m = TENANT.match("/id/organisatie/42").orElseThrow();
          assertThat(m.kind()).isEqualTo(TemplateKind.ID);
          assertThat(m.thingIri()).hasToString("https://data.example.org/id/organisatie/42");
          assertThat(m.documentUri()).hasToString("https://data.example.org/doc/organisatie/42");
      }

      @Test
      @DisplayName("AC-5.2 the most specific template wins")
      void mostSpecificWins() {
          TemplateMatch m = TENANT.match("/doc/emissie/5").orElseThrow();
          assertThat(m.type()).isEqualTo(EMISSIE_TYPE);
          assertThat(m.kind()).isEqualTo(TemplateKind.DOC);
          assertThat(TENANT.queriesFor(m.type())).containsExactly(EMISSIE);
          assertThat(TENANT.queriesFor(TENANT.match("/doc/organisatie/42").orElseThrow().type()))
                  .containsExactly(DEFAULT);
      }

      @Test
      void noTemplateMatches() {
          assertThat(TENANT.match("/elders/1")).isEmpty();
          assertThat(TENANT.match("/")).isEmpty();
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-core test` — expect: FAIL (compilation: types do not exist).
- [ ] Implement the records `DataSource`, `QuerySpec`, `ResourceType` (one line each, with Javadoc naming the requirement) and `TemplateKind`:

  ```java
  /** A SPARQL endpoint that descriptions are read from (FR-DS-01), with its time-out (AC-5.5). */
  public record DataSource(String name, URI endpoint, Duration timeout) {
  }

  /** A query as text, named by its file, and the data source it runs on (FR-RA-02, ADR 0010). */
  public record QuerySpec(String name, String text, DataSource dataSource) {
  }

  /** A kind of resource: its {@code id} and {@code doc} templates and the queries that describe it (ADR 0012). */
  public record ResourceType(String name, UriTemplate idTemplate, UriTemplate docTemplate, List<QuerySpec> queries) {

      public ResourceType {
          queries = List.copyOf(queries);
      }
  }

  /** Whether a template identifies a thing ({@code id}) or the document that describes it ({@code doc}). */
  public enum TemplateKind {
      ID,
      DOC
  }
  ```
- [ ] Implement `UriTemplate`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import java.net.URLDecoder;
  import java.nio.charset.StandardCharsets;
  import java.util.ArrayList;
  import java.util.LinkedHashMap;
  import java.util.LinkedHashSet;
  import java.util.List;
  import java.util.Map;
  import java.util.Optional;
  import java.util.Set;
  import java.util.regex.Pattern;

  /**
   * A path pattern such as {@code /id/{concept}/{id}}, whose variables are whole segments typed by a pattern
   * (FR-URI-01, ADR 0012).
   *
   * <p>A value is checked against its pattern after percent-decoding, but kept and expanded in its raw form, so
   * that an IRI is never built from decoded text. A raw segment must consist of URI characters only.
   */
  public final class UriTemplate {

      /** The pattern of a variable without one: any non-empty value without a slash. */
      public static final String DEFAULT_PATTERN = "^[^/]+$";

      private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}");
      private static final Pattern RAW_SEGMENT =
              Pattern.compile("(?:[A-Za-z0-9\\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})+");

      private final String text;
      private final List<String> literals;
      private final List<String> names;
      private final Map<String, Pattern> patterns;

      private UriTemplate(String text, List<String> literals, List<String> names, Map<String, Pattern> patterns) {
          this.text = text;
          this.literals = literals;
          this.names = names;
          this.patterns = patterns;
      }

      /**
       * Parses a template.
       *
       * @param text the template, starting with {@code /}
       * @param patterns the pattern per variable name; a variable without one gets {@link #DEFAULT_PATTERN}
       * @throws IllegalArgumentException if a segment mixes text and a variable, or the text is no path
       */
      public static UriTemplate parse(String text, Map<String, String> patterns) {
          if (!text.startsWith("/")) {
              throw new IllegalArgumentException("A URI template must start with '/': " + text);
          }
          List<String> literals = new ArrayList<>();
          List<String> names = new ArrayList<>();
          Map<String, Pattern> compiled = new LinkedHashMap<>();
          for (String segment : text.substring(1).split("/", -1)) {
              var variable = VARIABLE.matcher(segment);
              if (variable.matches()) {
                  String name = variable.group(1);
                  literals.add(null);
                  names.add(name);
                  compiled.put(name, Pattern.compile(patterns.getOrDefault(name, DEFAULT_PATTERN)));
              } else if (segment.isEmpty() || segment.contains("{") || segment.contains("}")) {
                  throw new IllegalArgumentException("Each segment must be text or one whole variable: " + text);
              } else {
                  literals.add(segment);
                  names.add(null);
              }
          }
          return new UriTemplate(text, List.copyOf(literals), names, compiled);
      }

      /**
       * Matches a raw (still percent-encoded) path.
       *
       * @return the raw value of each variable, or empty if the path does not match
       */
      public Optional<Map<String, String>> match(String rawPath) {
          if (rawPath == null || !rawPath.startsWith("/")) {
              return Optional.empty();
          }
          String[] segments = rawPath.substring(1).split("/", -1);
          if (segments.length != literals.size()) {
              return Optional.empty();
          }
          Map<String, String> values = new LinkedHashMap<>();
          for (int i = 0; i < segments.length; i++) {
              String segment = segments[i];
              if (literals.get(i) != null) {
                  if (!literals.get(i).equals(segment)) {
                      return Optional.empty();
                  }
              } else {
                  Optional<String> decoded = decode(segment);
                  if (decoded.isEmpty() || !patterns.get(names.get(i)).matcher(decoded.get()).find()) {
                      return Optional.empty();
                  }
                  values.put(names.get(i), segment);
              }
          }
          return Optional.of(values);
      }

      /** Fills in raw values, as returned by {@link #match(String)}. */
      public String expand(Map<String, String> rawValues) {
          var path = new StringBuilder();
          for (int i = 0; i < literals.size(); i++) {
              path.append('/').append(literals.get(i) != null ? literals.get(i) : rawValues.get(names.get(i)));
          }
          return path.toString();
      }

      public Set<String> variables() {
          return new LinkedHashSet<>(patterns.keySet());
      }

      /** The number of literal characters, the first measure of specificity. */
      public int literalLength() {
          return text.replaceAll(VARIABLE.pattern(), "").length();
      }

      /** The number of variables, the second measure of specificity. */
      public int variableCount() {
          return patterns.size();
      }

      public String text() {
          return text;
      }

      private static Optional<String> decode(String raw) {
          if (!RAW_SEGMENT.matcher(raw).matches()) {
              return Optional.empty();
          }
          try {
              return Optional.of(URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8));
          } catch (IllegalArgumentException e) {
              return Optional.empty();
          }
      }

      @Override
      public String toString() {
          return text;
      }
  }
  ```
- [ ] Implement `TemplateMatch` and `Tenant`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import java.net.URI;
  import java.util.Map;

  /** The template a path matched, with its raw values, and the IRIs that follow from it (FR-1). */
  public record TemplateMatch(URI base, ResourceType type, TemplateKind kind, Map<String, String> rawValues) {

      /** The IRI of the thing, formed from the configured base, never from the request. */
      public URI thingIri() {
          return URI.create(base + type.idTemplate().expand(rawValues));
      }

      /** The URI of the document that describes the thing. */
      public URI documentUri() {
          return URI.create(base + type.docTemplate().expand(rawValues));
      }
  }
  ```

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import java.net.URI;
  import java.util.Comparator;
  import java.util.List;
  import java.util.Locale;
  import java.util.Optional;
  import java.util.stream.Stream;

  /**
   * A domain served by SPieGeL: its base, from which every IRI is formed, the depth of blank nodes, SPieGeL's
   * default description query and its resource types (FR-MT-01, ADR 0012).
   */
  public record Tenant(URI base, int blankNodeDepth, QuerySpec defaultQuery, List<ResourceType> resourceTypes) {

      public Tenant {
          String text = base.toString();
          base = URI.create(text.endsWith("/") ? text.substring(0, text.length() - 1) : text);
          resourceTypes = List.copyOf(resourceTypes);
      }

      /** Tells whether a request for this host name belongs to this tenant (FR-MT-01). */
      public boolean serves(String host) {
          return host != null && host.toLowerCase(Locale.ROOT).equals(base.getHost().toLowerCase(Locale.ROOT));
      }

      /**
       * Finds the most specific template that matches a raw path: more literal characters, then more variables
       * (request flow, step 4).
       */
      public Optional<TemplateMatch> match(String rawPath) {
          return resourceTypes.stream()
                  .flatMap(type -> Stream.of(
                          type.idTemplate().match(rawPath)
                                  .map(values -> new TemplateMatch(base, type, TemplateKind.ID, values)),
                          type.docTemplate().match(rawPath)
                                  .map(values -> new TemplateMatch(base, type, TemplateKind.DOC, values))))
                  .flatMap(Optional::stream)
                  .max(Comparator.<TemplateMatch>comparingInt(m -> template(m).literalLength())
                          .thenComparingInt(m -> template(m).variableCount()));
      }

      /** The queries that describe a resource type: its own, or SPieGeL's default query (FR-RA-02). */
      public List<QuerySpec> queriesFor(ResourceType type) {
          return type.queries().isEmpty() ? List.of(defaultQuery) : type.queries();
      }

      private static UriTemplate template(TemplateMatch match) {
          return match.kind() == TemplateKind.ID ? match.type().idTemplate() : match.type().docTemplate();
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-core test` — expect: PASS.
- [ ] Commit: `feat: match URI templates within a tenant`

---
## Phase 2: The description in the core

**Implements:** FR-2 (except loading queries) | **Satisfies:** AC-2.1 to AC-2.7, AC-4.4 (core part)
**Files:** `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/domain/{ResourceDescription,DescriptionBounds}.java`, `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/port/out/{DataSourcePort,DataSourceException,DataSourceFailure,DataSourceTimeout}.java`, `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/port/in/DereferenceResource.java`, tests `DescriptionBoundsTest`, `DereferenceResourceTest`
**Interfaces:** Consumes: `Tenant`, `TemplateMatch`, `QuerySpec`, `DataSource` (Phase 1). Produces:
`record ResourceDescription(Graph graph, Node subject, Node document)`;
`final class DescriptionBounds { static Graph bound(Graph source, Node subject, int depth) }`;
`interface DataSourcePort { Graph describe(QuerySpec query, Node resource); }`;
`abstract sealed class DataSourceException extends RuntimeException permits DataSourceFailure, DataSourceTimeout { String dataSourceName() }` with constructors `DataSourceFailure(String dataSourceName, Throwable cause)`, `DataSourceTimeout(String dataSourceName, Throwable cause)`;
`final class DereferenceResource { DereferenceResource(DataSourcePort); Optional<ResourceDescription> describe(Tenant, TemplateMatch) }`.

Run the queries of a resource type, concurrently, merge the results, bound the description to its subject, and add the links between document and thing. A failed query fails the whole description.

- [ ] Write `DescriptionBoundsTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import static org.assertj.core.api.Assertions.assertThat;

  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.GraphMemFactory;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.graph.Triple;
  import org.apache.jena.vocabulary.RDF;
  import org.apache.jena.vocabulary.RDFS;
  import org.apache.jena.vocabulary.SKOS;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-2: a description is bounded to its subject")
  class DescriptionBoundsTest {

      static final Node S = iri("https://data.example.org/id/organisatie/42");
      static final Node OTHER = iri("https://data.example.org/id/organisatie/7");
      static final Node NINE = iri("https://data.example.org/id/organisatie/9");

      @Test
      @DisplayName("AC-2.2 blank nodes to depth 3; the triple to level 4 stays, level 4's own triples do not")
      void blankNodeDepth() {
          Graph g = GraphMemFactory.createDefaultGraph();
          Node b1 = NodeFactory.createBlankNode();
          Node b2 = NodeFactory.createBlankNode();
          Node b3 = NodeFactory.createBlankNode();
          Node b4 = NodeFactory.createBlankNode();
          g.add(Triple.create(S, p("level1"), b1));
          g.add(Triple.create(b1, p("level2"), b2));
          g.add(Triple.create(b2, p("level3"), b3));
          g.add(Triple.create(b3, p("level4"), b4));
          g.add(Triple.create(b4, p("tooDeep"), NodeFactory.createLiteralString("x")));

          Graph bounded = DescriptionBounds.bound(g, S, 3);

          assertThat(bounded.size()).isEqualTo(4);
          assertThat(bounded.contains(b3, p("level4"), b4)).isTrue();
          assertThat(bounded.contains(b4, Node.ANY, Node.ANY)).isFalse();
      }

      @Test
      @DisplayName("AC-2.3 label and type of linked IRIs, also inside blank nodes, and nothing else")
      void labelAndTypeOnly() {
          Graph g = GraphMemFactory.createDefaultGraph();
          Node b1 = NodeFactory.createBlankNode();
          g.add(Triple.create(S, p("partner"), OTHER));
          g.add(Triple.create(OTHER, RDFS.Nodes.label, NodeFactory.createLiteralString("Seven")));
          g.add(Triple.create(OTHER, RDF.Nodes.type, p("Organisatie")));
          g.add(Triple.create(OTHER, p("secret"), NodeFactory.createLiteralString("no")));
          g.add(Triple.create(S, p("address"), b1));
          g.add(Triple.create(b1, p("municipality"), NINE));
          g.add(Triple.create(NINE, SKOS.prefLabel.asNode(), NodeFactory.createLiteralString("Nine")));
          g.add(Triple.create(NINE, p("secret"), NodeFactory.createLiteralString("no")));

          Graph bounded = DescriptionBounds.bound(g, S, 3);

          assertThat(bounded.contains(OTHER, RDFS.Nodes.label, Node.ANY)).isTrue();
          assertThat(bounded.contains(OTHER, RDF.Nodes.type, Node.ANY)).isTrue();
          assertThat(bounded.contains(NINE, SKOS.prefLabel.asNode(), Node.ANY)).isTrue();
          assertThat(bounded.contains(Node.ANY, p("secret"), Node.ANY)).isFalse();
      }

      @Test
      @DisplayName("AC-2.4 no incoming relations")
      void noIncomingRelations() {
          Graph g = GraphMemFactory.createDefaultGraph();
          g.add(Triple.create(S, p("name"), NodeFactory.createLiteralString("Forty-two")));
          g.add(Triple.create(OTHER, p("partner"), S));

          Graph bounded = DescriptionBounds.bound(g, S, 3);

          assertThat(bounded.contains(OTHER, Node.ANY, Node.ANY)).isFalse();
      }

      @Test
      void cyclicBlankNodesEnd() {
          Graph g = GraphMemFactory.createDefaultGraph();
          Node b1 = NodeFactory.createBlankNode();
          Node b2 = NodeFactory.createBlankNode();
          g.add(Triple.create(S, p("a"), b1));
          g.add(Triple.create(b1, p("b"), b2));
          g.add(Triple.create(b2, p("c"), b1));

          assertThat(DescriptionBounds.bound(g, S, 10).size()).isEqualTo(3);
      }

      static Node iri(String iri) {
          return NodeFactory.createURI(iri);
      }

      static Node p(String local) {
          return iri("https://example.org/ns/" + local);
      }
  }
  ```
- [ ] Write `DereferenceResourceTest`, with the data source port as a lambda:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.port.in;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import be.vlaanderen.omgeving.spiegel.core.domain.DataSource;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceDescription;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceType;
  import be.vlaanderen.omgeving.spiegel.core.domain.TemplateMatch;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.domain.UriTemplate;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceFailure;
  import java.net.URI;
  import java.time.Duration;
  import java.util.List;
  import java.util.Map;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.GraphMemFactory;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.graph.Triple;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-2: DereferenceResource")
  class DereferenceResourceTest {

      static final DataSource A = new DataSource("a", URI.create("http://a/sparql"), Duration.ofSeconds(10));
      static final DataSource B = new DataSource("b", URI.create("http://b/sparql"), Duration.ofSeconds(10));
      static final QuerySpec QA = new QuerySpec("a.rq", "CONSTRUCT WHERE { ?resource ?p ?o }", A);
      static final QuerySpec QB = new QuerySpec("b.rq", "CONSTRUCT WHERE { ?resource ?p ?o }", B);
      static final ResourceType TYPE = new ResourceType("t",
              UriTemplate.parse("/id/{concept}/{id}", Map.of()),
              UriTemplate.parse("/doc/{concept}/{id}", Map.of()), List.of(QA, QB));
      static final Tenant TENANT = new Tenant(URI.create("https://data.example.org"), 3, QA, List.of(TYPE));
      static final Node S = NodeFactory.createURI("https://data.example.org/id/organisatie/42");
      static final Node DOC = NodeFactory.createURI("https://data.example.org/doc/organisatie/42");

      @Test
      @DisplayName("AC-2.1, AC-2.7 the results of both data sources are merged, with the resource bound as a term")
      void mergesResults() {
          var use = new DereferenceResource((query, resource) -> {
              assertThat(resource).isEqualTo(S);
              return graph(Triple.create(resource, p(query.dataSource().name()), NodeFactory.createLiteralString("x")));
          });

          ResourceDescription d = use.describe(TENANT, match()).orElseThrow();

          assertThat(d.graph().contains(S, p("a"), Node.ANY)).isTrue();
          assertThat(d.graph().contains(S, p("b"), Node.ANY)).isTrue();
      }

      @Test
      @DisplayName("AC-2.5 foaf:primaryTopic and foaf:page are added")
      void addsDocumentLinks() {
          var use = new DereferenceResource((query, resource) ->
                  graph(Triple.create(resource, p("x"), NodeFactory.createLiteralString("x"))));

          ResourceDescription d = use.describe(TENANT, match()).orElseThrow();

          assertThat(d.document()).isEqualTo(DOC);
          assertThat(d.graph().contains(DOC, NodeFactory.createURI("http://xmlns.com/foaf/0.1/primaryTopic"), S)).isTrue();
          assertThat(d.graph().contains(S, NodeFactory.createURI("http://xmlns.com/foaf/0.1/page"), DOC)).isTrue();
      }

      @Test
      @DisplayName("AC-2.6 nothing about the subject is not found")
      void emptyIsNotFound() {
          var use = new DereferenceResource((query, resource) ->
                  graph(Triple.create(p("other"), p("x"), resource)));

          assertThat(use.describe(TENANT, match())).isEmpty();
      }

      @Test
      @DisplayName("AC-4.4 one failing query fails the description")
      void oneFailureFailsAll() {
          var use = new DereferenceResource((query, resource) -> {
              if (query == QB) {
                  throw new DataSourceFailure("b", new RuntimeException("store detail"));
              }
              return graph(Triple.create(resource, p("a"), NodeFactory.createLiteralString("x")));
          });

          assertThatThrownBy(() -> use.describe(TENANT, match())).isInstanceOf(DataSourceFailure.class);
      }

      private static TemplateMatch match() {
          return TENANT.match("/doc/organisatie/42").orElseThrow();
      }

      private static Graph graph(Triple... triples) {
          Graph g = GraphMemFactory.createDefaultGraph();
          for (Triple t : triples) {
              g.add(t);
          }
          return g;
      }

      private static Node p(String local) {
          return NodeFactory.createURI("https://example.org/ns/" + local);
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-core test` — expect: FAIL (compilation).
- [ ] Implement `ResourceDescription` and `DescriptionBounds`:

  ```java
  /** The merged, bounded description of a thing, independent of format (ADR 0010). */
  public record ResourceDescription(Graph graph, Node subject, Node document) {
  }
  ```

  ```java
  package be.vlaanderen.omgeving.spiegel.core.domain;

  import java.util.HashSet;
  import java.util.Set;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.GraphMemFactory;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.Triple;
  import org.apache.jena.vocabulary.DCTerms;
  import org.apache.jena.vocabulary.RDF;
  import org.apache.jena.vocabulary.RDFS;
  import org.apache.jena.vocabulary.SKOS;

  /**
   * Bounds a description to its subject (FR-RA-04, FR-RA-06): the subject's own triples, blank nodes nested to a
   * depth, and only the label and type of every IRI that is an object. Whatever the queries returned beyond that,
   * such as incoming relations, is left out.
   */
  public final class DescriptionBounds {

      /** The properties kept for a linked resource. */
      public static final Set<Node> LABEL_AND_TYPE = Set.of(
              RDF.Nodes.type, RDFS.Nodes.label, SKOS.prefLabel.asNode(), DCTerms.title.asNode());

      private DescriptionBounds() {
      }

      /**
       * @param depth the number of nested blank-node levels to keep; level 1 is a value of the subject
       */
      public static Graph bound(Graph source, Node subject, int depth) {
          Graph result = GraphMemFactory.createDefaultGraph();
          copy(source, result, subject, 0, depth, new HashSet<>());
          return result;
      }

      private static void copy(Graph source, Graph result, Node node, int level, int depth, Set<Node> visited) {
          if (level > depth || !visited.add(node)) {
              return;
          }
          for (Triple triple : source.find(node, Node.ANY, Node.ANY).toList()) {
              result.add(triple);
              Node object = triple.getObject();
              if (object.isBlank()) {
                  copy(source, result, object, level + 1, depth, visited);
              } else if (object.isURI()) {
                  for (Node property : LABEL_AND_TYPE) {
                      source.find(object, property, Node.ANY).forEach(result::add);
                  }
              }
          }
      }
  }
  ```
- [ ] Implement the outbound port and its exceptions in `core.port.out`:

  ```java
  /** Runs a description query on its data source, with the resource bound as a term (FR-DS-01, NFR-SEC-04). */
  public interface DataSourcePort {

      /**
       * @return the graph the query constructs or describes
       * @throws DataSourceFailure if the data source answers with an error or an unreadable result
       * @throws DataSourceTimeout if it does not answer within its time-out
       */
      Graph describe(QuerySpec query, Node resource);
  }
  ```

  ```java
  /**
   * A data source could not deliver a result. The message names the data source only; the cause is for the log
   * and never reaches a client (ADR 0016).
   */
  public abstract sealed class DataSourceException extends RuntimeException permits DataSourceFailure, DataSourceTimeout {

      private final String dataSourceName;

      protected DataSourceException(String message, String dataSourceName, Throwable cause) {
          super(message, cause);
          this.dataSourceName = dataSourceName;
      }

      public String dataSourceName() {
          return dataSourceName;
      }
  }
  ```

  ```java
  /** A data source answered with an error, or with a result that cannot be read (E2). */
  public final class DataSourceFailure extends DataSourceException {

      public DataSourceFailure(String dataSourceName, Throwable cause) {
          super("Data source '" + dataSourceName + "' failed", dataSourceName, cause);
      }
  }
  ```

  ```java
  /** A data source did not answer within its time-out (E3). */
  public final class DataSourceTimeout extends DataSourceException {

      public DataSourceTimeout(String dataSourceName, Throwable cause) {
          super("Data source '" + dataSourceName + "' timed out", dataSourceName, cause);
      }
  }
  ```
- [ ] Implement `DereferenceResource`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.port.in;

  import be.vlaanderen.omgeving.spiegel.core.domain.DescriptionBounds;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceDescription;
  import be.vlaanderen.omgeving.spiegel.core.domain.TemplateMatch;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourcePort;
  import java.util.List;
  import java.util.Optional;
  import java.util.concurrent.ExecutionException;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;
  import java.util.concurrent.Future;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.GraphMemFactory;
  import org.apache.jena.graph.GraphUtil;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.graph.Triple;

  /**
   * Describes the thing behind a {@code doc} URI: runs the queries of its resource type, merges and bounds the
   * results, and links document and thing (request flow, steps 9 to 12).
   */
  public final class DereferenceResource {

      static final Node FOAF_PRIMARY_TOPIC = NodeFactory.createURI("http://xmlns.com/foaf/0.1/primaryTopic");
      static final Node FOAF_PAGE = NodeFactory.createURI("http://xmlns.com/foaf/0.1/page");

      private final DataSourcePort dataSources;

      public DereferenceResource(DataSourcePort dataSources) {
          this.dataSources = dataSources;
      }

      /**
       * @return the description, or empty when the data holds nothing about the subject (FR-URI-06)
       * @throws be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceException if any query fails; no partial
       *     description is returned
       */
      public Optional<ResourceDescription> describe(Tenant tenant, TemplateMatch match) {
          Node subject = NodeFactory.createURI(match.thingIri().toString());
          Node document = NodeFactory.createURI(match.documentUri().toString());
          Graph merged = GraphMemFactory.createDefaultGraph();
          for (Graph result : run(tenant.queriesFor(match.type()), subject)) {
              GraphUtil.addInto(merged, result);
          }
          Graph bounded = DescriptionBounds.bound(merged, subject, tenant.blankNodeDepth());
          if (!bounded.contains(subject, Node.ANY, Node.ANY)) {
              return Optional.empty();
          }
          bounded.add(Triple.create(document, FOAF_PRIMARY_TOPIC, subject));
          bounded.add(Triple.create(subject, FOAF_PAGE, document));
          return Optional.of(new ResourceDescription(bounded, subject, document));
      }

      private List<Graph> run(List<QuerySpec> queries, Node subject) {
          if (queries.size() == 1) {
              return List.of(dataSources.describe(queries.getFirst(), subject));
          }
          try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
              List<Future<Graph>> futures = queries.stream()
                      .map(query -> executor.submit(() -> dataSources.describe(query, subject)))
                      .toList();
              try {
                  return futures.stream().map(DereferenceResource::join).toList();
              } catch (RuntimeException e) {
                  executor.shutdownNow();
                  throw e;
              }
          }
      }

      private static Graph join(Future<Graph> future) {
          try {
              return future.get();
          } catch (ExecutionException e) {
              if (e.getCause() instanceof RuntimeException cause) {
                  throw cause;
              }
              throw new IllegalStateException(e.getCause());
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(e);
          }
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-core test` — expect: PASS. Then `mvn -q -pl spiegel-app -am test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false` — expect: PASS (only `org.apache.jena.graph` and `org.apache.jena.vocabulary` in the core).
- [ ] Commit: `feat: describe a resource from its queries, bounded to its subject`

---
## Phase 3: Query policy and loading a configuration in the core

**Implements:** FR-2 (only `CONSTRUCT` and `DESCRIBE`), NFR reliability (no partial configuration) | **Satisfies:** AC-5.3 (core part)
**Files:** `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/domain/{QueryForm,QueryPolicy}.java`, `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/port/out/{QueryInspector,TenantConfigurationPort,InvalidConfigurationException}.java`, `spiegel-core/src/main/java/be/vlaanderen/omgeving/spiegel/core/port/in/LoadTenantConfiguration.java`, test `LoadTenantConfigurationTest`
**Interfaces:** Consumes: `Tenant`, `QuerySpec` (Phase 1). Produces:
`enum QueryForm { SELECT, CONSTRUCT, ASK, DESCRIBE }`;
`record QueryPolicy(Set<QueryForm> allowedForms) { static final QueryPolicy DESCRIPTION }`;
`interface QueryInspector { List<String> violations(String queryText, QueryPolicy policy); }`;
`interface TenantConfigurationPort { Tenant load(); }`;
`final class InvalidConfigurationException extends RuntimeException { InvalidConfigurationException(List<String> violations); List<String> violations() }`;
`final class LoadTenantConfiguration { LoadTenantConfiguration(TenantConfigurationPort, QueryInspector); Tenant load() }`.

ADR 0011 puts the check in the core, reached through a port, so that it cannot be bypassed and the configuration adapter does not depend on the SPARQL adapter. One violation refuses the whole configuration.

- [ ] Write `LoadTenantConfigurationTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.core.port.in;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import be.vlaanderen.omgeving.spiegel.core.domain.DataSource;
  import be.vlaanderen.omgeving.spiegel.core.domain.QueryPolicy;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceType;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.domain.UriTemplate;
  import be.vlaanderen.omgeving.spiegel.core.port.out.InvalidConfigurationException;
  import java.net.URI;
  import java.time.Duration;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("AC-5.3: every description query is checked when the configuration loads")
  class LoadTenantConfigurationTest {

      static final DataSource MAIN = new DataSource("main", URI.create("http://store/sparql"), Duration.ofSeconds(10));
      static final QuerySpec DEFAULT = new QuerySpec("default description query", "good", MAIN);
      static final QuerySpec BAD = new QuerySpec("queries/bad.rq", "bad", MAIN);

      @Test
      void everyQueryIsInspectedWithTheDescriptionPolicy() {
          List<String> inspected = new ArrayList<>();
          var load = new LoadTenantConfiguration(() -> tenant(List.of()), (text, policy) -> {
              assertThat(policy).isEqualTo(QueryPolicy.DESCRIPTION);
              inspected.add(text);
              return List.of();
          });

          assertThat(load.load().base()).hasToString("https://data.example.org");
          assertThat(inspected).containsExactly("good");
      }

      @Test
      void aViolationRefusesTheConfigurationAndNamesTheFile() {
          var load = new LoadTenantConfiguration(() -> tenant(List.of(BAD)),
                  (text, policy) -> text.equals("bad") ? List.of("SERVICE is not allowed") : List.of());

          assertThatThrownBy(load::load)
                  .isInstanceOf(InvalidConfigurationException.class)
                  .satisfies(e -> assertThat(((InvalidConfigurationException) e).violations())
                          .containsExactly("queries/bad.rq: SERVICE is not allowed"));
      }

      private static Tenant tenant(List<QuerySpec> queries) {
          var type = new ResourceType("t", UriTemplate.parse("/id/{c}/{id}", Map.of()),
                  UriTemplate.parse("/doc/{c}/{id}", Map.of()), queries);
          return new Tenant(URI.create("https://data.example.org"), 3, DEFAULT, List.of(type));
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-core test` — expect: FAIL (compilation).
- [ ] Implement:

  ```java
  /** The forms a SPARQL query can take (ADR 0011). */
  public enum QueryForm {
      SELECT,
      CONSTRUCT,
      ASK,
      DESCRIBE
  }
  ```

  ```java
  /**
   * What a query may do: the allowed forms. {@code SERVICE} and updates are never allowed (ADR 0011).
   */
  public record QueryPolicy(Set<QueryForm> allowedForms) {

      /** The policy for description queries: they must yield a graph (FR-2). */
      public static final QueryPolicy DESCRIPTION = new QueryPolicy(EnumSet.of(QueryForm.CONSTRUCT, QueryForm.DESCRIBE));

      public QueryPolicy {
          allowedForms = Set.copyOf(allowedForms);
      }
  }
  ```

  ```java
  /** Checks query text against a policy (ADR 0011). */
  public interface QueryInspector {

      /** @return a reason for every violation; empty if the query is allowed */
      List<String> violations(String queryText, QueryPolicy policy);
  }
  ```

  ```java
  /** Reads the tenant configuration (ADR 0012, ADR 0013). */
  public interface TenantConfigurationPort {

      /** @throws InvalidConfigurationException naming every violation */
      Tenant load();
  }
  ```

  ```java
  /** A configuration that SPieGeL refuses; it names every violation (AC-5.3, AC-5.4). */
  public final class InvalidConfigurationException extends RuntimeException {

      private final List<String> violations;

      public InvalidConfigurationException(List<String> violations) {
          super("The tenant configuration is refused:" + violations.stream()
                  .map(violation -> System.lineSeparator() + " - " + violation)
                  .collect(Collectors.joining()));
          this.violations = List.copyOf(violations);
      }

      public List<String> violations() {
          return violations;
      }
  }
  ```

  ```java
  package be.vlaanderen.omgeving.spiegel.core.port.in;

  import be.vlaanderen.omgeving.spiegel.core.domain.QueryPolicy;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceType;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.port.out.InvalidConfigurationException;
  import be.vlaanderen.omgeving.spiegel.core.port.out.QueryInspector;
  import be.vlaanderen.omgeving.spiegel.core.port.out.TenantConfigurationPort;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;
  import java.util.stream.Stream;

  /**
   * Loads the tenant configuration and checks every description query against the query policy, so that a
   * faulty query refuses the configuration before any request is served (ADR 0011, decision 3).
   */
  public final class LoadTenantConfiguration {

      private final TenantConfigurationPort configuration;
      private final QueryInspector inspector;

      public LoadTenantConfiguration(TenantConfigurationPort configuration, QueryInspector inspector) {
          this.configuration = configuration;
          this.inspector = inspector;
      }

      public Tenant load() {
          Tenant tenant = configuration.load();
          Map<String, QuerySpec> queries = new LinkedHashMap<>();
          Stream.concat(Stream.of(tenant.defaultQuery()),
                          tenant.resourceTypes().stream().map(ResourceType::queries).flatMap(List::stream))
                  .forEach(query -> queries.putIfAbsent(query.name() + "\u0000" + query.text(), query));
          List<String> violations = queries.values().stream()
                  .flatMap(query -> inspector.violations(query.text(), QueryPolicy.DESCRIPTION).stream()
                          .map(reason -> query.name() + ": " + reason))
                  .distinct()
                  .toList();
          if (!violations.isEmpty()) {
              throw new InvalidConfigurationException(violations);
          }
          return tenant;
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-core test` — expect: PASS.
- [ ] Commit: `feat: refuse a configuration whose description queries break the query policy`

---
## Phase 4: SPARQL adapter

**Implements:** FR-2 (running queries), FR-4 (source errors), NFR-SEC-04, NFR-SEC-06 | **Satisfies:** AC-4.2, AC-4.3 (adapter part), AC-5.3 (inspection)
**Files:** `spiegel-adapter-sparql/src/main/java/be/vlaanderen/omgeving/spiegel/adapter/sparql/{SparqlDataSource,SparqlQueryInspector}.java`, tests `SparqlDataSourceTest`, `SparqlQueryInspectorTest`
**Interfaces:** Consumes: `DataSourcePort`, `DataSourceFailure`, `DataSourceTimeout`, `QueryInspector`, `QueryPolicy`, `QueryForm`, `QuerySpec`, `DataSource`; `StubSparqlEndpoint` (Phase 0). Produces: `SparqlDataSource implements DataSourcePort` (no-argument constructor); `SparqlQueryInspector implements QueryInspector` (no-argument constructor).

Run a description query over the SPARQL 1.1 Protocol with Jena, the resource bound as a term by `substitution`, asking the store for N-Triples, within the data source's time-out. Inspect query text by parsing it strictly and walking its algebra (decision 7).

- [ ] Write `SparqlDataSourceTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.sparql;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import be.vlaanderen.omgeving.spiegel.adapter.sparql.testing.StubSparqlEndpoint;
  import be.vlaanderen.omgeving.spiegel.core.domain.DataSource;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceFailure;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceTimeout;
  import java.net.URI;
  import java.time.Duration;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFParser;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-2, FR-4: running description queries over SPARQL")
  class SparqlDataSourceTest {

      static final Node A = NodeFactory.createURI("https://data.example.org/id/organisatie/42");
      static final String CONSTRUCT = "CONSTRUCT { ?resource ?p ?o } WHERE { ?resource ?p ?o }";
      static final Graph DATA = RDFParser.fromString("""
              <https://data.example.org/id/organisatie/42> <https://example.org/ns/name> "Forty-two" .
              <https://data.example.org/id/organisatie/7> <https://example.org/ns/name> "Seven" .
              """, Lang.TURTLE).toGraph();

      private final SparqlDataSource adapter = new SparqlDataSource();

      @Test
      @DisplayName("NFR-SEC-04 the resource is bound as a term")
      void constructWithBinding() {
          try (var store = StubSparqlEndpoint.serving(DATA)) {
              Graph g = adapter.describe(query(store, CONSTRUCT, Duration.ofSeconds(10)), A);
              assertThat(g.size()).isEqualTo(1);
              assertThat(g.contains(A, Node.ANY, Node.ANY)).isTrue();
          }
      }

      @Test
      void describeQueries() {
          try (var store = StubSparqlEndpoint.serving(DATA)) {
              Graph g = adapter.describe(query(store, "DESCRIBE ?resource", Duration.ofSeconds(10)), A);
              assertThat(g.contains(A, Node.ANY, Node.ANY)).isTrue();
          }
      }

      @Test
      @DisplayName("AC-4.2 an error from the store is a failure, without its message")
      void storeErrorIsFailure() {
          try (var store = StubSparqlEndpoint.failing()) {
              assertThatThrownBy(() -> adapter.describe(query(store, CONSTRUCT, Duration.ofSeconds(10)), A))
                      .isInstanceOf(DataSourceFailure.class)
                      .hasMessage("Data source 'store' failed");
          }
      }

      @Test
      @DisplayName("AC-4.3 no answer within the time-out is a time-out")
      void slowStoreTimesOut() {
          try (var store = StubSparqlEndpoint.slow(Duration.ofSeconds(3))) {
              long start = System.nanoTime();
              assertThatThrownBy(() -> adapter.describe(query(store, CONSTRUCT, Duration.ofMillis(500)), A))
                      .isInstanceOf(DataSourceTimeout.class);
              assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
          }
      }

      @Test
      void unreachableStoreIsFailure() {
          var source = new DataSource("store", URI.create("http://127.0.0.1:9/sparql"), Duration.ofSeconds(2));
          assertThatThrownBy(() -> adapter.describe(new QuerySpec("q.rq", CONSTRUCT, source), A))
                  .isInstanceOf(DataSourceFailure.class);
      }

      private static QuerySpec query(StubSparqlEndpoint store, String text, Duration timeout) {
          return new QuerySpec("q.rq", text, new DataSource("store", URI.create(store.url()), timeout));
      }
  }
  ```
- [ ] Write `SparqlQueryInspectorTest`, with the inputs of ADR 0011 that apply to description queries:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.sparql;

  import static org.assertj.core.api.Assertions.assertThat;

  import be.vlaanderen.omgeving.spiegel.core.domain.QueryPolicy;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.ValueSource;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("AC-5.3, ADR 0011: inspecting description queries")
  class SparqlQueryInspectorTest {

      private final SparqlQueryInspector inspector = new SparqlQueryInspector();

      @ParameterizedTest
      @ValueSource(strings = {
              "CONSTRUCT { ?resource ?p ?o } WHERE { ?resource ?p ?o }",
              "DESCRIBE ?resource",
              "PREFIX ex: <https://example.org/> CONSTRUCT WHERE { ?resource ex:p ?o }"})
      void allowed(String query) {
          assertThat(inspector.violations(query, QueryPolicy.DESCRIPTION)).isEmpty();
      }

      @ParameterizedTest
      @ValueSource(strings = {
              "CONSTRUCT { ?s ?p ?o } WHERE { SERVICE <http://internal-host/sparql> { ?s ?p ?o } }",
              "CONSTRUCT { ?s ?p ?o } WHERE { { SELECT * { SERVICE <http://internal-host/sparql> { ?s ?p ?o } } } }",
              "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER EXISTS { SERVICE <http://internal-host/sparql> { ?s ?p ?o } } }"})
      void serviceIsRefused(String query) {
          assertThat(inspector.violations(query, QueryPolicy.DESCRIPTION)).containsExactly("SERVICE is not allowed");
      }

      @Test
      void otherFormsAreRefused() {
          assertThat(inspector.violations("SELECT * { ?s ?p ?o }", QueryPolicy.DESCRIPTION))
                  .containsExactly("the query form SELECT is not allowed here");
          assertThat(inspector.violations("ASK { ?s ?p ?o }", QueryPolicy.DESCRIPTION))
                  .containsExactly("the query form ASK is not allowed here");
      }

      @ParameterizedTest
      @ValueSource(strings = {"INSERT DATA { <a:a> <a:b> <a:c> }", "LOAD <http://example.org/>", "CONSTRUCT WHERE { ?s ?p"})
      void updatesAndSyntaxErrorsAreRefused(String query) {
          assertThat(inspector.violations(query, QueryPolicy.DESCRIPTION))
                  .singleElement().asString().startsWith("not a valid SPARQL 1.1 query");
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-adapter-sparql -am test` — expect: FAIL (compilation).
- [ ] Implement `SparqlDataSource`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.sparql;

  import be.vlaanderen.omgeving.spiegel.core.domain.DataSource;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceFailure;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourcePort;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceTimeout;
  import java.net.http.HttpTimeoutException;
  import java.util.concurrent.TimeUnit;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.Node;
  import org.apache.jena.query.Query;
  import org.apache.jena.query.QueryCancelledException;
  import org.apache.jena.query.QueryFactory;
  import org.apache.jena.sparql.exec.http.QueryExecHTTP;

  /**
   * {@link DataSourcePort} over the SPARQL 1.1 Protocol. The resource is bound with Jena's syntactic substitution,
   * never pasted into text (NFR-SEC-04). The store is asked for N-Triples, whatever the client asked for, and its
   * error bodies are never passed on (ADR 0016).
   */
  public final class SparqlDataSource implements DataSourcePort {

      static final String RESULT_FORMAT = "application/n-triples";

      @Override
      public Graph describe(QuerySpec query, Node resource) {
          DataSource source = query.dataSource();
          Query parsed = QueryFactory.create(query.text());
          try (QueryExecHTTP exec = QueryExecHTTP.service(source.endpoint().toString())
                  .query(parsed)
                  .substitution("resource", resource)
                  .acceptHeader(RESULT_FORMAT)
                  .timeout(source.timeout().toMillis(), TimeUnit.MILLISECONDS)
                  .build()) {
              return parsed.isDescribeType() ? exec.describe() : exec.construct();
          } catch (RuntimeException e) {
              throw isTimeout(e) ? new DataSourceTimeout(source.name(), e) : new DataSourceFailure(source.name(), e);
          }
      }

      private static boolean isTimeout(Throwable e) {
          for (Throwable cause = e; cause != null; cause = cause.getCause()) {
              if (cause instanceof HttpTimeoutException || cause instanceof QueryCancelledException) {
                  return true;
              }
          }
          return false;
      }
  }
  ```
- [ ] Implement `SparqlQueryInspector`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.sparql;

  import be.vlaanderen.omgeving.spiegel.core.domain.QueryForm;
  import be.vlaanderen.omgeving.spiegel.core.domain.QueryPolicy;
  import be.vlaanderen.omgeving.spiegel.core.port.out.QueryInspector;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.concurrent.atomic.AtomicBoolean;
  import org.apache.jena.query.Query;
  import org.apache.jena.query.QueryFactory;
  import org.apache.jena.query.QueryParseException;
  import org.apache.jena.query.Syntax;
  import org.apache.jena.sparql.algebra.Algebra;
  import org.apache.jena.sparql.algebra.OpVisitorBase;
  import org.apache.jena.sparql.algebra.op.OpService;
  import org.apache.jena.sparql.algebra.walker.Walker;

  /**
   * Checks query text against a {@link QueryPolicy} (ADR 0011): strict SPARQL 1.1 parsing, so an update fails to
   * parse, the query form, and a walk over the algebra, which finds {@code SERVICE} also in subqueries and
   * {@code EXISTS}. It never matches on text.
   */
  public final class SparqlQueryInspector implements QueryInspector {

      @Override
      public List<String> violations(String queryText, QueryPolicy policy) {
          Query query;
          try {
              query = QueryFactory.create(queryText, Syntax.syntaxSPARQL_11);
          } catch (QueryParseException e) {
              return List.of("not a valid SPARQL 1.1 query: " + e.getMessage());
          }
          List<String> violations = new ArrayList<>();
          QueryForm form = switch (query.queryType()) {
              case SELECT -> QueryForm.SELECT;
              case CONSTRUCT -> QueryForm.CONSTRUCT;
              case ASK -> QueryForm.ASK;
              case DESCRIBE -> QueryForm.DESCRIBE;
              default -> null;
          };
          if (form == null || !policy.allowedForms().contains(form)) {
              violations.add("the query form " + query.queryType() + " is not allowed here");
          }
          if (usesService(query)) {
              violations.add("SERVICE is not allowed");
          }
          return violations;
      }

      private static boolean usesService(Query query) {
          if (query.getQueryPattern() == null) {
              return false;
          }
          AtomicBoolean found = new AtomicBoolean();
          Walker.walk(Algebra.compile(query), new OpVisitorBase() {
              @Override
              public void visit(OpService op) {
                  found.set(true);
              }
          });
          return found.get();
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-adapter-sparql -am test` — expect: PASS.
- [ ] Commit: `feat: run description queries over SPARQL and inspect them`

---
## Phase 5: Configuration adapter

**Implements:** FR-1 (templates and base from configuration), FR-2 (default query, depth), Story 5 | **Satisfies:** AC-5.1, AC-5.2, AC-5.4, AC-5.5, AC-2.2 (default query)
**Files:** `spiegel-adapter-config/src/main/java/be/vlaanderen/omgeving/spiegel/adapter/config/{TurtleTenantConfiguration,DefaultDescriptionQuery,ConfigVocabulary}.java`, `spiegel-adapter-config/src/main/resources/spiegel-config-shapes.ttl`, tests `TurtleTenantConfigurationTest`, `DefaultDescriptionQueryTest`
**Interfaces:** Consumes: `TenantConfigurationPort`, `InvalidConfigurationException`, `Tenant`, `ResourceType`, `UriTemplate`, `QuerySpec`, `DataSource` (Phases 1 and 3). Produces: `TurtleTenantConfiguration(Path directory) implements TenantConfigurationPort`, reading `tenant.ttl` and the `.rq` files it names; `DefaultDescriptionQuery.forDepth(int)`; the vocabulary terms of the "Decisions made while planning".

Read the tenant's Turtle, validate it with SPieGeL's SHACL shapes, and turn it into the core's types (ADR 0012, decision 3). Without query files, a resource type uses the default query generated for the tenant's depth.

- [ ] Write `DefaultDescriptionQueryTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.config;

  import static org.assertj.core.api.Assertions.assertThat;

  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.Node;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.query.Query;
  import org.apache.jena.query.QueryFactory;
  import org.apache.jena.query.Syntax;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFParser;
  import org.apache.jena.sparql.exec.QueryExec;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-RA-04, FR-RA-06: SPieGeL's default description query")
  class DefaultDescriptionQueryTest {

      static final String EX = "https://example.org/ns/";

      @Test
      void isAConstructQueryInStrictSparql() {
          Query query = QueryFactory.create(DefaultDescriptionQuery.forDepth(3), Syntax.syntaxSPARQL_11);
          assertThat(query.isConstructType()).isTrue();
      }

      @Test
      @DisplayName("AC-2.2, AC-2.3, AC-2.4 it fetches what the description needs, and no more")
      void fetchesTheBoundedDescription() {
          Graph data = RDFParser.fromString("""
                  @prefix ex: <https://example.org/ns/> .
                  @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                  ex:a ex:p [ ex:l1 [ ex:l2 [ ex:l3 [ ex:l4 "deep" ] ; ex:ref ex:c ] ] ] ; ex:link ex:b .
                  ex:b rdfs:label "B" ; a ex:T ; ex:other "no" .
                  ex:c rdfs:label "C" ; ex:other "no" .
                  ex:z ex:points ex:a .
                  """, Lang.TURTLE).toGraph();

          Graph result = QueryExec.graph(data).query(DefaultDescriptionQuery.forDepth(3))
                  .substitution("resource", iri("a")).build().construct();

          assertThat(result.contains(Node.ANY, iri("l3"), Node.ANY)).isTrue();
          assertThat(result.contains(Node.ANY, iri("l4"), Node.ANY)).isFalse();
          assertThat(result.contains(iri("c"), Node.ANY, Node.ANY)).isTrue();
          assertThat(result.contains(Node.ANY, iri("other"), Node.ANY)).isFalse();
          assertThat(result.contains(iri("z"), Node.ANY, Node.ANY)).isFalse();
      }

      private static Node iri(String local) {
          return NodeFactory.createURI(EX + local);
      }
  }
  ```

  In this data `ex:p` points to level 1, `ex:l1` to level 2, `ex:l2` to level 3 and `ex:l3` from level 3 to level 4; `ex:l4` is level 4's own triple. Tried on Jena 6.2 during planning: the result holds `ex:l3` and `ex:c`'s label, and neither `ex:l4`, `ex:other` nor `ex:z`.
- [ ] Write `TurtleTenantConfigurationTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.config;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceType;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.port.out.InvalidConfigurationException;
  import java.io.IOException;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.time.Duration;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.io.TempDir;

  @DisplayName("Story 5: describe a domain with configuration only")
  class TurtleTenantConfigurationTest {

      static final String PREFIXES = """
              @prefix spiegel: <https://w3id.org/spiegel/config#> .
              @prefix api:     <http://purl.org/linked-data/api/vocab#> .
              @prefix sh:      <http://www.w3.org/ns/shacl#> .
              @prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .
              """;

      static final String MINIMAL = PREFIXES + """
              <#tenant> a spiegel:Tenant ;
                  spiegel:base <https://data.example.org> ;
                  spiegel:defaultDataSource <#main> ;
                  spiegel:resourceType <#generic> .
              <#main> a spiegel:DataSource ; spiegel:endpoint <http://store/sparql> .
              <#generic> a spiegel:ResourceType ;
                  api:itemTemplate "/id/{concept}/{id}" ;
                  api:uriTemplate  "/doc/{concept}/{id}" ;
                  spiegel:variable [ spiegel:name "id" ; sh:pattern "^[0-9]+$" ] .
              """;

      @TempDir
      Path dir;

      @Test
      @DisplayName("AC-5.1, AC-5.5 a generic template alone works, with depth 3, time-out 10 s and the default query")
      void minimalConfiguration() throws IOException {
          Tenant tenant = load(MINIMAL);

          assertThat(tenant.base()).hasToString("https://data.example.org");
          assertThat(tenant.blankNodeDepth()).isEqualTo(3);
          assertThat(tenant.defaultQuery().dataSource().timeout()).isEqualTo(Duration.ofSeconds(10));
          assertThat(tenant.defaultQuery().text()).isEqualTo(DefaultDescriptionQuery.forDepth(3));
          ResourceType generic = tenant.resourceTypes().getFirst();
          assertThat(generic.queries()).isEmpty();
          assertThat(generic.idTemplate().match("/id/organisatie/42")).isPresent();
          assertThat(generic.idTemplate().match("/id/organisatie/abc")).isEmpty();
      }

      @Test
      @DisplayName("AC-5.2 a specific template with its own query, on the default or a named data source")
      void specificTemplateWithQuery() throws IOException {
          Files.createDirectories(dir.resolve("queries"));
          Files.writeString(dir.resolve("queries/emissie.rq"), "CONSTRUCT WHERE { ?resource ?p ?o }");
          Tenant tenant = load(MINIMAL.replace("spiegel:resourceType <#generic> .",
                  "spiegel:resourceType <#generic>, <#emissie> ; spiegel:blankNodeDepth 2 .") + """
                  <#second> a spiegel:DataSource ; spiegel:endpoint <http://second/sparql> ;
                      spiegel:timeout "PT2S"^^xsd:duration .
                  <#emissie> a spiegel:ResourceType ;
                      api:itemTemplate "/id/emissie/{id}" ;
                      api:uriTemplate  "/doc/emissie/{id}" ;
                      spiegel:query [ spiegel:file "queries/emissie.rq" ] ,
                                    [ spiegel:file "queries/emissie.rq" ; spiegel:dataSource <#second> ] .
                  """);

          ResourceType emissie = tenant.match("/doc/emissie/5").orElseThrow().type();
          assertThat(tenant.blankNodeDepth()).isEqualTo(2);
          assertThat(emissie.queries()).hasSize(2)
                  .allSatisfy(q -> assertThat(q.name()).isEqualTo("queries/emissie.rq"));
          assertThat(emissie.queries()).extracting(q -> q.dataSource().name())
                  .containsExactlyInAnyOrder("main", "second");
          assertThat(emissie.queries()).extracting(q -> q.dataSource().timeout())
                  .contains(Duration.ofSeconds(2));
      }

      @Test
      @DisplayName("AC-5.4 a configuration that breaks the shapes is refused, naming each violation")
      void shapesAreChecked() throws IOException {
          String broken = MINIMAL
                  .replace("spiegel:base <https://data.example.org> ;", "")
                  .replace("spiegel:endpoint <http://store/sparql>", "spiegel:timeout \"PT1S\"^^xsd:duration")
                  .replace("\"/doc/{concept}/{id}\"", "\"/doc/x{concept}/{id}\"");

          assertThatThrownBy(() -> load(broken))
                  .isInstanceOf(InvalidConfigurationException.class)
                  .satisfies(e -> assertThat(((InvalidConfigurationException) e).violations())
                          .hasSize(3)
                          .anySatisfy(v -> assertThat(v).contains("spiegel:base"))
                          .anySatisfy(v -> assertThat(v).contains("spiegel:endpoint"))
                          .anySatisfy(v -> assertThat(v).contains("api:uriTemplate")));
      }

      @Test
      void templatesOfATypeMustHaveTheSameVariables() {
          String broken = MINIMAL.replace("\"/doc/{concept}/{id}\"", "\"/doc/{concept}/{nummer}\"");

          assertThatThrownBy(() -> load(broken))
                  .isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("the same variables");
      }

      @Test
      void aMissingQueryFileIsNamed() {
          String config = MINIMAL.replace("spiegel:variable", "spiegel:query [ spiegel:file \"queries/missing.rq\" ] ; spiegel:variable");

          assertThatThrownBy(() -> load(config))
                  .isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("queries/missing.rq");
      }

      @Test
      void aQueryFileOutsideTheDirectoryIsRefused() {
          String config = MINIMAL.replace("spiegel:variable", "spiegel:query [ spiegel:file \"../secret.rq\" ] ; spiegel:variable");

          assertThatThrownBy(() -> load(config))
                  .isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("outside the configuration directory");
      }

      @Test
      void turtleThatDoesNotParseIsRefused() {
          assertThatThrownBy(() -> load("this is not Turtle"))
                  .isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("tenant.ttl");
      }

      private Tenant load(String turtle) throws IOException {
          Files.writeString(dir.resolve("tenant.ttl"), turtle);
          return new TurtleTenantConfiguration(dir).load();
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-adapter-config -am test` — expect: FAIL (compilation).
- [ ] Write the shapes `spiegel-adapter-config/src/main/resources/spiegel-config-shapes.ttl`. Every `sh:message` names the property as a prefixed name, so that a violation can be found in the Turtle.

  ```turtle
  @prefix sh:      <http://www.w3.org/ns/shacl#> .
  @prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .
  @prefix spiegel: <https://w3id.org/spiegel/config#> .
  @prefix api:     <http://purl.org/linked-data/api/vocab#> .
  @prefix shapes:  <https://w3id.org/spiegel/config/shapes#> .

  shapes:Tenant a sh:NodeShape ;
      sh:targetClass spiegel:Tenant ;
      sh:property [
          sh:path spiegel:base ; sh:minCount 1 ; sh:maxCount 1 ; sh:nodeKind sh:IRI ;
          sh:pattern "^https?://[^/]+$" ;
          sh:message "A tenant has exactly one spiegel:base: an http or https IRI with a host and no path." ] ,
      [
          sh:path spiegel:blankNodeDepth ; sh:maxCount 1 ; sh:datatype xsd:integer ; sh:minInclusive 1 ;
          sh:message "spiegel:blankNodeDepth is at most one integer of at least 1." ] ,
      [
          sh:path spiegel:defaultDataSource ; sh:minCount 1 ; sh:maxCount 1 ; sh:class spiegel:DataSource ;
          sh:message "A tenant has exactly one spiegel:defaultDataSource, which is a spiegel:DataSource." ] ,
      [
          sh:path spiegel:resourceType ; sh:minCount 1 ; sh:class spiegel:ResourceType ;
          sh:message "A tenant has at least one spiegel:resourceType, each a spiegel:ResourceType." ] .

  shapes:DataSource a sh:NodeShape ;
      sh:targetClass spiegel:DataSource ;
      sh:property [
          sh:path spiegel:endpoint ; sh:minCount 1 ; sh:maxCount 1 ; sh:nodeKind sh:IRI ;
          sh:message "A data source has exactly one spiegel:endpoint IRI." ] ,
      [
          sh:path spiegel:timeout ; sh:maxCount 1 ; sh:datatype xsd:duration ;
          sh:message "spiegel:timeout is at most one xsd:duration, such as \"PT10S\"." ] .

  shapes:ResourceType a sh:NodeShape ;
      sh:targetClass spiegel:ResourceType ;
      sh:property [
          sh:path api:itemTemplate ; sh:minCount 1 ; sh:maxCount 1 ; sh:datatype xsd:string ;
          sh:pattern "^(/([^{}/]+|\\{[A-Za-z][A-Za-z0-9_]*\\}))+$" ;
          sh:message "A resource type has exactly one api:itemTemplate: a path whose segments are text or one whole {variable}." ] ,
      [
          sh:path api:uriTemplate ; sh:minCount 1 ; sh:maxCount 1 ; sh:datatype xsd:string ;
          sh:pattern "^(/([^{}/]+|\\{[A-Za-z][A-Za-z0-9_]*\\}))+$" ;
          sh:message "A resource type has exactly one api:uriTemplate: a path whose segments are text or one whole {variable}." ] ,
      [
          sh:path spiegel:variable ; sh:node shapes:Variable ;
          sh:message "Each spiegel:variable has one spiegel:name and at most one sh:pattern." ] ,
      [
          sh:path spiegel:query ; sh:node shapes:Query ;
          sh:message "Each spiegel:query has one spiegel:file and at most one spiegel:dataSource." ] .

  shapes:Variable a sh:NodeShape ;
      sh:property [ sh:path spiegel:name ; sh:minCount 1 ; sh:maxCount 1 ; sh:datatype xsd:string ] ,
                  [ sh:path sh:pattern ; sh:maxCount 1 ; sh:datatype xsd:string ] .

  shapes:Query a sh:NodeShape ;
      sh:property [ sh:path spiegel:file ; sh:minCount 1 ; sh:maxCount 1 ; sh:datatype xsd:string ] ,
                  [ sh:path spiegel:dataSource ; sh:maxCount 1 ; sh:class spiegel:DataSource ] .
  ```
- [ ] Implement `ConfigVocabulary` (the terms used when mapping):

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.config;

  import org.apache.jena.rdf.model.Property;
  import org.apache.jena.rdf.model.Resource;
  import org.apache.jena.rdf.model.ResourceFactory;

  /** SPieGeL's configuration vocabulary, {@code https://w3id.org/spiegel/config#} (ADR 0012). */
  final class ConfigVocabulary {

      static final String NS = "https://w3id.org/spiegel/config#";
      static final String API = "http://purl.org/linked-data/api/vocab#";

      static final Resource TENANT = ResourceFactory.createResource(NS + "Tenant");
      static final Property BASE = property(NS, "base");
      static final Property BLANK_NODE_DEPTH = property(NS, "blankNodeDepth");
      static final Property DEFAULT_DATA_SOURCE = property(NS, "defaultDataSource");
      static final Property RESOURCE_TYPE = property(NS, "resourceType");
      static final Property ENDPOINT = property(NS, "endpoint");
      static final Property TIMEOUT = property(NS, "timeout");
      static final Property VARIABLE = property(NS, "variable");
      static final Property NAME = property(NS, "name");
      static final Property QUERY = property(NS, "query");
      static final Property FILE = property(NS, "file");
      static final Property DATA_SOURCE = property(NS, "dataSource");
      static final Property ITEM_TEMPLATE = property(API, "itemTemplate");
      static final Property URI_TEMPLATE = property(API, "uriTemplate");
      static final Property PATTERN = property("http://www.w3.org/ns/shacl#", "pattern");

      private ConfigVocabulary() {
      }

      private static Property property(String ns, String local) {
          return ResourceFactory.createProperty(ns + local);
      }
  }
  ```
- [ ] Implement `DefaultDescriptionQuery` (decision 5):

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.config;

  import java.util.ArrayList;
  import java.util.List;

  /**
   * SPieGeL's default description query for a blank-node depth (FR-RA-04, FR-RA-06, ADR 0012, decision 7). Per
   * level, one branch returns the node's own triples, and one the label and type of each IRI it links to. The
   * resource is bound later, as {@code ?resource}.
   */
  final class DefaultDescriptionQuery {

      static final String NAME = "default description query";

      private static final String PREFIXES = """
              PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
              PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
              PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
              PREFIX dct:  <http://purl.org/dc/terms/>
              """;

      private DefaultDescriptionQuery() {
      }

      static String forDepth(int depth) {
          List<String> branches = new ArrayList<>();
          for (int level = 0; level <= depth; level++) {
              String node = level == 0 ? "?resource" : "?b" + level;
              branches.add("{ " + chain(level) + node + " ?p ?o . BIND(" + node + " AS ?s) }");
              branches.add("{ " + chain(level) + node + " ?l" + level + " ?s . FILTER(isIRI(?s)) "
                      + "VALUES ?p { rdf:type rdfs:label skos:prefLabel dct:title } ?s ?p ?o }");
          }
          return PREFIXES + "CONSTRUCT { ?s ?p ?o }\nWHERE {\n  " + String.join("\n  UNION\n  ", branches) + "\n}\n";
      }

      /** The patterns from the resource to the blank node at a level. */
      private static String chain(int level) {
          var chain = new StringBuilder();
          for (int i = 1; i <= level; i++) {
              String from = i == 1 ? "?resource" : "?b" + (i - 1);
              chain.append(from).append(" ?c").append(i).append(" ?b").append(i)
                      .append(" . FILTER(isBlank(?b").append(i).append(")) ");
          }
          return chain.toString();
      }
  }
  ```
- [ ] Implement `TurtleTenantConfiguration`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.config;

  import static be.vlaanderen.omgeving.spiegel.adapter.config.ConfigVocabulary.*;

  import be.vlaanderen.omgeving.spiegel.core.domain.DataSource;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceType;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.domain.UriTemplate;
  import be.vlaanderen.omgeving.spiegel.core.port.out.InvalidConfigurationException;
  import be.vlaanderen.omgeving.spiegel.core.port.out.TenantConfigurationPort;
  import java.io.IOException;
  import java.net.URI;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.time.Duration;
  import java.time.format.DateTimeParseException;
  import java.util.ArrayList;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;
  import java.util.Optional;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.rdf.model.Model;
  import org.apache.jena.rdf.model.ModelFactory;
  import org.apache.jena.rdf.model.RDFNode;
  import org.apache.jena.rdf.model.Resource;
  import org.apache.jena.rdf.model.Statement;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFParser;
  import org.apache.jena.riot.RiotException;
  import org.apache.jena.shacl.ShaclValidator;
  import org.apache.jena.shacl.Shapes;
  import org.apache.jena.shacl.ValidationReport;
  import org.apache.jena.vocabulary.RDF;

  /**
   * Reads a tenant from {@code tenant.ttl} in a directory, with the {@code .rq} files it names (ADR 0012,
   * ADR 0013). The Turtle is validated with SPieGeL's shapes first; every violation is reported, and nothing is
   * returned unless the whole configuration is valid.
   */
  public final class TurtleTenantConfiguration implements TenantConfigurationPort {

      static final int DEFAULT_DEPTH = 3;
      static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
      private static final Shapes SHAPES = Shapes.parse(RDFParser
              .source(TurtleTenantConfiguration.class.getResource("/spiegel-config-shapes.ttl").toString())
              .lang(Lang.TURTLE).toGraph());

      private final Path directory;

      public TurtleTenantConfiguration(Path directory) {
          this.directory = directory.toAbsolutePath().normalize();
      }

      @Override
      public Tenant load() {
          Path file = directory.resolve("tenant.ttl");
          Graph graph;
          try {
              graph = RDFParser.source(file).base(file.toUri().toString()).lang(Lang.TURTLE).toGraph();
          } catch (RiotException e) {
              throw new InvalidConfigurationException(List.of(file.getFileName() + ": " + e.getMessage()));
          }
          ValidationReport report = ShaclValidator.get().validate(SHAPES, graph);
          if (!report.conforms()) {
              throw new InvalidConfigurationException(report.getEntries().stream()
                      .map(entry -> entry.focusNode() + ": " + entry.message())
                      .toList());
          }
          return new Mapping(ModelFactory.createModelForGraph(graph)).tenant();
      }

      /** Maps validated Turtle to the core's types, collecting what SHACL cannot check. */
      private final class Mapping {

          private final Model model;
          private final List<String> violations = new ArrayList<>();
          private final Map<Resource, DataSource> dataSources = new LinkedHashMap<>();

          Mapping(Model model) {
              this.model = model;
          }

          Tenant tenant() {
              List<Resource> tenants = model.listSubjectsWithProperty(RDF.type, TENANT).toList();
              if (tenants.size() != 1) {
                  throw new InvalidConfigurationException(
                          List.of("tenant.ttl: expected exactly one spiegel:Tenant, found " + tenants.size()));
              }
              Resource tenant = tenants.getFirst();
              int depth = tenant.hasProperty(BLANK_NODE_DEPTH)
                      ? tenant.getProperty(BLANK_NODE_DEPTH).getInt() : DEFAULT_DEPTH;
              DataSource defaultSource = dataSource(tenant.getPropertyResourceValue(DEFAULT_DATA_SOURCE));
              List<ResourceType> types = tenant.listProperties(RESOURCE_TYPE).toList().stream()
                      .map(statement -> resourceType(statement.getResource(), defaultSource))
                      .toList();
              if (!violations.isEmpty()) {
                  throw new InvalidConfigurationException(violations);
              }
              var defaultQuery = new QuerySpec(DefaultDescriptionQuery.NAME, DefaultDescriptionQuery.forDepth(depth),
                      defaultSource);
              return new Tenant(URI.create(tenant.getPropertyResourceValue(BASE).getURI()), depth, defaultQuery, types);
          }

          private ResourceType resourceType(Resource type, DataSource defaultSource) {
              Map<String, String> patterns = new LinkedHashMap<>();
              for (Statement variable : type.listProperties(VARIABLE).toList()) {
                  Resource v = variable.getResource();
                  if (v.hasProperty(PATTERN)) {
                      patterns.put(v.getProperty(NAME).getString(), v.getProperty(PATTERN).getString());
                  }
              }
              UriTemplate id = UriTemplate.parse(type.getProperty(ITEM_TEMPLATE).getString(), patterns);
              UriTemplate doc = UriTemplate.parse(type.getProperty(URI_TEMPLATE).getString(), patterns);
              if (!id.variables().equals(doc.variables())) {
                  violations.add(label(type) + ": api:itemTemplate and api:uriTemplate must have the same variables");
              }
              List<QuerySpec> queries = new ArrayList<>();
              for (Statement statement : type.listProperties(QUERY).toList()) {
                  Resource query = statement.getResource();
                  String name = query.getProperty(FILE).getString();
                  DataSource source = query.hasProperty(DATA_SOURCE)
                          ? dataSource(query.getPropertyResourceValue(DATA_SOURCE)) : defaultSource;
                  readQuery(name).ifPresent(text -> queries.add(new QuerySpec(name, text, source)));
              }
              return new ResourceType(label(type), id, doc, queries);
          }

          private DataSource dataSource(Resource resource) {
              return dataSources.computeIfAbsent(resource, r -> {
                  Duration timeout = DEFAULT_TIMEOUT;
                  if (r.hasProperty(TIMEOUT)) {
                      String lexical = r.getProperty(TIMEOUT).getLiteral().getLexicalForm();
                      try {
                          timeout = Duration.parse(lexical);
                      } catch (DateTimeParseException e) {
                          violations.add(label(r) + ": spiegel:timeout " + lexical + " is not a duration in days, hours, minutes or seconds");
                      }
                  }
                  return new DataSource(label(r), URI.create(r.getPropertyResourceValue(ENDPOINT).getURI()), timeout);
              });
          }

          private Optional<String> readQuery(String name) {
              Path file = directory.resolve(name).normalize();
              if (!file.startsWith(directory)) {
                  violations.add(name + ": the query file is outside the configuration directory");
                  return Optional.empty();
              }
              try {
                  return Optional.of(Files.readString(file));
              } catch (IOException e) {
                  violations.add(name + ": the query file cannot be read");
                  return Optional.empty();
              }
          }

          /** The fragment of a resource's IRI, such as {@code main} for {@code <#main>}. */
          private static String label(RDFNode node) {
              if (node.isURIResource()) {
                  String fragment = URI.create(node.asResource().getURI()).getFragment();
                  return fragment != null ? fragment : node.asResource().getURI();
              }
              return node.toString();
          }
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-adapter-config -am test` — expect: PASS.
- [ ] Commit: `feat: read and validate the tenant configuration in Turtle`

---
## Phase 6: Web adapter

**Implements:** FR-1 (`303`, host, base), FR-3, FR-4, NFR security (CORS, logs), NFR reliability (one log entry) | **Satisfies:** AC-1.1 to AC-1.6, AC-2.6, AC-2.8, AC-3.1 to AC-3.16, AC-4.1 to AC-4.8
**Files:** `spiegel-adapter-web/src/main/java/be/vlaanderen/omgeving/spiegel/adapter/web/{RdfFormat,FormatSelection,FormatNegotiator,RdfSerialiser,ProblemType,ProblemException,ProblemResponses,RequestLogFilter,DereferenceController}.java`, tests `FormatNegotiatorTest`, `RdfSerialiserTest`, `WebAdapterTest`, `WebTestApplication`
**Interfaces:** Consumes: `Tenant`, `TemplateMatch`, `TemplateKind`, `ResourceDescription`, `DereferenceResource`, `DataSourceFailure`, `DataSourceTimeout` (Phases 1 to 3). Produces: Spring beans `DereferenceController` (needs beans `Tenant` and `DereferenceResource`), `ProblemResponses`, `RequestLogFilter`; `RequestLogFilter.REQUEST_ID` and `RequestLogFilter.FAILURE` request attributes.

The inbound HTTP side: resolve the tenant, strip a format suffix before matching (FR-3), answer `id` with `303`, negotiate the format before any query, serialise to bytes, and answer every error as problem details. The request filter assigns the request identifier, sets CORS headers on every response and writes the one log entry.

- [ ] Write `FormatNegotiatorTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import java.util.List;
  import java.util.Optional;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-3: choosing the format")
  class FormatNegotiatorTest {

      @Test
      @DisplayName("AC-3.2 a suffix decides, and is not negotiated")
      void suffix() {
          assertThat(FormatNegotiator.select(Optional.of(RdfFormat.N_TRIPLES), null, "text/turtle"))
                  .isEqualTo(new FormatSelection(List.of(RdfFormat.N_TRIPLES), false));
      }

      @Test
      @DisplayName("AC-3.3 _format decides over Accept")
      void formatParameter() {
          assertThat(FormatNegotiator.select(Optional.empty(), "nt", "text/turtle"))
                  .isEqualTo(new FormatSelection(List.of(RdfFormat.N_TRIPLES), false));
      }

      @Test
      @DisplayName("AC-3.4 no Accept or */* gives every format in SPieGeL's order")
      void noPreference() {
          List<RdfFormat> all = List.of(RdfFormat.TURTLE, RdfFormat.JSON_LD, RdfFormat.N_TRIPLES, RdfFormat.RDF_XML);
          assertThat(FormatNegotiator.select(Optional.empty(), null, null).candidates()).isEqualTo(all);
          assertThat(FormatNegotiator.select(Optional.empty(), null, "*/*").candidates()).isEqualTo(all);
      }

      @Test
      @DisplayName("AC-3.5 quality values are weighed")
      void quality() {
          assertThat(FormatNegotiator.select(Optional.empty(), null, "application/ld+json;q=0.5, text/turtle;q=0.9")
                  .candidates()).containsExactly(RdfFormat.TURTLE, RdfFormat.JSON_LD);
      }

      @Test
      @DisplayName("AC-3.6, AC-3.15 equal quality follows SPieGeL's order; the rest stays as fallback")
      void preference() {
          assertThat(FormatNegotiator.select(Optional.empty(), null, "application/rdf+xml, application/ld+json")
                  .candidates()).containsExactly(RdfFormat.JSON_LD, RdfFormat.RDF_XML);
          assertThat(FormatNegotiator.select(Optional.empty(), null, "application/rdf+xml, text/turtle;q=0.5")
                  .candidates()).containsExactly(RdfFormat.RDF_XML, RdfFormat.TURTLE);
      }

      @Test
      @DisplayName("the most specific range decides a format's quality")
      void mostSpecificRange() {
          assertThat(FormatNegotiator.select(Optional.empty(), null, "*/*;q=0.1, text/turtle;q=0")
                  .candidates()).doesNotContain(RdfFormat.TURTLE).contains(RdfFormat.JSON_LD);
      }

      @Test
      @DisplayName("AC-3.16 media type parameters are ignored")
      void parametersIgnored() {
          assertThat(FormatNegotiator.select(Optional.empty(), null,
                  "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").candidates())
                  .containsExactly(RdfFormat.JSON_LD);
      }

      @Test
      @DisplayName("AC-3.11, AC-3.12 nothing acceptable gives 406")
      void notAcceptable() {
          for (String accept : new String[] {"text/html", "application/json"}) {
              assertThatThrownBy(() -> FormatNegotiator.select(Optional.empty(), null, accept))
                      .isInstanceOfSatisfying(ProblemException.class,
                              e -> assertThat(e.type()).isEqualTo(ProblemType.NOT_ACCEPTABLE));
          }
          assertThatThrownBy(() -> FormatNegotiator.select(Optional.empty(), "xyz", null))
                  .isInstanceOfSatisfying(ProblemException.class,
                          e -> assertThat(e.type()).isEqualTo(ProblemType.NOT_ACCEPTABLE));
      }

      @Test
      @DisplayName("AC-3.13 a suffix and a _format that disagree give 400")
      void conflict() {
          assertThatThrownBy(() -> FormatNegotiator.select(Optional.of(RdfFormat.TURTLE), "nt", null))
                  .isInstanceOfSatisfying(ProblemException.class,
                          e -> assertThat(e.type()).isEqualTo(ProblemType.INVALID_REQUEST));
      }

      @Test
      void malformedAcceptIsAnInvalidRequest() {
          assertThatThrownBy(() -> FormatNegotiator.select(Optional.empty(), null, "text/turtle;q=abc"))
                  .isInstanceOfSatisfying(ProblemException.class,
                          e -> assertThat(e.type()).isEqualTo(ProblemType.INVALID_REQUEST));
      }
  }
  ```
- [ ] Write `RdfSerialiserTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import static org.assertj.core.api.Assertions.assertThat;

  import org.apache.jena.graph.Graph;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFParser;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("FR-3: writing a description")
  class RdfSerialiserTest {

      static final Graph WRITABLE = RDFParser.fromString(
              "<https://data.example.org/id/x/1> <http://xmlns.com/foaf/0.1/name> \"x\" .", Lang.TURTLE).toGraph();
      static final Graph NOT_AS_XML = RDFParser.fromString(
              "<https://data.example.org/id/x/1> <https://example.org/ns/eigenschap/123> \"x\" .", Lang.TURTLE).toGraph();

      @Test
      @DisplayName("AC-3.1 every format round-trips")
      void everyFormat() {
          for (RdfFormat format : RdfFormat.values()) {
              byte[] body = RdfSerialiser.write(WRITABLE, format).orElseThrow();
              Graph back = RDFParser.fromString(new String(body, java.nio.charset.StandardCharsets.UTF_8), format.lang()).toGraph();
              assertThat(back.isIsomorphicWith(WRITABLE)).as(format.name()).isTrue();
          }
      }

      @Test
      @DisplayName("AC-3.14 a predicate without an XML local name cannot be written as RDF/XML")
      void rdfXmlRefused() {
          assertThat(RdfSerialiser.write(NOT_AS_XML, RdfFormat.RDF_XML)).isEmpty();
          assertThat(RdfSerialiser.write(NOT_AS_XML, RdfFormat.TURTLE)).isPresent();
      }
  }
  ```
- [ ] Write `WebTestApplication` and `WebAdapterTest`, a slice test with a tenant and a data source port that the test controls:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import be.vlaanderen.omgeving.spiegel.core.domain.DataSource;
  import be.vlaanderen.omgeving.spiegel.core.domain.QuerySpec;
  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceType;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.domain.UriTemplate;
  import be.vlaanderen.omgeving.spiegel.core.port.in.DereferenceResource;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourcePort;
  import java.net.URI;
  import java.time.Duration;
  import java.util.List;
  import java.util.Map;
  import java.util.concurrent.atomic.AtomicReference;
  import org.springframework.boot.SpringBootConfiguration;
  import org.springframework.context.annotation.Bean;

  /** The configuration that the web adapter's slice tests start from. */
  @SpringBootConfiguration
  class WebTestApplication {

      /** What the data source answers; each test sets it. */
      static final AtomicReference<DataSourcePort> STORE = new AtomicReference<>();

      @Bean
      Tenant tenant() {
          var source = new DataSource("main", URI.create("http://store/sparql"), Duration.ofSeconds(10));
          var query = new QuerySpec("default", "CONSTRUCT WHERE { ?resource ?p ?o }", source);
          var generic = new ResourceType("generic", UriTemplate.parse("/id/{concept}/{id}", Map.of()),
                  UriTemplate.parse("/doc/{concept}/{id}", Map.of()), List.of());
          return new Tenant(URI.create("https://data.example.org"), 3, query, List.of(generic));
      }

      @Bean
      DereferenceResource dereferenceResource() {
          return new DereferenceResource((query, resource) -> STORE.get().describe(query, resource));
      }
  }
  ```

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import com.jayway.jsonpath.JsonPath;
  import java.nio.charset.StandardCharsets;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.system.CapturedOutput;
  import org.springframework.boot.test.system.OutputCaptureExtension;
  import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
  import org.springframework.test.web.servlet.MockMvc;

  @WebMvcTest
  @ExtendWith(OutputCaptureExtension.class)
  @DisplayName("FR-4: errors and request handling in the web adapter")
  class WebAdapterTest {

      static final String SITE = "http://data.example.org";

      @Autowired
      MockMvc mvc;

      @Test
      @DisplayName("AC-4.6 another method gives 405 with Allow")
      void methodNotAllowed() throws Exception {
          mvc.perform(post(SITE + "/doc/organisatie/42"))
                  .andExpect(status().isMethodNotAllowed())
                  .andExpect(header().string("Allow", "GET, HEAD"))
                  .andExpect(jsonPath("$.type").value("https://w3id.org/spiegel/problem/method-not-allowed"));
      }

      @Test
      @DisplayName("AC-4.8 an unexpected failure gives 500 with a generic title, no stack trace and no-store")
      void internalError() throws Exception {
          WebTestApplication.STORE.set((query, resource) -> {
              throw new IllegalStateException("bug in line 12 of SomeClass");
          });

          String body = mvc.perform(get(SITE + "/doc/organisatie/42"))
                  .andExpect(status().isInternalServerError())
                  .andExpect(header().string("Cache-Control", "no-store"))
                  .andExpect(jsonPath("$.type").value("https://w3id.org/spiegel/problem/internal-error"))
                  .andExpect(jsonPath("$.title").value("Internal Error"))
                  .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

          assertThat(body).doesNotContain("SomeClass").doesNotContain("IllegalStateException");
      }

      @Test
      @DisplayName("AC-4.7 exactly one log entry carries the request identifier, with status and duration")
      void oneLogEntryPerRequest(CapturedOutput output) throws Exception {
          String body = mvc.perform(get("http://other.example.org/doc/organisatie/42"))
                  .andExpect(status().isNotFound())
                  .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
          String id = JsonPath.<String>read(body, "$.instance").substring("urn:uuid:".length());

          assertThat(output.getAll().lines().filter(line -> line.contains(id)))
                  .singleElement().asString().contains("404").contains(" ms");
      }

      @Test
      @DisplayName("NFR security: a CORS preflight is answered for GET and HEAD")
      void preflight() throws Exception {
          mvc.perform(options(SITE + "/doc/organisatie/42")
                          .header("Origin", "https://app.example.org")
                          .header("Access-Control-Request-Method", "GET"))
                  .andExpect(status().isNoContent())
                  .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                  .andExpect(header().string("Access-Control-Allow-Methods", "GET, HEAD"));
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-adapter-web -am test` — expect: FAIL (compilation).
- [ ] Implement `RdfFormat`, `FormatSelection` and `FormatNegotiator`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import java.util.Arrays;
  import java.util.Optional;
  import org.apache.jena.riot.Lang;
  import org.springframework.http.MediaType;

  /** The RDF formats of a description, in SPieGeL's order of preference (FR-CN-01, FR-CN-08). */
  enum RdfFormat {

      TURTLE("ttl", "text/turtle", Lang.TURTLE),
      JSON_LD("jsonld", "application/ld+json", Lang.JSONLD),
      N_TRIPLES("nt", "application/n-triples", Lang.NTRIPLES),
      RDF_XML("rdf", "application/rdf+xml", Lang.RDFXML);

      private final String suffix;
      private final MediaType mediaType;
      private final Lang lang;

      RdfFormat(String suffix, String mediaType, Lang lang) {
          this.suffix = suffix;
          this.mediaType = MediaType.parseMediaType(mediaType);
          this.lang = lang;
      }

      static Optional<RdfFormat> bySuffix(String suffix) {
          return Arrays.stream(values()).filter(format -> format.suffix.equals(suffix)).findFirst();
      }

      String suffix() {
          return suffix;
      }

      MediaType mediaType() {
          return mediaType;
      }

      Lang lang() {
          return lang;
      }
  }
  ```

  ```java
  /**
   * The formats a response may take, best first, and whether they were negotiated from {@code Accept} (which
   * decides {@code Vary} and {@code Content-Location}, FR-CN-06, FR-CN-07).
   */
  record FormatSelection(List<RdfFormat> candidates, boolean negotiated) {
  }
  ```

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import java.util.Arrays;
  import java.util.Comparator;
  import java.util.List;
  import java.util.Optional;
  import org.springframework.http.InvalidMediaTypeException;
  import org.springframework.http.MediaType;

  /**
   * Chooses the format of a description: a format suffix, then {@code _format}, then {@code Accept} weighed against
   * SPieGeL's order of preference (FR-CN-02, FR-CN-08). Media type parameters are ignored.
   */
  final class FormatNegotiator {

      private FormatNegotiator() {
      }

      static FormatSelection select(Optional<RdfFormat> suffix, String formatParameter, String accept) {
          if (formatParameter != null) {
              RdfFormat format = RdfFormat.bySuffix(formatParameter).orElseThrow(() -> new ProblemException(
                      ProblemType.NOT_ACCEPTABLE, "The value of _format is not a format that SPieGeL offers."));
              if (suffix.isPresent() && suffix.get() != format) {
                  throw new ProblemException(ProblemType.INVALID_REQUEST, "The format suffix and _format disagree.");
              }
              return new FormatSelection(List.of(format), false);
          }
          if (suffix.isPresent()) {
              return new FormatSelection(List.of(suffix.get()), false);
          }
          if (accept == null || accept.isBlank()) {
              return new FormatSelection(List.of(RdfFormat.values()), true);
          }
          List<MediaType> ranges;
          try {
              ranges = MediaType.parseMediaTypes(accept);
          } catch (InvalidMediaTypeException e) {
              throw new ProblemException(ProblemType.INVALID_REQUEST, "The Accept header cannot be read.");
          }
          List<RdfFormat> acceptable = Arrays.stream(RdfFormat.values())
                  .filter(format -> quality(format, ranges) > 0)
                  .sorted(Comparator.comparingDouble((RdfFormat format) -> quality(format, ranges)).reversed())
                  .toList();
          if (acceptable.isEmpty()) {
              throw new ProblemException(ProblemType.NOT_ACCEPTABLE,
                      "SPieGeL offers text/turtle, application/ld+json, application/n-triples and application/rdf+xml.");
          }
          return new FormatSelection(acceptable, true);
      }

      /** The quality of the most specific range that includes the format (RFC 9110, section 12.5.1). */
      private static double quality(RdfFormat format, List<MediaType> ranges) {
          double quality = 0;
          int specificity = -1;
          for (MediaType range : ranges) {
              if (range.includes(format.mediaType())) {
                  int s = range.isWildcardType() ? 0 : range.isWildcardSubtype() ? 1 : 2;
                  if (s > specificity) {
                      specificity = s;
                      quality = range.getQualityValue();
                  }
              }
          }
          return quality;
      }
  }
  ```
- [ ] Implement `RdfSerialiser`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import java.io.ByteArrayOutputStream;
  import java.util.Optional;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.Node;
  import org.apache.jena.riot.RDFWriter;
  import org.apache.jena.util.SplitIRI;

  /**
   * Writes a description to bytes, before any status is sent, so that a response never starts with {@code 200}
   * and then fails (FR-3).
   */
  final class RdfSerialiser {

      private RdfSerialiser() {
      }

      /** @return the body, or empty if the description cannot be written in this format */
      static Optional<byte[]> write(Graph graph, RdfFormat format) {
          if (format == RdfFormat.RDF_XML && !expressibleAsRdfXml(graph)) {
              return Optional.empty();
          }
          var body = new ByteArrayOutputStream();
          RDFWriter.source(graph).lang(format.lang()).output(body);
          return Optional.of(body.toByteArray());
      }

      /** RDF/XML needs every predicate split into a namespace and an XML local name. */
      private static boolean expressibleAsRdfXml(Graph graph) {
          return graph.find().toList().stream()
                  .map(triple -> triple.getPredicate())
                  .distinct()
                  .map(Node::getURI)
                  .allMatch(uri -> {
                      int split = SplitIRI.splitXML(uri);
                      return split > 0 && split < uri.length();
                  });
      }
  }
  ```
- [ ] Implement `ProblemType`, `ProblemException` and `ProblemResponses`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  /** The problem types of this feature (ADR 0016), under {@code https://w3id.org/spiegel/problem/}. */
  enum ProblemType {

      INVALID_REQUEST(400, "invalid-request", "Invalid Request"),
      NOT_FOUND(404, "not-found", "Not Found"),
      METHOD_NOT_ALLOWED(405, "method-not-allowed", "Method Not Allowed"),
      NOT_ACCEPTABLE(406, "not-acceptable", "Not Acceptable"),
      INTERNAL_ERROR(500, "internal-error", "Internal Error"),
      SOURCE_ERROR(502, "source-error", "Data Source Error"),
      SOURCE_TIMEOUT(504, "source-timeout", "Data Source Timeout");

      static final String BASE = "https://w3id.org/spiegel/problem/";

      private final int status;
      private final String slug;
      private final String title;

      ProblemType(int status, String slug, String title) {
          this.status = status;
          this.slug = slug;
          this.title = title;
      }

      int status() {
          return status;
      }

      String type() {
          return BASE + slug;
      }

      String title() {
          return title;
      }
  }
  ```

  ```java
  /** An error answered with problem details. The detail is written by SPieGeL, never copied from elsewhere. */
  final class ProblemException extends RuntimeException {

      private final ProblemType type;
      private final String detail;

      ProblemException(ProblemType type) {
          this(type, null);
      }

      ProblemException(ProblemType type, String detail) {
          super(type.type());
          this.type = type;
          this.detail = detail;
      }

      ProblemType type() {
          return type;
      }

      String detail() {
          return detail;
      }
  }
  ```

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceFailure;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourceTimeout;
  import jakarta.servlet.http.HttpServletRequest;
  import java.util.LinkedHashMap;
  import java.util.Map;
  import org.springframework.http.CacheControl;
  import org.springframework.http.HttpHeaders;
  import org.springframework.http.MediaType;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.HttpRequestMethodNotSupportedException;
  import org.springframework.web.bind.annotation.ExceptionHandler;
  import org.springframework.web.bind.annotation.RestControllerAdvice;
  import tools.jackson.databind.json.JsonMapper;

  /**
   * Answers every error as {@code application/problem+json}, whatever format was asked for (FR-CN-10, ADR 0016).
   * The cause of a failure goes to the log through the request, never into the body.
   */
  @RestControllerAdvice
  public class ProblemResponses {

      @ExceptionHandler(ProblemException.class)
      ResponseEntity<byte[]> problem(ProblemException e, HttpServletRequest request) {
          return respond(e.type(), e.detail(), request, null);
      }

      @ExceptionHandler(DataSourceFailure.class)
      ResponseEntity<byte[]> sourceError(DataSourceFailure e, HttpServletRequest request) {
          return respond(ProblemType.SOURCE_ERROR, null, request, e);
      }

      @ExceptionHandler(DataSourceTimeout.class)
      ResponseEntity<byte[]> sourceTimeout(DataSourceTimeout e, HttpServletRequest request) {
          return respond(ProblemType.SOURCE_TIMEOUT, null, request, e);
      }

      @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
      ResponseEntity<byte[]> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
          ResponseEntity<byte[]> response = respond(ProblemType.METHOD_NOT_ALLOWED, null, request, null);
          HttpHeaders headers = new HttpHeaders();
          headers.putAll(response.getHeaders());
          headers.set(HttpHeaders.ALLOW, "GET, HEAD");
          return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
      }

      @ExceptionHandler(Exception.class)
      ResponseEntity<byte[]> internalError(Exception e, HttpServletRequest request) {
          return respond(ProblemType.INTERNAL_ERROR, null, request, e);
      }

      static ResponseEntity<byte[]> respond(ProblemType type, String detail, HttpServletRequest request, Throwable cause) {
          if (cause != null) {
              request.setAttribute(RequestLogFilter.FAILURE, cause);
          }
          Map<String, Object> body = new LinkedHashMap<>();
          body.put("type", type.type());
          body.put("title", type.title());
          body.put("status", type.status());
          if (detail != null) {
              body.put("detail", detail);
          }
          body.put("instance", "urn:uuid:" + request.getAttribute(RequestLogFilter.REQUEST_ID));
          ResponseEntity.BodyBuilder response = ResponseEntity.status(type.status())
                  .contentType(MediaType.APPLICATION_PROBLEM_JSON);
          if (type.status() >= 500) {
              response.cacheControl(CacheControl.noStore());
          }
          return response.body(JsonMapper.shared().writeValueAsBytes(body));
      }
  }
  ```
- [ ] Implement `RequestLogFilter`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import jakarta.servlet.FilterChain;
  import jakarta.servlet.ServletException;
  import jakarta.servlet.http.HttpServletRequest;
  import jakarta.servlet.http.HttpServletResponse;
  import java.io.IOException;
  import java.util.UUID;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.slf4j.event.Level;
  import org.springframework.core.Ordered;
  import org.springframework.core.annotation.Order;
  import org.springframework.stereotype.Component;
  import org.springframework.web.filter.OncePerRequestFilter;

  /**
   * Gives every request an identifier, sets the CORS headers of a public response, and writes one structured log
   * entry per request with its status and duration (request flow, steps 1 and 16; NFR-OPS-05, NFR-OPEN-01). The
   * entry holds no address and no query value other than {@code _format} (NFR-PRIV-02).
   */
  @Component
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public class RequestLogFilter extends OncePerRequestFilter {

      public static final String REQUEST_ID = RequestLogFilter.class.getName() + ".requestId";
      public static final String FAILURE = RequestLogFilter.class.getName() + ".failure";

      private static final Logger LOG = LoggerFactory.getLogger(RequestLogFilter.class);

      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
          String id = UUID.randomUUID().toString();
          long start = System.nanoTime();
          request.setAttribute(REQUEST_ID, id);
          response.setHeader("Access-Control-Allow-Origin", "*");
          response.setHeader("Access-Control-Expose-Headers", "Link, Location, Content-Location");
          try {
              if ("OPTIONS".equals(request.getMethod()) && request.getHeader("Access-Control-Request-Method") != null) {
                  response.setHeader("Access-Control-Allow-Methods", "GET, HEAD");
                  response.setHeader("Access-Control-Allow-Headers", "Accept");
                  response.setHeader("Access-Control-Max-Age", "86400");
                  response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                  return;
              }
              chain.doFilter(request, response);
          } finally {
              long millis = (System.nanoTime() - start) / 1_000_000;
              int status = response.getStatus();
              LOG.atLevel(status >= 500 ? Level.ERROR : Level.INFO)
                      .setCause((Throwable) request.getAttribute(FAILURE))
                      .addKeyValue("requestId", id)
                      .addKeyValue("method", request.getMethod())
                      .addKeyValue("path", request.getRequestURI())
                      .addKeyValue("format", request.getParameter("_format"))
                      .addKeyValue("status", status)
                      .addKeyValue("durationMs", millis)
                      .log("{} {} {} {} ms request {}", request.getMethod(), request.getRequestURI(), status, millis, id);
          }
      }
  }
  ```
- [ ] Implement `DereferenceController`:

  ```java
  package be.vlaanderen.omgeving.spiegel.adapter.web;

  import be.vlaanderen.omgeving.spiegel.core.domain.ResourceDescription;
  import be.vlaanderen.omgeving.spiegel.core.domain.TemplateKind;
  import be.vlaanderen.omgeving.spiegel.core.domain.TemplateMatch;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.port.in.DereferenceResource;
  import jakarta.servlet.http.HttpServletRequest;
  import java.net.URI;
  import java.util.Arrays;
  import java.util.Optional;
  import java.util.stream.Collectors;
  import org.springframework.http.HttpHeaders;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.RestController;

  /**
   * Dereferences {@code id} and {@code doc} URIs (request flow, steps 2 to 15). {@code HEAD} is answered by the
   * same method, without a body (FR-CN-09). Every URI it sends is formed from the tenant's base (FR-1).
   */
  @RestController
  public class DereferenceController {

      private final Tenant tenant;
      private final DereferenceResource dereference;

      public DereferenceController(Tenant tenant, DereferenceResource dereference) {
          this.tenant = tenant;
          this.dereference = dereference;
      }

      @GetMapping("/**")
      ResponseEntity<byte[]> dereference(HttpServletRequest request) {
          if (!tenant.serves(request.getServerName())) {
              throw new ProblemException(ProblemType.NOT_FOUND);
          }
          SuffixedPath path = SuffixedPath.of(request.getRequestURI().substring(request.getContextPath().length()));
          TemplateMatch match = tenant.match(path.path())
                  .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
          if (match.kind() == TemplateKind.ID) {
              return redirect(match, path, request.getQueryString());
          }
          FormatSelection selection = FormatNegotiator.select(
                  path.format(), request.getParameter("_format"), request.getHeader(HttpHeaders.ACCEPT));
          ResourceDescription description = dereference.describe(tenant, match)
                  .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
          for (RdfFormat format : selection.candidates()) {
              Optional<byte[]> body = RdfSerialiser.write(description.graph(), format);
              if (body.isPresent()) {
                  return describe(match, selection, format, body.get());
              }
          }
          throw new ProblemException(ProblemType.NOT_ACCEPTABLE, "The description cannot be expressed in RDF/XML.");
      }

      /** {@code 303 See Other} from the thing to its document, carrying over suffix and query (FR-URI-02). */
      private static ResponseEntity<byte[]> redirect(TemplateMatch match, SuffixedPath path, String query) {
          String location = match.documentUri()
                  + path.format().map(format -> "." + format.suffix()).orElse("")
                  + (query == null ? "" : "?" + query);
          return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(location)).build();
      }

      private static ResponseEntity<byte[]> describe(TemplateMatch match, FormatSelection selection, RdfFormat format,
              byte[] body) {
          String document = match.documentUri().toString();
          ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                  .contentType(format.mediaType())
                  .header(HttpHeaders.LINK, Arrays.stream(RdfFormat.values())
                          .map(f -> "<" + document + "." + f.suffix() + ">; rel=\"alternate\"; type=\"" + f.mediaType() + "\"")
                          .collect(Collectors.joining(", ")));
          if (selection.negotiated()) {
              response.header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
                      .header(HttpHeaders.CONTENT_LOCATION, document + "." + format.suffix());
          }
          return response.body(body);
      }

      /** A raw path with its format suffix, if any, recognised before template matching (FR-3). */
      record SuffixedPath(String path, Optional<RdfFormat> format) {

          static SuffixedPath of(String rawPath) {
              int slash = rawPath.lastIndexOf('/');
              int dot = rawPath.lastIndexOf('.');
              if (dot > slash + 1) {
                  Optional<RdfFormat> format = RdfFormat.bySuffix(rawPath.substring(dot + 1));
                  if (format.isPresent()) {
                      return new SuffixedPath(rawPath.substring(0, dot), format);
                  }
              }
              return new SuffixedPath(rawPath, Optional.empty());
          }
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-adapter-web -am test` — expect: PASS.
- [ ] Commit: `feat: dereference id and doc URIs over HTTP with content negotiation and problem details`

---
## Phase 7: Wiring the application

**Implements:** all FRs end to end, NFR reliability (refuse to start) | **Satisfies:** AC-1.5, AC-4.7 (structured), AC-5.3, AC-5.4 (start-up), and makes `DereferenceContractTest` pass
**Files:** `spiegel-app/src/main/java/be/vlaanderen/omgeving/spiegel/SpiegelConfiguration.java`, `spiegel-app/src/main/resources/application.yml`, `spiegel-app/src/test/resources/tenants/minimal/tenant.ttl`, `spiegel-app/src/test/java/be/vlaanderen/omgeving/spiegel/{SpiegelApplicationTest,StartupTest,ForwardedRequestTest}.java`
**Interfaces:** Consumes: `TurtleTenantConfiguration(Path)`, `LoadTenantConfiguration`, `SparqlDataSource`, `SparqlQueryInspector`, `DereferenceResource`, the web adapter's beans, `StubSparqlEndpoint`, `ContractTenant`. Produces: the running application, configured by the property `spiegel.configuration.location` (a directory with `tenant.ttl`).

Only `spiegel-app` knows every module (module structure). It loads and checks the configuration once at start-up; an invalid one stops the start (ADR 0013, decision 4).

- [ ] Write `StartupTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel;

  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import be.vlaanderen.omgeving.spiegel.core.port.out.InvalidConfigurationException;
  import java.io.IOException;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.io.TempDir;
  import org.springframework.boot.builder.SpringApplicationBuilder;

  @DisplayName("AC-5.3, AC-5.4: an invalid configuration stops the start")
  class StartupTest {

      @TempDir
      Path dir;

      @Test
      void aQueryWithServiceIsRefused() throws IOException {
          ContractTenant.write(dir, "http://127.0.0.1:9/sparql", "http://127.0.0.1:9/sparql",
                  "http://127.0.0.1:9/sparql", "http://127.0.0.1:9/sparql");
          Files.writeString(dir.resolve("queries/emissie.rq"),
                  "CONSTRUCT { ?s ?p ?o } WHERE { SERVICE <http://internal-host/sparql> { ?s ?p ?o } }");

          assertThatThrownBy(this::start)
                  .rootCause().isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("queries/emissie.rq: SERVICE is not allowed");
      }

      @Test
      void aSelectQueryIsRefused() throws IOException {
          ContractTenant.write(dir, "http://127.0.0.1:9/sparql", "http://127.0.0.1:9/sparql",
                  "http://127.0.0.1:9/sparql", "http://127.0.0.1:9/sparql");
          Files.writeString(dir.resolve("queries/emissie.rq"), "SELECT * { ?s ?p ?o }");

          assertThatThrownBy(this::start)
                  .rootCause().isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("queries/emissie.rq: the query form SELECT is not allowed here");
      }

      @Test
      void aShapeViolationIsRefused() throws IOException {
          ContractTenant.write(dir, "http://127.0.0.1:9/sparql", "http://127.0.0.1:9/sparql",
                  "http://127.0.0.1:9/sparql", "http://127.0.0.1:9/sparql");
          Path tenant = dir.resolve("tenant.ttl");
          Files.writeString(tenant, Files.readString(tenant).replace("spiegel:base <https://data.example.org> ;", ""));

          assertThatThrownBy(this::start)
                  .rootCause().isInstanceOf(InvalidConfigurationException.class)
                  .hasMessageContaining("spiegel:base");
      }

      private void start() {
          new SpringApplicationBuilder(SpiegelApplication.class)
                  .properties("spiegel.configuration.location=" + dir, "server.port=0")
                  .run()
                  .close();
      }
  }
  ```
- [ ] Write `ForwardedRequestTest` (AC-1.5), over a real port, as the reverse proxy would send it:

  ```java
  package be.vlaanderen.omgeving.spiegel;

  import static org.assertj.core.api.Assertions.assertThat;

  import be.vlaanderen.omgeving.spiegel.adapter.sparql.testing.StubSparqlEndpoint;
  import java.io.IOException;
  import java.net.URI;
  import java.net.http.HttpClient;
  import java.net.http.HttpRequest;
  import java.net.http.HttpResponse;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import org.apache.jena.riot.Lang;
  import org.apache.jena.riot.RDFParser;
  import org.junit.jupiter.api.AfterAll;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.boot.test.web.server.LocalServerPort;
  import org.springframework.test.context.DynamicPropertyRegistry;
  import org.springframework.test.context.DynamicPropertySource;

  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  @DisplayName("AC-1.5: IRIs come from the configured base, not from the request")
  class ForwardedRequestTest {

      static final StubSparqlEndpoint STORE = StubSparqlEndpoint.serving(RDFParser.fromString(
              "<https://data.example.org/id/organisatie/42> <https://example.org/ns/name> \"x\" .", Lang.TURTLE).toGraph());

      @DynamicPropertySource
      static void tenant(DynamicPropertyRegistry registry) throws IOException {
          Path dir = ContractTenant.write(Files.createTempDirectory("tenant"), STORE.url(), STORE.url(), STORE.url(), STORE.url());
          registry.add("spiegel.configuration.location", dir::toString);
      }

      @AfterAll
      static void stop() {
          STORE.close();
      }

      @LocalServerPort
      int port;

      private final HttpClient client = HttpClient.newHttpClient();

      @Test
      void redirectAndSubjectUseTheBase() throws Exception {
          HttpResponse<String> redirect = send("/id/organisatie/42", true);
          assertThat(redirect.statusCode()).isEqualTo(303);
          assertThat(redirect.headers().firstValue("Location")).contains("https://data.example.org/doc/organisatie/42");

          HttpResponse<String> description = send("/doc/organisatie/42", true);
          assertThat(description.statusCode()).isEqualTo(200);
          assertThat(RDFParser.fromString(description.body(), Lang.TURTLE).toGraph()
                  .contains(org.apache.jena.graph.NodeFactory.createURI("https://data.example.org/id/organisatie/42"),
                          org.apache.jena.graph.Node.ANY, org.apache.jena.graph.Node.ANY)).isTrue();
      }

      @Test
      void theInternalHostIsNotATenant() throws Exception {
          assertThat(send("/id/organisatie/42", false).statusCode()).isEqualTo(404);
      }

      private HttpResponse<String> send(String path, boolean throughProxy) throws Exception {
          HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                  .header("Accept", "text/turtle");
          if (throughProxy) {
              request.header("X-Forwarded-Host", "data.example.org").header("X-Forwarded-Proto", "https");
          }
          return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
      }
  }
  ```
- [ ] Give `SpiegelApplicationTest` a configuration: annotate it `@SpringBootTest(properties = "spiegel.configuration.location=src/test/resources/tenants/minimal")` and add `spiegel-app/src/test/resources/tenants/minimal/tenant.ttl`:

  ```turtle
  @prefix spiegel: <https://w3id.org/spiegel/config#> .
  @prefix api:     <http://purl.org/linked-data/api/vocab#> .

  <#tenant> a spiegel:Tenant ;
      spiegel:base <https://data.example.org> ;
      spiegel:defaultDataSource <#main> ;
      spiegel:resourceType <#generic> .
  <#main> a spiegel:DataSource ; spiegel:endpoint <http://127.0.0.1:9/sparql> .
  <#generic> a spiegel:ResourceType ;
      api:itemTemplate "/id/{concept}/{id}" ;
      api:uriTemplate  "/doc/{concept}/{id}" .
  ```
- [ ] Run: `mvn -q -pl spiegel-app -am test` — expect: FAIL (no `Tenant` bean; the contract tests fail).
- [ ] Implement `SpiegelConfiguration`:

  ```java
  package be.vlaanderen.omgeving.spiegel;

  import be.vlaanderen.omgeving.spiegel.adapter.config.TurtleTenantConfiguration;
  import be.vlaanderen.omgeving.spiegel.adapter.sparql.SparqlDataSource;
  import be.vlaanderen.omgeving.spiegel.adapter.sparql.SparqlQueryInspector;
  import be.vlaanderen.omgeving.spiegel.core.domain.Tenant;
  import be.vlaanderen.omgeving.spiegel.core.port.in.DereferenceResource;
  import be.vlaanderen.omgeving.spiegel.core.port.in.LoadTenantConfiguration;
  import be.vlaanderen.omgeving.spiegel.core.port.out.DataSourcePort;
  import be.vlaanderen.omgeving.spiegel.core.port.out.QueryInspector;
  import java.nio.file.Path;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;

  /** Wires the core to its adapters. The tenant is loaded and checked once, at start-up (ADR 0013). */
  @Configuration(proxyBeanMethods = false)
  class SpiegelConfiguration {

      @Bean
      QueryInspector queryInspector() {
          return new SparqlQueryInspector();
      }

      @Bean
      DataSourcePort dataSourcePort() {
          return new SparqlDataSource();
      }

      @Bean
      Tenant tenant(@Value("${spiegel.configuration.location}") Path location, QueryInspector inspector) {
          return new LoadTenantConfiguration(new TurtleTenantConfiguration(location), inspector).load();
      }

      @Bean
      DereferenceResource dereferenceResource(DataSourcePort dataSources) {
          return new DereferenceResource(dataSources);
      }
  }
  ```
- [ ] Extend `application.yml`:

  ```yaml
  server:
    # Trust X-Forwarded-* from the reverse proxy only (request flow, step 0); Tomcat's default internal proxies.
    forward-headers-strategy: native

  logging:
    structured:
      format:
        console: logstash
  ```
- [ ] Run: `mvn -q -pl spiegel-app -am test` — expect: PASS, including every test of `DereferenceContractTest`.
- [ ] Commit: `feat: wire feature 001 into the application`

---
## Phase 8: Performance and documentation

**Implements:** NFR performance; documentation of the decisions made while planning | **Satisfies:** the two performance NFRs of the spec
**Files:** `spiegel-app/src/test/java/be/vlaanderen/omgeving/spiegel/DereferencePerformanceTest.java`, `spiegel-app/pom.xml`, `docs/adr/0011-query-policy.md`, `docs/adr/0012-tenant-configuration-as-rdf.md`, `docs/specs/001-dereference-resource-as-rdf/quickstart.md`, `mkdocs.yml`
**Interfaces:** Consumes: `StubSparqlEndpoint.servingAfter(Graph, Duration)`, `ContractTenant.write(...)`, the running application. Produces: a test tagged `performance`, excluded from the default build.

Measure what SPieGeL adds to a store that answers in 50 ms, and record the planning decisions where they belong.

- [ ] In `spiegel-app/pom.xml`, exclude the tag by default:

  ```xml
  <properties>
    <surefire.excludedGroups>performance</surefire.excludedGroups>
  </properties>
  ...
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
      <excludedGroups>${surefire.excludedGroups}</excludedGroups>
    </configuration>
  </plugin>
  ```
- [ ] Write `DereferencePerformanceTest`:

  ```java
  package be.vlaanderen.omgeving.spiegel;

  import static org.assertj.core.api.Assertions.assertThat;

  import be.vlaanderen.omgeving.spiegel.adapter.sparql.testing.StubSparqlEndpoint;
  import java.io.IOException;
  import java.net.URI;
  import java.net.http.HttpClient;
  import java.net.http.HttpRequest;
  import java.net.http.HttpResponse;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.time.Duration;
  import java.util.ArrayList;
  import java.util.List;
  import org.apache.jena.graph.Graph;
  import org.apache.jena.graph.GraphMemFactory;
  import org.apache.jena.graph.NodeFactory;
  import org.apache.jena.graph.Triple;
  import org.junit.jupiter.api.AfterAll;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Tag;
  import org.junit.jupiter.api.Test;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.boot.test.web.server.LocalServerPort;
  import org.springframework.test.context.DynamicPropertyRegistry;
  import org.springframework.test.context.DynamicPropertySource;

  @Tag("performance")
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  @DisplayName("Feature 001 NFR: performance")
  class DereferencePerformanceTest {

      static final int RESOURCES = 1_000;
      static final Duration STORE_TIME = Duration.ofMillis(50);
      static final StubSparqlEndpoint STORE = StubSparqlEndpoint.servingAfter(data(), STORE_TIME);

      @DynamicPropertySource
      static void tenant(DynamicPropertyRegistry registry) throws IOException {
          Path dir = ContractTenant.write(Files.createTempDirectory("tenant"), STORE.url(), STORE.url(), STORE.url(), STORE.url());
          registry.add("spiegel.configuration.location", dir::toString);
      }

      @AfterAll
      static void stop() {
          STORE.close();
      }

      @LocalServerPort
      int port;

      private final HttpClient client = HttpClient.newHttpClient();

      @Test
      @DisplayName("SPieGeL adds at most 50 ms at p95 to a description")
      void descriptionOverhead() throws Exception {
          warmUp("/doc/organisatie/");
          List<Long> overhead = new ArrayList<>();
          for (int i = 0; i < RESOURCES; i++) {
              long elapsed = time("/doc/organisatie/" + i, 200);
              overhead.add(elapsed - STORE_TIME.toMillis());
          }
          assertThat(p95(overhead)).isLessThanOrEqualTo(50);
      }

      @Test
      @DisplayName("A 303 takes at most 20 ms at p95")
      void redirectTime() throws Exception {
          warmUp("/id/organisatie/");
          List<Long> times = new ArrayList<>();
          for (int i = 0; i < RESOURCES; i++) {
              times.add(time("/id/organisatie/" + i, 303));
          }
          assertThat(p95(times)).isLessThanOrEqualTo(20);
      }

      private void warmUp(String prefix) throws Exception {
          for (int i = 0; i < 100; i++) {
              send(prefix + i);
          }
      }

      private long time(String path, int expectedStatus) throws Exception {
          long start = System.nanoTime();
          HttpResponse<byte[]> response = send(path);
          long millis = (System.nanoTime() - start) / 1_000_000;
          assertThat(response.statusCode()).isEqualTo(expectedStatus);
          return millis;
      }

      private HttpResponse<byte[]> send(String path) throws Exception {
          return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                          .header("Accept", "text/turtle")
                          .header("X-Forwarded-Host", "data.example.org")
                          .header("X-Forwarded-Proto", "https")
                          .build(),
                  HttpResponse.BodyHandlers.ofByteArray());
      }

      private static long p95(List<Long> values) {
          List<Long> sorted = values.stream().sorted().toList();
          return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
      }

      private static Graph data() {
          Graph g = GraphMemFactory.createDefaultGraph();
          for (int i = 0; i < RESOURCES; i++) {
              g.add(Triple.create(NodeFactory.createURI("https://data.example.org/id/organisatie/" + i),
                      NodeFactory.createURI("https://example.org/ns/name"), NodeFactory.createLiteralString("Organisation " + i)));
          }
          return g;
      }
  }
  ```
- [ ] Run: `mvn -q -pl spiegel-app -am test -Dgroups=performance -Dsurefire.excludedGroups=none -Dsurefire.failIfNoSpecifiedTests=false` — expect: PASS. Record the measured p95 values in the commit message. If a target is missed, stop and report it with the figures; do not loosen the assertion.
- [ ] Amend ADR 0011, decision 2: replace "it walks the syntax tree with an `ElementWalker`" by "it compiles the query to the algebra and walks it, which also finds `SERVICE` inside `EXISTS` and `NOT EXISTS`, where an `ElementWalker` does not", and add an `Amended` line in the header naming spec 001.
- [ ] Amend ADR 0012: update the sketch (`spiegel:base` instead of `spiegel:hostName`, `spiegel:defaultDataSource`, `api:itemTemplate` on the exception, `spiegel:timeout` on a data source), and add an `Amended` line naming decisions 1 to 4 of this plan.
- [ ] Write `docs/specs/001-dereference-resource-as-rdf/quickstart.md` (content: the "Quickstart Validation" section below, as instructions) and add `plan.md`, `contracts/api.md` and `quickstart.md` under the spec in the `mkdocs.yml` nav.
- [ ] Run: `.venv/bin/mkdocs build --strict` — expect: no warnings.
- [ ] Commit: `test: measure the performance of feature 001` and `docs: record the configuration decisions of feature 001`

---

## Phase 9: Integration verification

**Implements:** All FRs | **Satisfies:** All ACs

- [ ] Run the full build: `mvn -q verify` — expect: BUILD SUCCESS, every test passing, `ArchitectureTest` included.
- [ ] Check the coverage matrix below: every AC has a passing test. Fix any gap before continuing.
- [ ] Follow the Quickstart Validation below by hand.
- [ ] Run `sdd-superpowers:sdd-review` in implementation mode (Mode B).
- [ ] Commit: `feat: complete feature 001, dereference a resource as RDF`

### Coverage matrix

| AC | Test |
|---|---|
| AC-1.1 | `DereferenceContractTest.Story1.idRedirectsToDoc`, `TenantTest.idMatch` |
| AC-1.2 | `DereferenceContractTest.Story1.redirectSendsNoQuery` |
| AC-1.3 | `DereferenceContractTest.Story1.redirectKeepsQueryString` |
| AC-1.4 | `DereferenceContractTest.Story1.invalidValueIsNotFound`, `UriTemplateTest.patternIsChecked` |
| AC-1.5 | `ForwardedRequestTest` |
| AC-1.6 | `DereferenceContractTest.Story1.trailingSlashIsNotFound`, `UriTemplateTest.trailingSlashDoesNotMatch` |
| AC-2.1 | `DereferenceContractTest.Story2.subjectTriples`, `DereferenceResourceTest.mergesResults` |
| AC-2.2 | `DereferenceContractTest.Story2.blankNodeDepth`, `DescriptionBoundsTest.blankNodeDepth`, `DefaultDescriptionQueryTest` |
| AC-2.3 | `DereferenceContractTest.Story2.labelsOfLinkedResources`, `DescriptionBoundsTest.labelAndTypeOnly` |
| AC-2.4 | `DereferenceContractTest.Story2.noIncomingRelations`, `DescriptionBoundsTest.noIncomingRelations` |
| AC-2.5 | `DereferenceContractTest.Story2.documentLinks`, `DereferenceResourceTest.addsDocumentLinks` |
| AC-2.6 | `DereferenceContractTest.Story2.emptyDescriptionIsNotFound`, `DereferenceResourceTest.emptyIsNotFound` |
| AC-2.7 | `DereferenceContractTest.Story2.twoSourcesMerged`, `DereferenceResourceTest.mergesResults` |
| AC-2.8 | `DereferenceContractTest.Story2.unknownHostIsNotFound`, `TenantTest.servesItsHostOnly` |
| AC-3.1 | `DereferenceContractTest.Story3.formatByAccept`, `RdfSerialiserTest.everyFormat` |
| AC-3.2 | `DereferenceContractTest.Story3.formatBySuffix`, `FormatNegotiatorTest.suffix` |
| AC-3.3 | `DereferenceContractTest.Story3.formatByParameter`, `FormatNegotiatorTest.formatParameter` |
| AC-3.4 | `DereferenceContractTest.Story3.defaultIsTurtle`, `FormatNegotiatorTest.noPreference` |
| AC-3.5 | `DereferenceContractTest.Story3.qualityValues`, `FormatNegotiatorTest.quality` |
| AC-3.6 | `DereferenceContractTest.Story3.preferenceOrder`, `FormatNegotiatorTest.preference` |
| AC-3.7 | `DereferenceContractTest.Story3.negotiatedHeaders` |
| AC-3.8 | `DereferenceContractTest.Story3.formatSpecificHeaders` |
| AC-3.9 | `DereferenceContractTest.Story3.alternateLinks` |
| AC-3.10 | `DereferenceContractTest.Story3.headLikeGet` |
| AC-3.11 | `DereferenceContractTest.Story3.htmlOrJsonNotAcceptable`, `FormatNegotiatorTest.notAcceptable` |
| AC-3.12 | `DereferenceContractTest.Story3.unknownFormatNotAcceptable`, `FormatNegotiatorTest.notAcceptable` |
| AC-3.13 | `DereferenceContractTest.Story3.conflictingFormats`, `FormatNegotiatorTest.conflict` |
| AC-3.14 | `DereferenceContractTest.Story3.rdfXmlImpossible`, `RdfSerialiserTest.rdfXmlRefused` |
| AC-3.15 | `DereferenceContractTest.Story3.rdfXmlFallsBack`, `FormatNegotiatorTest.preference` |
| AC-3.16 | `DereferenceContractTest.Story3.profileParameterIgnored`, `FormatNegotiatorTest.parametersIgnored` |
| AC-4.1 | `DereferenceContractTest.Story4.problemShape` and every `expectProblem` |
| AC-4.2 | `DereferenceContractTest.Story4.sourceError`, `SparqlDataSourceTest.storeErrorIsFailure` |
| AC-4.3 | `DereferenceContractTest.Story4.sourceTimeout`, `SparqlDataSourceTest.slowStoreTimesOut` |
| AC-4.4 | `DereferenceContractTest.Story4.sourceError`, `DereferenceResourceTest.oneFailureFailsAll` |
| AC-4.5 | `DereferenceContractTest.Story4.noStoreOnGatewayErrors` |
| AC-4.6 | `DereferenceContractTest.Story4.methodNotAllowed`, `WebAdapterTest.methodNotAllowed` |
| AC-4.7 | `WebAdapterTest.oneLogEntryPerRequest` |
| AC-4.8 | `WebAdapterTest.internalError` |
| AC-5.1 | `TurtleTenantConfigurationTest.minimalConfiguration`, `DereferenceContractTest.Story2.ownQueryForSpecificTemplate` |
| AC-5.2 | `DereferenceContractTest.Story2.ownQueryForSpecificTemplate`, `TurtleTenantConfigurationTest.specificTemplateWithQuery`, `TenantTest.mostSpecificWins` |
| AC-5.3 | `StartupTest`, `SparqlQueryInspectorTest`, `LoadTenantConfigurationTest` |
| AC-5.4 | `StartupTest.aShapeViolationIsRefused`, `TurtleTenantConfigurationTest.shapesAreChecked` |
| AC-5.5 | `TurtleTenantConfigurationTest.minimalConfiguration` |


---

## Quickstart Validation

1. `mvn -q -DskipTests package`
2. Start a SPARQL endpoint with test data, for example Apache Jena Fuseki: `fuseki-server --file=spiegel-app/src/test/resources/contract-data.ttl /ds` (endpoint `http://localhost:3030/ds/sparql`).
3. Write a tenant directory: `tenant.ttl` as in `tenants/minimal`, with `spiegel:endpoint <http://localhost:3030/ds/sparql>`.
4. `java -jar spiegel-app/target/spiegel-app-*.jar --spiegel.configuration.location=/path/to/tenant`
5. Check, with the host of the base:
   - `curl -si -H 'Host: data.example.org' http://localhost:8080/id/organisatie/42` → `303`, `Location: https://data.example.org/doc/organisatie/42`
   - `curl -si -H 'Host: data.example.org' http://localhost:8080/doc/organisatie/42` → `200`, Turtle, `Vary: Accept`, four `Link` alternates
   - `curl -si -H 'Host: data.example.org' http://localhost:8080/doc/organisatie/42.jsonld` → JSON-LD, no `Vary`
   - `curl -si -H 'Host: data.example.org' http://localhost:8080/doc/organisatie/43.rdf` → `406`, `application/problem+json`, the RDF/XML detail
   - `curl -si -H 'Host: data.example.org' http://localhost:8080/doc/organisatie/999` → `404` problem
   - `curl -si -X POST -H 'Host: data.example.org' http://localhost:8080/doc/organisatie/42` → `405`, `Allow: GET, HEAD`
6. In the log: one JSON line per request with `requestId`, `status` and `durationMs`.
7. Stop Fuseki and request `/doc/organisatie/42` → `502`, `Cache-Control: no-store`, no store message.
