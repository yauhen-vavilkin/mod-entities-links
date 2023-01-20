package org.folio.entlinks.utils;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import lombok.Value;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ObjectUtilsTest {

  @Test
  @SneakyThrows
  void getDifference_positive_noDifference() {
    var t1 = new TestClass("b1", "b2");
    var t2 = new TestClass("b1", "b2");

    var actual = ObjectUtils.getDifference(t1, t2);

    assertThat(actual).isEmpty();
  }

  @Test
  @SneakyThrows
  void getDifference_positive_oneFieldDifferent() {
    var t1 = new TestClass("b1", "b2");
    var t2 = new TestClass("c1", "b2");

    var actual = ObjectUtils.getDifference(t1, t2);

    assertThat(actual).containsExactly(new ObjectUtils.Difference("A1", "b1", "c1"));
  }

  @Test
  @SneakyThrows
  void getDifference_positive_allFieldsDifferent() {
    var t1 = new TestClass("b1", "b2");
    var t2 = new TestClass("c1", "c2");

    var actual = ObjectUtils.getDifference(t1, t2);

    assertThat(actual).containsExactlyInAnyOrder(
      new ObjectUtils.Difference("A1", "b1", "c1"),
      new ObjectUtils.Difference("A2", "b2", "c2"));
  }

  @Value
  static class TestClass {

    String a1;
    String a2;
  }
}
