# Security policy

## Reporting a vulnerability

**Please do not report security problems in public issues, pull requests or discussions.**

Report them privately through GitHub instead: open the repository's **Security** tab and choose **Report a vulnerability**. This creates a private advisory that only the maintainers can see. Please include:

- what is affected (module, endpoint, configuration) and which version or commit;
- how to reproduce it, against a local instance and test data;
- what an attacker could achieve;
- whether, and when, you intend to disclose it yourself.

Please test only against your own instances. Do not test against services operated by the Flemish Government.

## What happens next

SPieGeL is maintained by a small team at the Department of Environment and Spatial Development (Departement Omgeving). There is no bug bounty. We aim to:

- acknowledge your report within **5 working days**;
- give a first assessment within **15 working days**;
- agree a disclosure date with you, normally no later than **90 days** after the report, or sooner once a fix is released.

We will credit you in the advisory and the release notes, unless you prefer not to be named.

## Supported versions

SPieGeL is in an early design phase and has no releases yet. Until the first release, only the `main` branch is supported. This section will list the supported release lines once they exist.

## Scope

In scope:

- the source code in this repository, including its default configuration and example configurations;
- the documentation, where it recommends an insecure set-up.

Out of scope:

- vulnerabilities in dependencies that SPieGeL does not make exploitable. Please report those to the dependency's maintainers. Do tell us if SPieGeL uses the dependency in a vulnerable way;
- findings that require a deliberately insecure configuration, such as granting write rights on a data source to the account SPieGeL uses (see NFR-SEC-01 in the [non-functional requirements](docs/analysis/03-requirements/non-functional.md));
- open CORS and the public, read-only SPARQL endpoint. These are deliberate open-data choices (NFR-OPEN-01).

If you find a problem in a running service of the Flemish Government that uses SPieGeL, you may report it through the same channel. We will pass it on to the team that operates the service.

## Security requirements

The security requirements SPieGeL is built to are listed as NFR-SEC-01 to NFR-SEC-08 in the [non-functional requirements](docs/analysis/03-requirements/non-functional.md). A report showing that SPieGeL does not meet one of them is always in scope.
