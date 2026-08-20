package io.akka.pageindex.domain;

/** What to fetch, decided purely from page count — no embeddings, no content inspection. */
public enum RetrievalStrategy {
  /** {@code pageCount <= 5}: fetch the whole document directly. */
  FETCH_ALL,
  /** {@code 5 < pageCount <= 20}: read the structure first, then a few named key pages. */
  STRUCTURE_THEN_KEY_PAGES,
  /** {@code pageCount > 20}: read the structure first, then a narrow targeted range. */
  STRUCTURE_THEN_TARGETED_RANGE
}
