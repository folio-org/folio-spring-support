package org.folio.junit.extension;

import java.lang.reflect.Parameter;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RandomParametersExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(RandomInt.class) ||
        parameterContext.isAnnotated(RandomLong.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getRandomValue(parameterContext.getParameter());
  }

  private Object getRandomValue(Parameter parameter) {
    if (parameter.getAnnotation(RandomInt.class) != null) {
      RandomInt ri = parameter.getAnnotation(RandomInt.class);

      return (int) random(ri.min(), ri.max());
    } else if (parameter.getAnnotation(RandomLong.class) != null) {
      RandomLong rl = parameter.getAnnotation(RandomLong.class);

      return (long) random(rl.min(), rl.max());
    } else {
      throw new ParameterResolutionException("No random generator implemented for " + parameter.getName());
    }
  }

  private static double random(double min, double max) {
    return Math.random() * ((max - min)) + min;
  }
}