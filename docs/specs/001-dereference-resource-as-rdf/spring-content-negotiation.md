# Content negotiation by Spring MVC: limits against feature 001

**Feature:** [001, Dereference a resource as RDF](spec.md)
**Date:** 2026-10-27

## Question

Should Spring MVC's own content negotiation choose the RDF format of a description, instead of the `FormatNegotiator` in the [plan](plan.md)? In that approach, an `HttpMessageConverter` is registered per RDF format, the controller returns a Jena `Graph` and declares the formats in `produces`, and Spring picks the format from `Accept` or a request parameter.

## How it was tested

A minimal application on the versions of the plan: Spring Boot 4.1.1 (Spring Framework 7), Jena 6.2.0, Java 21, Tomcat. It has:

- four converters, extending `AbstractHttpMessageConverter<Graph>`, that write with `RDFDataMgr` in Turtle, JSON-LD, N-Triples and RDF/XML, registered first and in SPieGeL's order of preference;
- one handler, `@GetMapping(value = "/doc/organisatie/{id}", produces = {"text/turtle", "application/ld+json", "application/n-triples", "application/rdf+xml"})`, that returns a `Graph`;
- parameter negotiation and problem details switched on:

  ```properties
  spring.mvc.problemdetails.enabled=true
  spring.mvc.contentnegotiation.favor-parameter=true
  spring.mvc.contentnegotiation.parameter-name=_format
  spring.mvc.contentnegotiation.media-types.ttl=text/turtle
  spring.mvc.contentnegotiation.media-types.nt=application/n-triples
  spring.mvc.contentnegotiation.media-types.rdf=application/rdf+xml
  spring.mvc.contentnegotiation.media-types.jsonld=application/ld+json
  ```

Resource `42` has one `rdfs:label`. Resource `43` uses the predicate `<https://example.org/ns/eigenschap/123>`, which cannot be written as RDF/XML. Resource `44` has 20,000 triples and that same predicate. The requests were sent with MockMvc, and those that fail on writing were repeated against the running server.

## Results

| # | Request | Spring MVC | Spec 001 | AC |
|---|---|---|---|---|
| 1 | no `Accept`, or `*/*` | `200` Turtle | `200` Turtle | AC-3.4 ✓ |
| 2 | `application/ld+json;q=0.5, text/turtle;q=0.9` | `200` Turtle | `200` Turtle | AC-3.5 ✓ |
| 3 | `?_format=nt` with `Accept: text/turtle` | `200` N-Triples | `200` N-Triples | AC-3.3 ✓ |
| 4 | `Accept: text/html` | `406`, `application/problem+json` | `406` `not-acceptable` | AC-3.11 ✓, but without a problem type |
| 5 | `/999` (unknown) with `Accept: text/turtle` | `404`, `application/problem+json` | `404` `not-found` | AC-2.6 ✓, but without a problem type |
| 6 | `application/rdf+xml, application/ld+json` | `200` **RDF/XML** | `200` JSON-LD | AC-3.6 ✗ |
| 7 | `/doc/organisatie/42.ttl` | `200` Turtle, describing **`<…/organisatie/42.ttl>`** | `200` Turtle of `<…/organisatie/42>` | AC-3.2 ✗ |
| 8 | `?_format=xyz` | `406`, **empty body** | `406` `not-acceptable` as problem details | AC-3.12 ✗ |
| 9 | `Accept: text/turtle;q=abc` | `406`, **empty body** | `400` `invalid-request` | ✗ |
| 10 | `application/ld+json; profile="…#compacted"` | `200`, `Content-Type: application/ld+json;profile="…#compacted"` | `200` JSON-LD, profile ignored | AC-3.16 ✗ |
| 11 | any negotiated `200` | **no** `Vary`, **no** `Content-Location` | `Vary: Accept`, `Content-Location` | AC-3.7 ✗ |
| 12 | `/43`, `Accept: application/rdf+xml` | `500`, **empty body** (`InvalidPropertyURIException`) | `406` `not-acceptable`, with a `detail` | AC-3.14 ✗ |
| 13 | `/43`, `application/rdf+xml, text/turtle;q=0.5` | `500`, **empty body** | `200` Turtle | AC-3.15 ✗ |
| 14 | `/44` (large), `Accept: application/rdf+xml` | **`200`**, then the connection is closed after 1.4 MB | `406` `not-acceptable` | AC-3.14 ✗, FR-3 ✗ |

