package io.akka.pageindex.application;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Parses and formats page-range specifications ({@code "5"}, {@code "3,7,10"},
 * {@code "5-10"}, {@code "1-3,7,9-12"}).
 *
 * <p>Ported from {@code pageindex/agent_tools.py}'s {@code _expand_pages} /
 * {@code _format_page_spec} (SPEC-001 rules 6-7).
 */
public final class PageRange {

  /** Matches the source's {@code _MAX_REQUESTED_PAGES = 10_000}. */
  public static final int MAX_REQUESTED_PAGES = 10_000;

  private PageRange() {}

  public static final class PageRangeException extends RuntimeException {
    public PageRangeException(String message) {
      super(message);
    }
  }

  /**
   * Expands a page spec into a sorted list of distinct page numbers. Rejects a spec whose
   * expansion would exceed {@link #MAX_REQUESTED_PAGES} distinct pages BEFORE materializing
   * it — a part's size is checked arithmetically first (rule 6).
   */
  public static List<Integer> expand(String pages) {
    if (pages == null || pages.isBlank()) {
      throw new PageRangeException("Invalid page specification: " + pages);
    }
    TreeSet<Integer> expanded = new TreeSet<>();
    for (String rawPart : pages.split(",")) {
      String part = rawPart.strip();
      long start;
      long end;
      try {
        if (part.contains("-")) {
          String[] bounds = part.split("-", 2);
          start = Long.parseLong(bounds[0].strip());
          end = Long.parseLong(bounds[1].strip());
          if (start > end) {
            throw new PageRangeException("Invalid range '" + part + "': start must be <= end");
          }
        } else {
          start = end = Long.parseLong(part);
        }
      } catch (NumberFormatException e) {
        throw new PageRangeException("Invalid page specification '" + pages + "'");
      }
      // Bound each part arithmetically (as a long, before truncating to int) before
      // materializing it — a spec like "1-1000000000" would otherwise try to build a set
      // of that many entries, and an overflowing literal like "1-99999999999" must fail
      // with the same "too many pages" message rather than a parse error.
      if (end - start + 1 > MAX_REQUESTED_PAGES) {
        throw new PageRangeException(
            "Page specification '" + pages + "' spans more than " + MAX_REQUESTED_PAGES
                + " pages; request a narrower range");
      }
      for (long p = start; p <= end; p++) {
        expanded.add((int) p);
      }
      if (expanded.size() > MAX_REQUESTED_PAGES) {
        throw new PageRangeException(
            "Page specification '" + pages + "' spans more than " + MAX_REQUESTED_PAGES
                + " pages; request a narrower range");
      }
    }
    if (expanded.first() < 1) {
      throw new PageRangeException("Invalid page numbers. Page numbers must be positive integers");
    }
    return List.copyOf(expanded);
  }

  /** Compresses a page list into minimal run-length notation (rule 7). */
  public static String format(List<Integer> pages) {
    if (pages == null || pages.isEmpty()) {
      return "";
    }
    TreeSet<Integer> ordered = new TreeSet<>(pages);
    List<String> ranges = new ArrayList<>();
    Integer start = null;
    Integer prev = null;
    for (int page : ordered) {
      if (start == null) {
        start = prev = page;
        continue;
      }
      if (page == prev + 1) {
        prev = page;
        continue;
      }
      ranges.add(start.equals(prev) ? start.toString() : start + "-" + prev);
      start = prev = page;
    }
    ranges.add(start.equals(prev) ? start.toString() : start + "-" + prev);
    return String.join(",", ranges);
  }
}
