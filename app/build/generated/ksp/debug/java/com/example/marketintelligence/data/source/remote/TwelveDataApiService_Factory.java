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
public final class TwelveDataApiService_Factory implements Factory<TwelveDataApiService> {
  @Override
  public TwelveDataApiService get() {
    return newInstance();
  }

  public static TwelveDataApiService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TwelveDataApiService newInstance() {
    return new TwelveDataApiService();
  }

  private static final class InstanceHolder {
    private static final TwelveDataApiService_Factory INSTANCE = new TwelveDataApiService_Factory();
  }
}
