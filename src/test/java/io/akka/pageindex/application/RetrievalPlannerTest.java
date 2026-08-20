package io.akka.pageindex.application;

import io.akka.pageindex.domain.RetrievalStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalPlannerTest {

  @Test
  void bandBoundariesMatchSourceThresholds() {
    assertThat(RetrievalPlanner.plan(1)).isEqualTo(RetrievalStrategy.FETCH_ALL);
    assertThat(RetrievalPlanner.plan(5)).isEqualTo(RetrievalStrategy.FETCH_ALL);
    assertThat(RetrievalPlanner.plan(6)).isEqualTo(RetrievalStrategy.STRUCTURE_THEN_KEY_PAGES);
    assertThat(RetrievalPlanner.plan(20)).isEqualTo(RetrievalStrategy.STRUCTURE_THEN_KEY_PAGES);
    assertThat(RetrievalPlanner.plan(21)).isEqualTo(RetrievalStrategy.STRUCTURE_THEN_TARGETED_RANGE);
    assertThat(RetrievalPlanner.plan(500)).isEqualTo(RetrievalStrategy.STRUCTURE_THEN_TARGETED_RANGE);
  }
}