## The limits

1. **No order of preference of the server's own.** At equal quality, Spring keeps the client's order (row 6). RFC 9110 leaves the choice to the server, and FR-CN-08 makes it SPieGeL's: Turtle, JSON-LD, N-Triples, RDF/XML. The order in which converters are registered decides only `*/*` and a missing `Accept`.

2. **The format is fixed before the description is known.** Spring chooses one media type and only then calls the converter. When that converter cannot write the description, there is no second choice: the result is a `500` without problem details (rows 12 and 13). Worse, a large description is sent with `200` and broken off halfway (row 14): the client gets a truncated document, and caches and crawlers may take it as valid. This breaks the rule "never `200` and then fail" (FR-3) and AC-3.14 and AC-3.15. Avoiding it means serialising before choosing, which takes the work out of the converter.

3. **Format suffixes are not supported, and fail silently.** Spring Framework 6 removed suffix pattern matching and path-extension negotiation. `{id}` then captures `42.ttl`, and the application describes a resource that does not exist, with `200` (row 7). In SPieGeL the suffix is recognised before template matching (FR-3), so it has to be our own code anyway; and so does the rule that a suffix and `_format` that disagree give `400` (AC-3.13), since Spring's strategies have no notion of a conflict: the first that finds a format wins.

4. **`Vary` and `Content-Location` are not set** (row 11). They must be sent only when the format was negotiated from `Accept`, not for a format URL (AC-3.7, AC-3.8). Spring does not tell the handler or a `ResponseBodyAdvice` *how* the format was chosen, so that would need a `ContentNegotiationStrategy` of our own that records it.

5. **Media type parameters reach the response.** Spring copies the more specific acceptable type into `Content-Type`, so a JSON-LD response claims a `profile` it does not honour (row 10). Profiles are a phase 2 concern (FR-CN-03); until then the parameter must be ignored (AC-3.16).

6. **Errors from negotiation are not SPieGeL's problem details.** An unknown `_format` and an unreadable `Accept` give an empty `406` (rows 8 and 9); the spec asks for problem details with the types of ADR 0016, and `400` for an unreadable header. Where Spring does write problem details (rows 4 and 5), it leaves out `type`, which RFC 9457 reads as `about:blank`, sets `title` to the reason phrase and `detail` to its own message:

   ```json
   {"detail":"Acceptable representations: [text/turtle, application/ld+json, application/n-triples, application/rdf+xml].",
    "instance":"/doc/organisatie/42","status":406,"title":"Not Acceptable"}
   ```

   A client then cannot tell "SPieGeL does not offer this format" from "this description cannot be expressed in RDF/XML", since both are a `406` without a type.

## What Spring does well

Quality values (row 2), a default when nothing is asked (row 1), the `_format` parameter (row 3), and a `406` or `404` as `application/problem+json` even when the client accepts only Turtle (rows 4 and 5). Parsing and comparing media types, `MediaType.parseMediaTypes`, `includes` and `getQualityValue`, is reused by the plan's `FormatNegotiator`.

## Working around the limits inside Spring

A second application keeps the same handler, converters and resources, and adds the work-arounds that Spring MVC offers for each limit:

| Extension point | What it does | Limit |
|---|---|---|
| servlet `Filter` | strips a format suffix and wraps the request, so that `{id}` matches `42`; records the suffix as a request attribute | 3 |
| `ContentNegotiationStrategy`, replacing Spring's | suffix, then `_format`, then `Accept`; `400` on a conflict or an unreadable header; computes the quality of each format and returns only concrete media types, in SPieGeL's order at equal quality; records whether the format was negotiated | 1, 3, 4, 6 |
| `HttpMessageConverter` | writes to a buffer first and throws an exception of its own when Jena fails; drops media type parameters from `Content-Type` | 2, 5 |
| `ResponseBodyAdvice<Graph>` | sets `Vary: Accept` and `Content-Location` when the format was negotiated | 4 |
| `@RestControllerAdvice` with `@Order(HIGHEST_PRECEDENCE)` | SPieGeL's problem types; on an RDF/XML failure, writes the next acceptable format, or `406` | 2, 6 |
| `WebMvcConfigurer` | registers the strategy and the converters | |

