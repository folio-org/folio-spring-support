package org.folio.tenant.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

/**
 * JPA entity representing a tenant setting.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "setting")
public class SettingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @Column(name = "key", nullable = false, length = 100)
  private String key;

  @Column(name = "value", nullable = false)
  private String value;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 50)
  private SettingType type;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "group_id", nullable = false, length = 100)
  private String groupId;

  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @Column(name = "created_by_user_id")
  private UUID createdByUserId;

  @Column(name = "updated_date")
  private OffsetDateTime updatedDate;

  @Column(name = "updated_by_user_id")
  private UUID updatedByUserId;

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy hibernateProxy
           ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
           : getClass().hashCode();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    var effectiveClass = o instanceof HibernateProxy hibernateProxy
                         ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                         : o.getClass();
    var thisEffectiveClass = this instanceof HibernateProxy hibernateProxy
                             ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                             : this.getClass();
    if (thisEffectiveClass != effectiveClass) {
      return false;
    }
    SettingEntity that = (SettingEntity) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  /**
   * Enumeration of supported setting value types.
   */
  public enum SettingType {
    BOOLEAN, INTEGER, STRING
  }
}
