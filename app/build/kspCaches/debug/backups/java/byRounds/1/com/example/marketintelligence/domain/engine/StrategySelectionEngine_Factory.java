package com.example.marketintelligence.domain.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class StrategySelectionEngine_Factory implements Factory<StrategySelectionEngine> {
  @Override
  public StrategySelectionEngine get() {
    return newInstance();
  }

  public static StrategySelectionEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static StrategySelectionEngine newInstance() {
    return new StrategySelectionEngine();
  }

  private static final class InstanceHolder {
    private static final StrategySelectionEngine_Factory INSTANCE = new StrategySelectionEngine_Factory();
  }
}
