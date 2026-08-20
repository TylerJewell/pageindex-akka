package io.akka.pageindex.application;

import io.akka.pageindex.domain.IndexNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TreeOptimizerTest {

  @Test
  void subtreeEndDoesNotMutate() {
    IndexNode parent = new IndexNode("A", 1, 6);
    IndexNode child = new IndexNode("A.1", 1, 2);
    parent.children().add(child);

    int before = parent.endIndex();
    int result = TreeOptimizer.subtreeEnd(parent);
    assertThat(result).isEqualTo(6); // parent's own endIndex already reaches 6
    assertThat(parent.endIndex()).isEqualTo(before); // unchanged by the read

    // Calling it twice gives the same answer and still doesn't mutate.
    assertThat(TreeOptimizer.subtreeEnd(parent)).isEqualTo(result);
    assertThat(parent.endIndex()).isEqualTo(before);
  }

  // The exact two-section tree from docs/question-log.md row 3, reproducing the Python run
  // against pageindex.tree_optimize.merge_tree: section A (pages 1-6, children 1-2 and 3-6)
  // must keep its children; section B (pages 7-8, children 7-7 and 8-8) must collapse.
  private static List<IndexNode> sourceRowThreeTree() {
    IndexNode a = new IndexNode("A", 1, 6);
    a.children().add(new IndexNode("A.1", 1, 2));
    a.children().add(new IndexNode("A.2", 3, 6));

    IndexNode b = new IndexNode("B", 7, 8);
    b.children().add(new IndexNode("B.1", 7, 7));
    b.children().add(new IndexNode("B.2", 8, 8));

    List<IndexNode> tree = new ArrayList<>();
    tree.add(a);
    tree.add(b);
    return tree;
  }

  @Test
  void mergeCollapsesAtTieKeepsLargerSubtree() {
    List<IndexNode> tree = sourceRowThreeTree();

    boolean changed = TreeOptimizer.merge(tree);

    assertThat(changed).isTrue();
    IndexNode a = tree.get(0);
    assertThat(a.isFrontier()).isFalse();
    assertThat(a.children()).hasSize(2);

    IndexNode b = tree.get(1);
    assertThat(b.isFrontier()).isTrue(); // collapsed
    assertThat(b.endIndex()).isEqualTo(8);
    assertThat(b.keyItems()).containsExactly("B.1", "B.2");
  }

  @Test
  void treeCostMatchesDocumentedFormula() {
    List<IndexNode> tree = sourceRowThreeTree();
    IndexNode a = tree.get(0);
    IndexNode b = tree.get(1);

    // A: S=6, tree_cost = 1 + max(residual=0, max(child scans)=4) = 5 -> 6 > 5, keep.
    assertThat(TreeOptimizer.scanPages(a)).isEqualTo(6);
    assertThat(TreeOptimizer.treeCost(a)).isEqualTo(5);

    // B: S=2, tree_cost = 1 + max(residual=0, max(child scans)=1) = 2 -> 2 <= 2, collapse.
    assertThat(TreeOptimizer.scanPages(b)).isEqualTo(2);
    assertThat(TreeOptimizer.treeCost(b)).isEqualTo(2);
  }
}
