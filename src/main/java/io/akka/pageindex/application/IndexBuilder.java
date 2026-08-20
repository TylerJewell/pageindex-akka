package io.akka.pageindex.application;

import io.akka.pageindex.domain.IndexNode;
import io.akka.pageindex.domain.TocEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a hierarchical {@link IndexNode} tree from a flat, page-anchored table of contents.
 *
 * <p>Ported from {@code pageindex/utils.py}'s {@code post_processing} + {@code list_to_tree}
 * (SPEC-001 rules 1 and 3). Everything upstream of this — turning PDF bytes into
 * {@link TocEntry} rows via an LLM — is out of this port's scope.
 */
public final class IndexBuilder {

  private IndexBuilder() {}

  /**
   * @param entries a flat list, already in document order
   * @param endOfDocument the document's final page — used as the last entry's {@code endIndex}
   */
  public static List<IndexNode> build(List<TocEntry> entries, int endOfDocument) {
    if (entries.isEmpty()) {
      return List.of();
    }

    // Rule 1: derive each entry's endIndex from the NEXT entry's physicalIndex and
    // appearsAtPageStart, exactly as pageindex/utils.py:571-581 does over the flat list
    // before any hierarchy is built.
    int[] endIndex = new int[entries.size()];
    for (int i = 0; i < entries.size(); i++) {
      if (i < entries.size() - 1) {
        TocEntry next = entries.get(i + 1);
        endIndex[i] = next.appearsAtPageStart() ? next.physicalIndex() - 1 : next.physicalIndex();
      } else {
        endIndex[i] = endOfDocument;
      }
    }

    // Rule 3: place each entry under its dotted-path parent; an entry whose parent path is
    // not present in this list becomes a root, mirroring list_to_tree's fallback.
    Map<String, IndexNode> byPath = new LinkedHashMap<>();
    List<IndexNode> roots = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      TocEntry entry = entries.get(i);
      IndexNode node = new IndexNode(entry.title(), entry.physicalIndex(), endIndex[i]);
      byPath.put(entry.structure(), node);
      String parentPath = parentOf(entry.structure());
      IndexNode parent = parentPath == null ? null : byPath.get(parentPath);
      if (parent != null) {
        parent.children().add(node);
      } else {
        roots.add(node);
      }
    }
    return roots;
  }

  private static String parentOf(String structure) {
    if (structure == null || structure.isEmpty()) {
      return null;
    }
    int lastDot = structure.lastIndexOf('.');
    return lastDot < 0 ? null : structure.substring(0, lastDot);
  }
}
