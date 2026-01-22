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
public final class LiveIntelligenceBus_Factory implements Factory<LiveIntelligenceBus> {
  @Override
  public LiveIntelligenceBus get() {
    return newInstance();
  }

  public static LiveIntelligenceBus_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LiveIntelligenceBus newInstance() {
    return new LiveIntelligenceBus();
  }

  private static final class InstanceHolder {
    private static final LiveIntelligenceBus_Factory INSTANCE = new LiveIntelligenceBus_Factory();
  }
}
