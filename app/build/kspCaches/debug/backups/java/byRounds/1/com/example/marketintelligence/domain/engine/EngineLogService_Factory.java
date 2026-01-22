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
public final class EngineLogService_Factory implements Factory<EngineLogService> {
  @Override
  public EngineLogService get() {
    return newInstance();
  }

  public static EngineLogService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EngineLogService newInstance() {
    return new EngineLogService();
  }

  private static final class InstanceHolder {
    private static final EngineLogService_Factory INSTANCE = new EngineLogService_Factory();
  }
}
