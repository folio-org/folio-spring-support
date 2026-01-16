package org.folio.tenant.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

/**
 * JPA entity representing a group of tenant settings.
 * Settings are organized into groups for better categorization.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "setting_group")
public class SettingGroupEntity {

  @Id
  @Column(name = "id", nullable = false, length = 100)
  private String id;

  @Column(name = "name", nullable = false, length = 250)
  private String name;

  @Column(name = "description", length = 1000)
  private String description;

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
    var that = (SettingGroupEntity) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }
}
