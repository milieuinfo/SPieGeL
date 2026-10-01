# SPieGeL: notes for Claude

## Language

**All documentation and code are written in British English.** This covers README files, Javadoc, comments, identifiers, log and error messages, configuration keys, test names and commit messages. Examples: *behaviour*, *serialise*, *authorisation*, *licence* (noun) / *license* (verb), *colour*, *catalogue*, *initialise*.

Exceptions: names fixed by external standards or APIs stay as they are (e.g. HTTP `Authorization` header, `Content-Type`, Spring/Jena class names, W3C vocabulary terms such as `dcat:Catalog`).

## What this is

A configurable Linked Data publishing server: Spring Boot, hexagonal architecture (ports & adapters), Apache Jena. It succeeds a NetKernel-based LOD platform at the Flemish Department of Environment and Spatial Development. See `README.md` for goals.

## Documentation

- Markdown under `docs/`, site built with MkDocs Material (`mkdocs.yml`, `requirements-docs.txt`), published to <https://milieuinfo.github.io/SPieGeL/> by Jenkins (`Jenkinsfile.groovy`). See ADRs 0002 and 0003.
- Check locally with `mkdocs build --strict`; broken links fail the build.
- Analysis lives in `docs/analysis/`, decisions in `docs/adr/`. Requirement IDs (`FR-…`, `NFR-…`) are stable: never renumber.

## Public repository

This repository is public (`github.com/milieuinfo/SPieGeL`, MIT). Do not commit internal host names, cluster names, credentials, internal ticket numbers or details of security findings in the predecessor platform. Keep internal analysis material outside this repository.
