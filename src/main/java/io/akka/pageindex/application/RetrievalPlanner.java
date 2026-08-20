package io.akka.pageindex.application;

import io.akka.pageindex.domain.RetrievalStrategy;

/**
 * The retrieval decision: given a document's page count alone, which fetch strategy applies.
 *
 * <p>Ported from {@code pageindex/agent_tools.py}'s {@code _get_document} threshold branches
 * (SPEC-001 rule 8). No embeddings, no content inspection — this IS PageIndex's retrieval
 * decision, not a simplification of it.
 */
public final class RetrievalPlanner {

  /** {@code pageCount <= this} → fetch everything directly. */
  public static final int FETCH_ALL_MAX_PAGES = 5;

  /** Matches the source's {@code STRUCTURE_FIRST_PAGE_THRESHOLD = 20}. */
  public static final int STRUCTURE_FIRST_THRESHOLD = 20;

  private RetrievalPlanner() {}

  public static RetrievalStrategy plan(int pageCount) {
    if (pageCount <= FETCH_ALL_MAX_PAGES) {
      return RetrievalStrategy.FETCH_ALL;
    }
    if (pageCount <= STRUCTURE_FIRST_THRESHOLD) {
      return RetrievalStrategy.STRUCTURE_THEN_KEY_PAGES;
    }
    return RetrievalStrategy.STRUCTURE_THEN_TARGETED_RANGE;
  }
}
