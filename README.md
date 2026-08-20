# pageindex-akka

Turns a flat, page-anchored table of contents into a cost-optimal navigable tree, and
decides what to fetch to answer a question from page count and tree shape alone — no
search index, no similarity score.

A port of [VectifyAI/PageIndex](https://github.com/VectifyAI/PageIndex) onto **Akka**,
built with **Akka Specify**.

---

## Where it came from

VectifyAI/PageIndex is a Python library that builds a table of contents for a long PDF
and lets a language model navigate that tree to answer questions, instead of chopping
the document into overlapping chunks and searching them by similarity. It was ported to
derive a specification format precise enough to regenerate a system on a different
stack — the port is the vehicle, the specification is the deliverable.

The specifications this port was generated from are in
[TylerJewell/akka-specify-harness](https://github.com/TylerJewell/akka-specify-harness)
under `pageindex-port/`.

---

## VectifyAI/PageIndex → this port

📉 221 Python lines (the ported behaviour only) → **347 Java lines**<br>
📁 3 Python files touched → **8 Java files**<br>
🖥️ 1 process (a library call) → **1 process** (a running service)<br>
⚡ 0.0035 seconds, in-process call → **0.0151** seconds, HTTP round trip<br>
🎯 2 of 2 comparison scenarios agree exactly → **2 of 2**

Full method and the numbers that did not make this list:
[`bench/REPORT.md`](../pageindex-port/bench/REPORT.md).

---

## What it took to build

⏱️ **0.5 hours** from the first command to the published repository, **0.5** of them
active<br>
💬 **269** exchanges with the model<br>
✍️ **148,125** tokens written by the model, **54,786,578** counting everything sent
and re-sent<br>
🙋 **0** questions to a human<br>
🧪 22 tests

```bash
python toolkit/tokens.py --port pageindex    # turns, tokens, elapsed and active time
```

The record of every question, and where the time went, is in
[`port-log/`](../port-log).

---

## What it does

From the specification:

- **A section's page range never grows to include its subsections.** Its `endIndex`
  covers only its own position among its siblings; a caller that needs the true extent
  of everything underneath a section asks for that separately.
- **A subsection is folded back into its parent whenever reading it linearly costs no
  more than routing through its structure.** A two-page subsection under a two-page
  sibling gets absorbed; its title survives as a note on the parent so nothing that
  could be searched for disappears.
- **What to fetch is decided from page count alone.** Five pages or fewer: everything,
  directly. Between six and twenty: the structure first, then a few named pages.
  Above twenty: the structure first, then a narrow page range. No document content
  ever changes which of the three applies.

---

## Design decisions

**A section keeps its own page span, not its subtree's.** The source computes this
value once, from a section's position among its siblings, and a later step
(`subtreeEnd`) answers "how far does everything underneath this section reach" as a
separate, on-demand question. Keeping the two apart preserves an existing behaviour
that later steps depend on, instead of quietly resolving what may look like a
one-line inconsistency.

**The optimizer rewrites the tree in place rather than building a new one.** A section
that gets folded away loses its children and gains the titles they were reachable by,
directly on the same node object. This mirrors exactly what the original algorithm
does to its own tree, so a reader checking one against the other is checking the same
shape of change, not two designs that happen to agree on the end result.

**Page ranges are checked for size before they are ever built, not after.** A request
for a billion pages is rejected by doing arithmetic on the numbers in the request,
before anything tries to hold that many pages in memory. Catching the same mistake
afterward, once memory runs out, would already be too late to say anything useful back
to whoever asked.

---

## Running it — the short path

You do not need Java, Maven, or the Akka CLI installed. Akka Specify installs them for
you.

**1. Install Akka Specify** in Claude Code:

```
/plugin marketplace add akka/ai-marketplace
/plugin install akka@akka-ai-marketplace
```

Restart Claude Code when it asks.

**2. Give it this prompt:**

> Clone https://github.com/TylerJewell/pageindex-akka into a new directory and open it.
> Then run /akka:setup to install everything this project needs, and /akka:build to
> compile it, run the tests, and start it locally.

**3. Send it a request:**

```bash
curl -X POST http://localhost:9019/index/build-optimized \
  -H "Content-Type: application/json" \
  -d '{"entries":[{"structure":"1","title":"Intro","physicalIndex":1,"appearsAtPageStart":true}],"endOfDocument":5}'
```

---

## Running it — the developer path

### Requirements

- Java 21 or newer
- Maven 3.9 or newer
- An Akka download token — run `akka code token` once

### Start the service

```bash
mvn compile
akka local run
```

The service starts on **port 9019**.

---

## Configuration

Nothing beyond the port above — this slice makes no calls to a model provider and
holds no other configuration.

---

## Where it differs from VectifyAI/PageIndex

Everything not listed here behaves the same way on purpose, including the parts that
look like mistakes.

- **This service never parses a PDF, calls a model, or stores a document.** The
  original builds its table of contents and its page summaries with a language model
  reading actual PDF pages; this port starts one step later, from an already-produced
  flat table of contents, and rebuilds only the tree construction and the
  cost-based optimization on top of it. A caller supplies the flat list this port
  needs; nothing here manufactures one from a PDF.
- **The `expand` half of the original's tree optimizer — proposing brand-new
  subsections with a model — is not ported.** Only `merge`, the deterministic half the
  original itself calls its "no-LLM default path," is rebuilt. A tree given to this
  port is only ever made smaller, never given new sections it did not already have.
- **Document management — uploading, browsing, deleting, and folder navigation — is
  out of scope.** The original's cloud and local surfaces both wrap those operations
  around the same retrieval decision this port rebuilds; this port exposes the
  retrieval decision alone, as a stateless computation, with no document store behind
  it.
- **A section whose very first subsection starts on the very same page can get an
  `endIndex` smaller than its own `startIndex`, or even `0`.** This is not a defect
  introduced here — running the original's own `post_processing` on the same input
  produces the identical value (`bench/REPORT.md` §1, scenario `row1_edge_case`). It is
  carried forward unchanged because changing it would stop this from being a port of
  the source's actual behaviour, quirks included.

---

## Licence

VectifyAI/PageIndex is MIT licensed, © 2025 Vectify AI. This port reimplements the
behaviour described above without copying source; see `ACKNOWLEDGEMENTS.md`.
