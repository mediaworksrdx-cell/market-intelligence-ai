package com.example.marketintelligence.data.source.remote;

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
public final class RealTimeDataService_Factory implements Factory<RealTimeDataService> {
  @Override
  public RealTimeDataService get() {
    return newInstance();
  }

  public static RealTimeDataService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RealTimeDataService newInstance() {
    return new RealTimeDataService();
  }

  private static final class InstanceHolder {
    private static final RealTimeDataService_Factory INSTANCE = new RealTimeDataService_Factory();
  }
}
