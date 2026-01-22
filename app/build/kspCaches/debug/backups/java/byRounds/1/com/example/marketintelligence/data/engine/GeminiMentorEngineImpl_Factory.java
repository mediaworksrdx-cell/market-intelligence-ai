package com.example.marketintelligence.data.engine;

import com.example.marketintelligence.data.source.remote.GeminiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class GeminiMentorEngineImpl_Factory implements Factory<GeminiMentorEngineImpl> {
  private final Provider<GeminiService> geminiServiceProvider;

  public GeminiMentorEngineImpl_Factory(Provider<GeminiService> geminiServiceProvider) {
    this.geminiServiceProvider = geminiServiceProvider;
  }

  @Override
  public GeminiMentorEngineImpl get() {
    return newInstance(geminiServiceProvider.get());
  }

  public static GeminiMentorEngineImpl_Factory create(
      Provider<GeminiService> geminiServiceProvider) {
    return new GeminiMentorEngineImpl_Factory(geminiServiceProvider);
  }

  public static GeminiMentorEngineImpl newInstance(GeminiService geminiService) {
    return new GeminiMentorEngineImpl(geminiService);
  }
}
