package org.folio.spring.testing.extension.impl;

import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.lang.reflect.Parameter;
import org.folio.spring.testing.extension.Random;
import org.folio.spring.testing.extension.RandomInt;
import org.folio.spring.testing.extension.RandomLong;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension for resolving parameters annotated with 'Random', 'RandomInt', or 'RandomLong'
 * by generating appropriate random values.
 */
public class RandomParametersExtension implements ParameterResolver {

  private final EasyRandom easyRandom;

  /**
   * Create the extension with a default {@link EasyRandom}.
   */
  public RandomParametersExtension() {
    this(new EasyRandomParameters());
  }

  /**
   * Create the extension with the given {@link EasyRandomParameters}. This is used, instead of the zero-arg alternative
   * , when the caller wants to override the default 'randomizer' configuration. This constructor will be called
   * by using the {@code RegisterExtension} annotation.
   *
   * @param easyRandomParameters parameters of an EasyRandom instance
   */
  public RandomParametersExtension(EasyRandomParameters easyRandomParameters) {
    easyRandom = new EasyRandom(easyRandomParameters);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(RandomInt.class)
      || parameterContext.isAnnotated(RandomLong.class)
      || parameterContext.isAnnotated(Random.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getRandomValue(parameterContext.getParameter());
  }

  private Object getRandomValue(Parameter parameter) {

    if (isAnnotated(parameter, RandomInt.class)) {
      RandomInt ri = parameter.getAnnotation(RandomInt.class);

      return new IntegerRangeRandomizer(ri.min(), ri.max()).getRandomValue();
    } else if (isAnnotated(parameter, RandomLong.class)) {
      RandomLong rl = parameter.getAnnotation(RandomLong.class);

      return new LongRangeRandomizer(rl.min(), rl.max()).getRandomValue();
    } else if (isAnnotated(parameter, Random.class)) {
      Class<?> targetType = parameter.getType();

      return easyRandom.nextObject(targetType);
    } else {
      throw new ParameterResolutionException("No random generator implemented for " + parameter.getName());
    }
  }
}
