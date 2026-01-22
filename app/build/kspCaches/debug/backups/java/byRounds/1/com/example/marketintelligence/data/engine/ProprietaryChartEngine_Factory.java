package com.example.marketintelligence.data.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class ProprietaryChartEngine_Factory implements Factory<ProprietaryChartEngine> {
  @Override
  public ProprietaryChartEngine get() {
    return newInstance();
  }

  public static ProprietaryChartEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProprietaryChartEngine newInstance() {
    return new ProprietaryChartEngine();
  }

  private static final class InstanceHolder {
    private static final ProprietaryChartEngine_Factory INSTANCE = new ProprietaryChartEngine_Factory();
  }
}
