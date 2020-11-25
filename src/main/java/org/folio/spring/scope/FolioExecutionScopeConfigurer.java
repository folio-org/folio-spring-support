package org.folio.spring.scope;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getConversationIdForScope;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getFolioExecutionScope;

public class FolioExecutionScopeConfigurer implements Scope {

  @Override
  public Object get(String name, ObjectFactory<?> objectFactory) {
    Map<String, Object> folioExecutionScope = getFolioExecutionScope();
    Object bean = folioExecutionScope.get(name);
    if (bean == null) {
      folioExecutionScope.put(name, bean = objectFactory.getObject());
    }

    return bean;
  }

  @Override
  public Object remove(String name) {
    return getFolioExecutionScope().remove(name);
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {

  }

  @Override
  public Object resolveContextualObject(String key) {
    return null;
  }

  @Override
  public String getConversationId() {
    return getConversationIdForScope();
  }
}
