package org.folio.tenant.settings.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SettingEntityTest {

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
    var id = UUID.randomUUID();
    var entity1 = createEntity();
    entity1.setId(id);

    var entity2 = createEntity();
    entity2.setId(id);

    assertThat(entity1.equals(entity2)).isTrue();
  }

  @Test
  void equals_shouldReturnFalseForDifferentIds() {
    var entity1 = createEntity();
    entity1.setId(UUID.randomUUID());

    var entity2 = createEntity();
    entity2.setId(UUID.randomUUID());

    assertThat(entity1.equals(entity2)).isFalse();
  }

  @Test
  void equals_shouldReturnFalseWhenIdIsNull() {
    var entity1 = createEntity();
    entity1.setId(null);

    var entity2 = createEntity();
    entity2.setId(UUID.randomUUID());

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
    var id = UUID.randomUUID();
    var entity1 = createEntity();
    entity1.setId(id);

    var entity2 = createEntity();
    entity2.setId(id);

    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  @Test
  void hashCode_shouldBeBasedOnClass() {
    var entity1 = createEntity();
    var entity2 = createEntity();

    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  private SettingEntity createEntity() {
    var entity = new SettingEntity();
    entity.setKey("test.key");
    entity.setValue("test-value");
    entity.setType(SettingEntity.SettingType.STRING);
    entity.setGroupId("test-group");
    return entity;
  }
}
