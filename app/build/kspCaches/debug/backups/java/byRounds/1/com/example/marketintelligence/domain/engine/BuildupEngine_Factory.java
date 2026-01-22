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
public final class BuildupEngine_Factory implements Factory<BuildupEngine> {
  @Override
  public BuildupEngine get() {
    return newInstance();
  }

  public static BuildupEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BuildupEngine newInstance() {
    return new BuildupEngine();
  }

  private static final class InstanceHolder {
    private static final BuildupEngine_Factory INSTANCE = new BuildupEngine_Factory();
  }
}
