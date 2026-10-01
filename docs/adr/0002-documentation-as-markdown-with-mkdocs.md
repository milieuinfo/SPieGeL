# 0002. Documentation as Markdown, published with MkDocs

- **Status:** Accepted
- **Date:** 2026-10-01

## Context

The analysis that preceded SPieGeL was written as self-contained HTML files. That works for an internal snapshot, but not for an open-source project: HTML is hard to review in pull requests, GitHub does not render it, and external contributors expect plain text.

## Decision

- All documentation is Markdown under `docs/`, written in British English.
- Diagrams use [Mermaid](https://mermaid.js.org/), which renders both on GitHub and on the documentation site.
- The documentation site is built with [MkDocs](https://www.mkdocs.org/) and the [Material](https://squidfunk.github.io/mkdocs-material/) theme, and published on GitHub Pages at <https://milieuinfo.github.io/SPieGeL/>.
- The build runs with `--strict`, so broken links fail the build.

## Consequences

- One source serves both the repository view and the site.
- The documentation toolchain needs Python. That is acceptable because it runs only in CI and on writers' machines, not in the product.
- MkDocs is pinned below 2.0 (`requirements-docs.txt`). The Material team [warns](https://squidfunk.github.io/mkdocs-material/blog/2026/02/18/mkdocs-2.0/) that MkDocs 2.0 removes the plug-in and theming systems that Material depends on. Before lifting the pin, we will review the options again, for example a successor to Material or another generator.
- A formal specification (for example of the configuration format) may later need a W3C-style tool such as ReSpec. That would be a separate decision.
