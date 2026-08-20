package io.akka.pageindex.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * One node of a constructed or optimized index tree.
 *
 * <p>{@code endIndex} covers only this node's own span among its siblings at construction
 * time — it is NOT widened to its subtree's true extent when children attach (see
 * {@code docs/question-log.md} row 1 / SPEC-001 rule 2). Call {@link TreeOptimizer#subtreeEnd}
 * for the subtree's real last page.
 *
 * <p>Mutable by design: {@link TreeOptimizer#merge} collapses a node in place — clearing
 * {@code children}, rewriting {@code endIndex}, and recording every absorbed title in
 * {@code keyItems} — the same in-place rewrite the source performs on its dict tree.
 */
public final class IndexNode {
  private String title;
  private int startIndex;
  private int endIndex;
  private String nodeId;
  private final List<IndexNode> children = new ArrayList<>();
  private List<String> keyItems = List.of();

  public IndexNode(String title, int startIndex, int endIndex) {
    this.title = title;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  public String title() { return title; }
  public void setTitle(String title) { this.title = title; }
  public int startIndex() { return startIndex; }
  public int endIndex() { return endIndex; }
  public void setEndIndex(int endIndex) { this.endIndex = endIndex; }
  public String nodeId() { return nodeId; }
  public void setNodeId(String nodeId) { this.nodeId = nodeId; }
  public List<IndexNode> children() { return children; }
  public boolean isFrontier() { return children.isEmpty(); }
  public List<String> keyItems() { return keyItems; }
  public void setKeyItems(List<String> keyItems) { this.keyItems = List.copyOf(keyItems); }
  public void clearChildren() { children.clear(); }
}
