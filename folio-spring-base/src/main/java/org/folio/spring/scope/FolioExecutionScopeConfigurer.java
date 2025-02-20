package org.folio.spring.scope;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getConversationIdForScope;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getFolioExecutionScope;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

@Log4j2
public class FolioExecutionScopeConfigurer implements Scope {

  @Override
  public Object get(String name, ObjectFactory<?> objectFactory) {
    return getFolioExecutionScope()
      .computeIfAbsent(name, k -> objectFactory.getObject());
  }

  @Override
  public Object remove(String name) {
    return getFolioExecutionScope().remove(name);
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    log.debug("Ignore destruction callback for {}", name);
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
