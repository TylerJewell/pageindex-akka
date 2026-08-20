package io.akka.pageindex.domain;

/**
 * One row of a flat, already page-anchored table of contents, in document order.
 *
 * <p>{@code structure} is a dotted path ({@code "1"}, {@code "1.2"}, {@code "1.2.1"}) that
 * places this entry in the hierarchy; {@code null} or empty means a root entry.
 * {@code appearsAtPageStart} answers, for THIS entry, whether its title is confirmed to be
 * the first thing on {@code physicalIndex} — it is read only when computing the END index of
 * the entry immediately BEFORE it in the list, never this entry's own end index.
 */
public record TocEntry(String structure, String title, int physicalIndex, boolean appearsAtPageStart) {}
