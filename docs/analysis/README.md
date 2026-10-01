# Analysis

This section collects the analysis behind SPieGeL. It describes what the predecessor platform does, what its successor must do, what already exists elsewhere, and how we intend to build it.

The analysis is a **snapshot**. It records what we found and why we concluded what we did. Decisions taken on the basis of it are recorded separately as [architecture decision records](../adr/README.md).

## Reading guide

| Chapter | Question it answers | Start here if you are… |
|---|---|---|
| [1. Context](01-context/background.md) | Why does SPieGeL exist, and what is in scope? | new to the project |
| [2. Current platform](02-current-platform/functional-overview.md) | What does the predecessor do today, exactly? | migrating behaviour or checking compatibility |
| [3. Requirements](03-requirements/functional.md) | What must SPieGeL do, and how important is each item? | planning work or writing tests |
| [4. Landscape](04-landscape/comparison.md) | What existing open-source software does similar things? | asking "why not reuse X?" |
| [5. Architecture](05-architecture/hexagonal-design.md) | How is SPieGeL structured? | writing code |
| [6. Open questions](06-open-questions.md) | What is still undecided? | looking for something to resolve |

## Conventions

- British English throughout.
- *Predecessor* means the NetKernel-based Linked Open Data platform that SPieGeL replaces.
- Requirement priorities use [MoSCoW](https://en.wikipedia.org/wiki/MoSCoW_method): **M**ust, **S**hould, **C**ould, **W**on't (this time).
- Requirement identifiers (`FR-…`, `NFR-…`) are stable. Do not renumber them; deprecate instead.
