package io.akka.pageindex.application;

import io.akka.pageindex.domain.IndexNode;
import io.akka.pageindex.domain.TocEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexBuilderTest {

  // The exact synthetic TOC used in docs/question-log.md row 1, ported from the Python run
  // against pageindex.utils.post_processing.
  private static List<TocEntry> sourceRowOneEntries() {
    return List.of(
        new TocEntry("1", "Intro", 1, true),
        new TocEntry("1.1", "Background", 1, false),
        new TocEntry("1.2", "Motivation", 3, true),
        new TocEntry("2", "Method", 5, true));
  }

  @Test
  void endIndexFollowsNextEntryAppearsAtStart() {
    List<IndexNode> tree = IndexBuilder.build(sourceRowOneEntries(), 10);

    IndexNode intro = tree.get(0);
    assertThat(intro.title()).isEqualTo("Intro");
    assertThat(intro.startIndex()).isEqualTo(1);
    assertThat(intro.endIndex()).isEqualTo(1); // next entry starts its own page

    IndexNode background = intro.children().get(0);
    assertThat(background.endIndex()).isEqualTo(2); // Motivation shares page 3, so end = 3-1

    IndexNode motivation = intro.children().get(1);
    assertThat(motivation.endIndex()).isEqualTo(4); // Method(5) appears at start -> end = 5-1

    IndexNode method = tree.get(1);
    assertThat(method.endIndex()).isEqualTo(10); // last entry gets the document end page
  }

  @Test
  void parentEndIndexNotWidenedByChildren() {
    List<IndexNode> tree = IndexBuilder.build(sourceRowOneEntries(), 10);
    IndexNode intro = tree.get(0);

    // Intro's own span is just page 1, even though its children (Motivation) reach page 4.
    assertThat(intro.endIndex()).isEqualTo(1);
    assertThat(TreeOptimizer.subtreeEnd(intro)).isEqualTo(4);
  }

  @Test
  void dottedPathBuildsHierarchyOrphanBecomesRoot() {
    List<TocEntry> entries = List.of(
        new TocEntry("1", "Top", 1, true),
        new TocEntry("1.1", "Child", 2, true),
        new TocEntry("1.1.1", "Grandchild", 3, true),
        // "3.1" has no "3" ahead of it in the list -> falls back to a root, mirroring
        // list_to_tree's `else: root_nodes.append(node)` branch.
        new TocEntry("3.1", "Orphan", 4, true));

    List<IndexNode> tree = IndexBuilder.build(entries, 5);

    assertThat(tree).hasSize(2);
    IndexNode top = tree.get(0);
    assertThat(top.title()).isEqualTo("Top");
    IndexNode child = top.children().get(0);
    assertThat(child.title()).isEqualTo("Child");
    assertThat(child.children().get(0).title()).isEqualTo("Grandchild");
    assertThat(tree.get(1).title()).isEqualTo("Orphan");
  }

  @Test
  void singleEntryTreeUsesEndOfDocument() {
    List<IndexNode> tree = IndexBuilder.build(List.of(new TocEntry("1", "Only", 1, true)), 7);
    assertThat(tree.get(0).endIndex()).isEqualTo(7);
  }

  @Test
  void emptyEntriesReturnsEmptyTree() {
    assertThat(IndexBuilder.build(List.of(), 10)).isEmpty();
  }
}
