package com.example.marketintelligence.di;

import com.example.marketintelligence.data.source.remote.GeminiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideGeminiServiceFactory implements Factory<GeminiService> {
  @Override
  public GeminiService get() {
    return provideGeminiService();
  }

  public static AppModule_ProvideGeminiServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GeminiService provideGeminiService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGeminiService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideGeminiServiceFactory INSTANCE = new AppModule_ProvideGeminiServiceFactory();
  }
}
