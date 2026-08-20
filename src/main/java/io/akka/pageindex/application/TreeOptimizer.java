package io.akka.pageindex.application;

import io.akka.pageindex.domain.IndexNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The deterministic, no-LLM half of PageIndex's tree optimization: collapse a subtree
 * whenever navigating its structure costs no less than scanning it linearly.
 *
 * <p>Ported from {@code pageindex/tree_optimize.py}'s {@code S}, {@code S_residual},
 * {@code tree_cost} and {@code merge} (SPEC-001 rules 4-5). The {@code expand} half — which
 * proposes brand-new subsections via an LLM — is out of scope.
 */
public final class TreeOptimizer {

  /** R(v): the fixed cost, in pages, of routing through one node. Matches the source's
   * {@code ROUTING_COST = 1} (tree_optimize.py:68) — see SPEC-001 §4. */
  public static final int ROUTING_COST = 1;

  private TreeOptimizer() {}

  /** Last page covered by this node or any descendant — a pure read, never mutates. */
  public static int subtreeEnd(IndexNode node) {
    int end = node.endIndex();
    for (IndexNode child : node.children()) {
      end = Math.max(end, subtreeEnd(child));
    }
    return end;
  }

  /** Pages to scan linearly if this node were collapsed. */
  public static int scanPages(IndexNode node) {
    return subtreeEnd(node) - node.startIndex() + 1;
  }

  /** Pages of the node's own span covered by no child. */
  public static int residualPages(IndexNode node) {
    if (node.children().isEmpty()) {
      return scanPages(node);
    }
    boolean[] covered = new boolean[subtreeEnd(node) - node.startIndex() + 1];
    for (IndexNode child : node.children()) {
      for (int p = child.startIndex(); p <= subtreeEnd(child); p++) {
        int offset = p - node.startIndex();
        if (offset >= 0 && offset < covered.length) {
          covered[offset] = true;
        }
      }
    }
    int uncovered = 0;
    for (boolean c : covered) {
      if (!c) uncovered++;
    }
    return uncovered;
  }

  /** Worst-case search cost of the subtree as it currently stands. */
  public static int treeCost(IndexNode node) {
    if (node.isFrontier()) {
      return scanPages(node);
    }
    int worstChild = 0;
    for (IndexNode child : node.children()) {
      worstChild = Math.max(worstChild, treeCost(child));
    }
    int residual = residualPages(node);
    return ROUTING_COST + Math.max(residual, worstChild);
  }

  /**
   * Collapse any subtree whose structure does not beat a linear scan, bottom-up. Mutates
   * the tree in place; returns whether anything changed (rule 5).
   */
  public static boolean merge(List<IndexNode> structure) {
    boolean[] changed = {false};
    for (IndexNode root : new ArrayList<>(structure)) {
      visit(root, changed);
    }
    return changed[0];
  }

  private static void visit(IndexNode node, boolean[] changed) {
    if (node.isFrontier()) {
      return;
    }
    for (IndexNode child : new ArrayList<>(node.children())) {
      visit(child, changed);
    }
    if (node.isFrontier()) { // every child collapsed away
      return;
    }

    int span = scanPages(node);
    int cost = treeCost(node);
    if (span <= cost) { // ties collapse
      List<String> titles = new ArrayList<>();
      collectTitles(node.children(), titles);
      node.setEndIndex(subtreeEnd(node));
      node.clearChildren();
      if (!titles.isEmpty()) {
        node.setKeyItems(titles);
      }
      changed[0] = true;
    }
  }

  private static void collectTitles(List<IndexNode> nodes, List<String> out) {
    for (IndexNode child : nodes) {
      out.add(child.title());
      out.addAll(child.keyItems());
      collectTitles(child.children(), out);
    }
  }

  /** Every frontier node's title, depth-first, document order — for tests and inspection. */
  public static List<String> frontierTitles(List<IndexNode> structure) {
    List<String> out = new ArrayList<>();
    for (IndexNode root : structure) {
      collectFrontier(root, out);
    }
    return out;
  }

  private static void collectFrontier(IndexNode node, List<String> out) {
    if (node.isFrontier()) {
      out.add(node.title());
      return;
    }
    for (IndexNode child : node.children()) {
      collectFrontier(child, out);
    }
  }

  /** Convenience: node titles, comma-joined, for logging/tests. */
  public static String describe(List<IndexNode> structure) {
    return structure.stream().map(IndexNode::title).collect(Collectors.joining(", "));
  }
}
