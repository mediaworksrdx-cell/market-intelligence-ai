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
public final class PositionConstructionEngine_Factory implements Factory<PositionConstructionEngine> {
  @Override
  public PositionConstructionEngine get() {
    return newInstance();
  }

  public static PositionConstructionEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PositionConstructionEngine newInstance() {
    return new PositionConstructionEngine();
  }

  private static final class InstanceHolder {
    private static final PositionConstructionEngine_Factory INSTANCE = new PositionConstructionEngine_Factory();
  }
}
