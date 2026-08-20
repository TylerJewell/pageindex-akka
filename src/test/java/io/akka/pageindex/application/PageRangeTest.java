package io.akka.pageindex.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PageRangeTest {

  @Test
  void expandsRangesCommasAndSingles() {
    assertThat(PageRange.expand("1-3,7,9-12"))
        .containsExactly(1, 2, 3, 7, 9, 10, 11, 12);
  }

  @Test
  void formatsConsecutiveRunsAndSingletons() {
    assertThat(PageRange.format(List.of(1, 2, 3, 5, 7, 8, 9))).isEqualTo("1-3,5,7-9");
  }

  @Test
  void rejectsOversizedRangeBeforeExpanding() {
    // The size cap after the loop (belt-and-suspenders on the accumulated set) would
    // eventually reject "1-99999999999" too, but only after materializing billions of
    // entries first — assertTimeoutPreemptively is what actually distinguishes "rejected
    // by the pre-check" from "the pre-check is gone and this hangs instead", since a
    // mutant that removes only the pre-check still throws the identical message, just
    // nowhere near in time.
    assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
        assertThatThrownBy(() -> PageRange.expand("1-99999999999"))
            .isInstanceOf(PageRange.PageRangeException.class)
            .hasMessageContaining("spans more than 10000 pages"));
  }

  @Test
  void rejectsPageCountJustOverTheLimit() {
    assertThatThrownBy(() -> PageRange.expand("1-10001"))
        .isInstanceOf(PageRange.PageRangeException.class)
        .hasMessageContaining("spans more than 10000 pages");
  }

  @Test
  void rejectsStartGreaterThanEnd() {
    assertThatThrownBy(() -> PageRange.expand("10-5"))
        .isInstanceOf(PageRange.PageRangeException.class);
  }

  @Test
  void rejectsNonPositivePages() {
    assertThatThrownBy(() -> PageRange.expand("0-2"))
        .isInstanceOf(PageRange.PageRangeException.class);
  }

  @Test
  void rejectsBlankSpec() {
    assertThatThrownBy(() -> PageRange.expand("  "))
        .isInstanceOf(PageRange.PageRangeException.class);
  }

  @Test
  void roundTripsExpandThenFormat() {
    String spec = "1-3,5,7-9";
    assertThat(PageRange.format(PageRange.expand(spec))).isEqualTo(spec);
  }
}
