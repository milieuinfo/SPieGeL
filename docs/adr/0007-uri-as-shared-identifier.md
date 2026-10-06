# 0007. Use the URI as the shared identifier in every distribution

- **Status:** Proposed
- **Date:** 2026-10-06

## Context

The same things are published in several forms. Besides SPieGeL's resource endpoints and event streams ([ADR 0006](0006-resources-and-event-streams.md)), a river segment may also be a feature in an OGC API Features collection or a WFS layer, a row in a CSV download, or a record in a business API. Each form tends to have its own local identifier. A user who finds a segment in a map viewer then has no way to reach everything else that is known about it, and a consumer cannot join the forms without a lookup table.

The W3C [Spatial Data on the Web Best Practices](https://www.w3.org/TR/sdw-bp/) recommend globally unique, persistent HTTP URIs for spatial things ([Best Practice 1](https://www.w3.org/TR/sdw-bp/#globally-unique-ids)), and linking resources together ([Best Practice 3](https://www.w3.org/TR/sdw-bp/#linking)).

## Decision

1. **The `id` URI is the identifier of a thing in every form in which it is distributed**, not only in Linked Data. A GIS layer, a download or an API carries the thing's URI, for example as an attribute of each feature, next to any local identifier it needs.
2. **Any application that shows a thing can act as a portal.** A map viewer, a dashboard or a spreadsheet links to the URI. Following that link in a browser leads, through the `303` redirect, to SPieGeL's subject page, with more information and links to related resources.
3. **SPieGeL's subject page is the landing page** for such links, and can link back to the thing in the other forms, for example to the feature in an OGC API or to a map viewer centred on it (FR-HTML-13).

## Consequences

- SPieGeL's subject pages must work as landing pages for users who arrive from elsewhere: HTML by default for a browser (FR-CN-08), readable without JavaScript (NFR-UI-04), and with the URI policy kept stable (NFR-OPEN-02).
- Data owners must add the URI to their other distributions. That is work outside SPieGeL, and it needs a shared convention: the attribute name, and how a feature links to its URI. GML is a constraint: a `gml:id` must be an XML name and cannot hold a URI, so the URI goes into an attribute. Where the convention is documented and who owns it is open question 32.
- Joining data across forms becomes a matter of matching URIs, without lookup tables.
- A URI that is published in a GIS layer is as hard to change as one published in Linked Data. NFR-OPEN-02 applies to it as well.