All requests above, and eight more, then give what the spec asks for:

| # | Request | With the work-arounds | AC |
|---|---|---|---|
| 6 | `application/rdf+xml, application/ld+json` | `200` JSON-LD | AC-3.6 ✓ |
| 7 | `/doc/organisatie/42.ttl` | `200` Turtle of `<…/organisatie/42>`, no `Vary`, no `Content-Location` | AC-3.2 ✓, AC-3.8 ✓ |
| 8 | `?_format=xyz` | `406` `not-acceptable` | AC-3.12 ✓ |
| 9 | `Accept: text/turtle;q=abc` | `400` `invalid-request` | ✓ |
| 10 | `application/ld+json; profile="…#compacted"` | `200`, `Content-Type: application/ld+json` | AC-3.16 ✓ |
| 11 | no `Accept` | `200` Turtle, `Vary: Accept`, `Content-Location: …/42.ttl` | AC-3.7 ✓ |
| 12 | `/43`, as `.rdf`, `_format=rdf` or `Accept: application/rdf+xml` | `406` `not-acceptable`, with the `detail` | AC-3.14 ✓ |
| 13 | `/43`, `application/rdf+xml, text/turtle;q=0.5` | `200` Turtle, `Content-Location: …/43.ttl` | AC-3.15 ✓ |
| 14 | `/44` (large), `Accept: application/rdf+xml` | `406` `not-acceptable`, nothing of the body sent | AC-3.14 ✓, FR-3 ✓ |
| 15 | `/42.ttl?_format=nt` | `400` `invalid-request` | AC-3.13 ✓ |
| 16 | `/43`, `application/rdf+xml, application/ld+json` | `200` JSON-LD | AC-3.6 ✓ |
| 17 | `*/*;q=0.1, text/turtle;q=0` | `200` JSON-LD | ✓ |
| 18 | `Accept: text/html` or `application/json` | `406` `not-acceptable` | AC-3.11 ✓ |
| 19 | `/999` | `404` `not-found` | AC-2.6 ✓ |

Getting there took four rounds, because Spring behaves in ways that its documentation does not lead one to expect:

1. **Spring does not exclude a format with `q=0` under a wider range.** With the client's ranges merely re-sorted, `*/*;q=0.1, text/turtle;q=0` still gave Turtle. RFC 9110 lets the most specific range decide. The strategy therefore computes the quality of each format itself, which is the core of `FormatNegotiator`.
2. **Spring negotiates a second time to choose the exception handler.** An exception from the strategy was thrown again while it was being handled, and hid the problem response. The strategy now remembers in a request attribute that it failed, and answers `*/*` the second time.
3. **The `produces` condition swallows `HttpMediaTypeNotAcceptableException`.** Together with the previous work-around, `?_format=xyz` then gave `200` Turtle. The strategy has to throw an exception of its own.
4. **Spring Boot's own problem details handler comes first**, so SPieGeL's handlers must be given the highest precedence.

There is also a cost that the probes do not show. When the converter fails, it has already set `Content-Type`, `Vary` and `Content-Location` on the response. The `406` therefore calls `response.reset()`, which removes every other header as well: in SPieGeL, the CORS headers that the request filter sets on every response would have to be set again.

`HEAD` (AC-3.10) and the `Link` alternates (AC-3.9) are in neither application and were not tested.

## Conclusion

Every limit can be worked around inside Spring, and the second application shows that this was done. It takes 385 lines over six extension points, against 61 for the plain application, and two of the work-arounds lean on how Spring behaves internally rather than on a documented contract: the second negotiation, and the `produces` condition that swallows an exception. The strategy at the centre computes the quality of each format and the order of preference, which is what `FormatNegotiator` does. The result is `FormatNegotiator` rebuilt, spread over six extension points instead of one class with a unit test per acceptance criterion.

The plan therefore keeps its own negotiation, uses Spring MVC for routing, `ResponseEntity` and the exception handlers, and uses Spring's `MediaType` for parsing (decision 11).
