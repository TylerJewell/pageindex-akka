# Acknowledgements

This project is a port of **[VectifyAI/PageIndex](https://github.com/VectifyAI/PageIndex)**.

## Licence of the original

**MIT**, © 2025 Vectify AI. Read from the `LICENSE` file at the root of the repository
at commit `5f44d69`, not from a badge.

## What was copied

**No source was copied.** No file, function, class or fragment of `VectifyAI/PageIndex`
appears in this project. Everything here is written against a behavioural
specification — `pageindex-port/specs/SPEC-001-pageindex.md` in the harness repository —
and the Java in `src/main` shares no text with the Python it was derived from.

Two things did cross over, and neither is source:

- **The behaviour itself.** How a flat table of contents becomes a hierarchy, how a
  subtree is judged not worth keeping and folded back into its parent, how a
  page-range spec is parsed and rejected when it is too large, and the page-count
  thresholds that decide what to fetch — all of these are derived from
  `VectifyAI/PageIndex`, and reproduce it deliberately. That is what a port is, and it
  is not something to be coy about.
- **Scenario inputs.** `pageindex-port/bench/scenarios.json` in the harness repository
  holds the exact synthetic table-of-contents fixtures fed through both systems to
  compare their answers, including the one that reproduces the source's own
  `endIndex`-can-go-to-`0` edge case. They were written for that comparison; none is
  taken from the original's own test suite.

The probes and benchmark runners in the harness repository import and call
`VectifyAI/PageIndex` unmodified, from a clone kept beside the harness. They live
there, not here, and this project does not depend on it at build time or at run time.

## What that means for this project's licence

MIT is a permissive licence and imposes no share-alike obligation, so nothing about the
original constrains what this project may be licensed as. Its attribution clause
applies to redistributed copies of its own source, and none is included here; the
attribution above is given because it is owed to the work this was derived from, not
because a copied file forces it.

## Also used

- **[Akka](https://akka.io)** — the SDK and runtime this port is built on
  (`akka-javasdk` 3.6.3, Business Source License 1.1).
