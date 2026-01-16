package org.folio.tenant.settings.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SettingGroupEntityTest {

  @Test
  void equals_shouldReturnTrueForSameObject() {
    var entity = createEntity();

    assertThat(entity.equals(entity)).isTrue();
  }

  @Test
  void equals_shouldReturnFalseForNull() {
    var entity = createEntity();

    assertThat(entity.equals(null)).isFalse();
  }

  @Test
  void equals_shouldReturnFalseForDifferentClass() {
    var entity = createEntity();
    var other = new Object();

    assertThat(entity.equals(other)).isFalse();
  }

  @Test
  void equals_shouldReturnTrueForSameId() {
    var entity1 = createEntity();
    entity1.setId("authorities");

    var entity2 = createEntity();
    entity2.setId("authorities");

    assertThat(entity1.equals(entity2)).isTrue();
  }

  @Test
  void equals_shouldReturnFalseForDifferentIds() {
    var entity1 = createEntity();
    entity1.setId("authorities");

    var entity2 = createEntity();
    entity2.setId("audit.authority");

    assertThat(entity1.equals(entity2)).isFalse();
  }

  @Test
  void equals_shouldReturnFalseWhenIdIsNull() {
    var entity1 = createEntity();
    entity1.setId(null);

    var entity2 = createEntity();
    entity2.setId("authorities");

    assertThat(entity1.equals(entity2)).isFalse();
  }

  @Test
  void equals_shouldReturnFalseWhenBothIdsAreNull() {
    var entity1 = createEntity();
    entity1.setId(null);

    var entity2 = createEntity();
    entity2.setId(null);

    assertThat(entity1.equals(entity2)).isFalse();
  }

  @Test
  void hashCode_shouldReturnConsistentValue() {
    var entity = createEntity();

    int hashCode1 = entity.hashCode();
    int hashCode2 = entity.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  void hashCode_shouldBeSameForObjectsWithSameId() {
    var entity1 = createEntity();
    entity1.setId("authorities");

    var entity2 = createEntity();
    entity2.setId("authorities");

    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  @Test
  void hashCode_shouldBeBasedOnClass() {
    var entity1 = createEntity();
    var entity2 = createEntity();

    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  private SettingGroupEntity createEntity() {
    var entity = new SettingGroupEntity();
    entity.setName("Test Group");
    return entity;
  }
}
