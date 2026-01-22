package com.example.marketintelligence.domain.engine;

import com.example.marketintelligence.data.engine.GeminiMentorEngineImpl;
import com.example.marketintelligence.data.engine.ProprietaryMentorEngineImpl;
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
public final class MentorEngineFactory_Factory implements Factory<MentorEngineFactory> {
  private final Provider<GeminiMentorEngineImpl> geminiEngineProvider;

  private final Provider<ProprietaryMentorEngineImpl> proprietaryEngineProvider;

  public MentorEngineFactory_Factory(Provider<GeminiMentorEngineImpl> geminiEngineProvider,
      Provider<ProprietaryMentorEngineImpl> proprietaryEngineProvider) {
    this.geminiEngineProvider = geminiEngineProvider;
    this.proprietaryEngineProvider = proprietaryEngineProvider;
  }

  @Override
  public MentorEngineFactory get() {
    return newInstance(geminiEngineProvider.get(), proprietaryEngineProvider.get());
  }

  public static MentorEngineFactory_Factory create(
      Provider<GeminiMentorEngineImpl> geminiEngineProvider,
      Provider<ProprietaryMentorEngineImpl> proprietaryEngineProvider) {
    return new MentorEngineFactory_Factory(geminiEngineProvider, proprietaryEngineProvider);
  }

  public static MentorEngineFactory newInstance(GeminiMentorEngineImpl geminiEngine,
      ProprietaryMentorEngineImpl proprietaryEngine) {
    return new MentorEngineFactory(geminiEngine, proprietaryEngine);
  }
}
