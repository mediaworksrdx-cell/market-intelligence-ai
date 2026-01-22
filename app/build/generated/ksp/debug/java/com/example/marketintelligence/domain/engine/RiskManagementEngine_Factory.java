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
public final class RiskManagementEngine_Factory implements Factory<RiskManagementEngine> {
  @Override
  public RiskManagementEngine get() {
    return newInstance();
  }

  public static RiskManagementEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RiskManagementEngine newInstance() {
    return new RiskManagementEngine();
  }

  private static final class InstanceHolder {
    private static final RiskManagementEngine_Factory INSTANCE = new RiskManagementEngine_Factory();
  }
}
