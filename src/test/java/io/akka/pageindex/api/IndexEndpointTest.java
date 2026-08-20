package io.akka.pageindex.api;

import akka.javasdk.testkit.TestKitSupport;
import io.akka.pageindex.domain.RetrievalStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPEC-001 §3 rules 1-2, 5, 8 — driven through the real HTTP surface, so
 * {@link IndexEndpoint}'s wire shapes are exercised, not just the application classes behind it.
 */
public class IndexEndpointTest extends TestKitSupport {

  @Test
  void buildKeepsFlatEndIndexWithoutOptimizing() {
    var request = new IndexEndpoint.BuildRequest(
        List.of(
            new IndexEndpoint.TocEntryBody("1", "Intro", 1, true),
            new IndexEndpoint.TocEntryBody("1.1", "Background", 1, false),
            new IndexEndpoint.TocEntryBody("1.2", "Motivation", 3, true),
            new IndexEndpoint.TocEntryBody("2", "Method", 5, true)),
        10);

    var response = httpClient
        .POST("/index/build")
        .withRequestBody(request)
        .responseBodyAs(IndexEndpoint.TreeResponse.class)
        .invoke()
        .body();

    IndexEndpoint.NodeView intro = response.tree().get(0);
    assertThat(intro.title()).isEqualTo("Intro");
    assertThat(intro.endIndex()).isEqualTo(1); // not widened to children
    assertThat(intro.subtreeEnd()).isEqualTo(4);
  }

  @Test
  void buildOptimizedCollapsesATiedSubtree() {
    var request = new IndexEndpoint.BuildRequest(
        List.of(
            new IndexEndpoint.TocEntryBody("1", "A", 1, true),
            new IndexEndpoint.TocEntryBody("1.1", "A.1", 1, true),
            new IndexEndpoint.TocEntryBody("1.2", "A.2", 3, true),
            new IndexEndpoint.TocEntryBody("2", "B", 7, true),
            new IndexEndpoint.TocEntryBody("2.1", "B.1", 7, true),
            new IndexEndpoint.TocEntryBody("2.2", "B.2", 8, true)),
        8);

    var response = httpClient
        .POST("/index/build-optimized")
        .withRequestBody(request)
        .responseBodyAs(IndexEndpoint.TreeResponse.class)
        .invoke()
        .body();

    IndexEndpoint.NodeView a = response.tree().get(0);
    assertThat(a.children()).hasSize(2); // kept

    IndexEndpoint.NodeView b = response.tree().get(1);
    assertThat(b.children()).isEmpty(); // collapsed
    assertThat(b.keyItems()).containsExactly("B.1", "B.2");
  }

  @Test
  void expandAndFormatPagesRoundTrip() {
    var expanded = httpClient
        .GET("/index/pages/expand/1-3,7,9-12")
        .responseBodyAs(IndexEndpoint.PageRangeResponse.class)
        .invoke()
        .body();
    assertThat(expanded.pages()).containsExactly(1, 2, 3, 7, 9, 10, 11, 12);

    var formatted = httpClient
        .POST("/index/pages/format")
        .withRequestBody(new IndexEndpoint.FormatRequest(expanded.pages()))
        .responseBodyAs(IndexEndpoint.FormatResponse.class)
        .invoke()
        .body();
    assertThat(formatted.spec()).isEqualTo("1-3,7,9-12");
  }

  @Test
  void retrievalStrategyFollowsPageCountBands() {
    var five = httpClient
        .GET("/index/retrieval-strategy/5")
        .responseBodyAs(IndexEndpoint.StrategyResponse.class)
        .invoke()
        .body();
    assertThat(five.strategy()).isEqualTo(RetrievalStrategy.FETCH_ALL);

    var twentyOne = httpClient
        .GET("/index/retrieval-strategy/21")
        .responseBodyAs(IndexEndpoint.StrategyResponse.class)
        .invoke()
        .body();
    assertThat(twentyOne.strategy()).isEqualTo(RetrievalStrategy.STRUCTURE_THEN_TARGETED_RANGE);
  }
}
