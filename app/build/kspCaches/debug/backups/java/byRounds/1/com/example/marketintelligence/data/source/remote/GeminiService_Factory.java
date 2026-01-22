package com.example.marketintelligence.data.source.remote;

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
public final class GeminiService_Factory implements Factory<GeminiService> {
  @Override
  public GeminiService get() {
    return newInstance();
  }

  public static GeminiService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GeminiService newInstance() {
    return new GeminiService();
  }

  private static final class InstanceHolder {
    private static final GeminiService_Factory INSTANCE = new GeminiService_Factory();
  }
}
