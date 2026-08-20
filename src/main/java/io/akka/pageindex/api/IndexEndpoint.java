package io.akka.pageindex.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.http.AbstractHttpEndpoint;
import io.akka.pageindex.application.IndexBuilder;
import io.akka.pageindex.application.PageRange;
import io.akka.pageindex.application.RetrievalPlanner;
import io.akka.pageindex.application.TreeOptimizer;
import io.akka.pageindex.domain.IndexNode;
import io.akka.pageindex.domain.RetrievalStrategy;
import io.akka.pageindex.domain.TocEntry;

import java.util.List;

/**
 * Index construction over a flat table of contents, and the retrieval decision on top —
 * see {@code specs/SPEC-001-pageindex.md}.
 *
 * <p>Opened up for access from the public internet to make this port easy to try out; a
 * production service would scope this more tightly (see {@code akka-sdk} access-control docs).
 */
@HttpEndpoint("/index")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class IndexEndpoint extends AbstractHttpEndpoint {

  public record TocEntryBody(String structure, String title, int physicalIndex, boolean appearsAtPageStart) {
    TocEntry toDomain() {
      return new TocEntry(structure, title, physicalIndex, appearsAtPageStart);
    }
  }

  public record BuildRequest(List<TocEntryBody> entries, int endOfDocument) {}

  public record NodeView(String title, int startIndex, int endIndex, int subtreeEnd,
                          List<String> keyItems, List<NodeView> children) {
    static NodeView of(IndexNode node) {
      return new NodeView(
          node.title(),
          node.startIndex(),
          node.endIndex(),
          TreeOptimizer.subtreeEnd(node),
          node.keyItems(),
          node.children().stream().map(NodeView::of).toList());
    }
  }

  public record TreeResponse(List<NodeView> tree) {}

  /** Build the raw hierarchy from a flat TOC list (SPEC-001 rules 1-3). No optimization. */
  @Post("/build")
  public TreeResponse build(BuildRequest request) {
    List<TocEntry> entries = request.entries().stream().map(TocEntryBody::toDomain).toList();
    List<IndexNode> tree = IndexBuilder.build(entries, request.endOfDocument());
    return new TreeResponse(tree.stream().map(NodeView::of).toList());
  }

  /** Build, then collapse every subtree that does not beat a linear scan (SPEC-001 rules 4-5). */
  @Post("/build-optimized")
  public TreeResponse buildOptimized(BuildRequest request) {
    List<TocEntry> entries = request.entries().stream().map(TocEntryBody::toDomain).toList();
    List<IndexNode> tree = IndexBuilder.build(entries, request.endOfDocument());
    TreeOptimizer.merge(tree);
    return new TreeResponse(tree.stream().map(NodeView::of).toList());
  }

  public record PageRangeResponse(List<Integer> pages) {}

  /** Expand a page spec like {@code "1-3,7,9-12"} into its distinct page numbers (rule 6). */
  @Get("/pages/expand/{spec}")
  public PageRangeResponse expandPages(String spec) {
    return new PageRangeResponse(PageRange.expand(spec));
  }

  public record FormatRequest(List<Integer> pages) {}
  public record FormatResponse(String spec) {}

  /** Compress a page list back into range notation (rule 7). */
  @Post("/pages/format")
  public FormatResponse formatPages(FormatRequest request) {
    return new FormatResponse(PageRange.format(request.pages()));
  }

  public record StrategyResponse(RetrievalStrategy strategy) {}

  /** The retrieval decision: what to fetch, given only a page count (rule 8). */
  @Get("/retrieval-strategy/{pageCount}")
  public StrategyResponse retrievalStrategy(int pageCount) {
    return new StrategyResponse(RetrievalPlanner.plan(pageCount));
  }
}
