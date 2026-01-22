package com.example.marketintelligence.data.engine;

import com.example.marketintelligence.data.source.remote.GeminiService;
import com.example.marketintelligence.domain.repository.MarketDataRepository;
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
public final class GeminiScannerEngineImpl_Factory implements Factory<GeminiScannerEngineImpl> {
  private final Provider<GeminiService> geminiServiceProvider;

  private final Provider<MarketDataRepository> marketDataRepositoryProvider;

  public GeminiScannerEngineImpl_Factory(Provider<GeminiService> geminiServiceProvider,
      Provider<MarketDataRepository> marketDataRepositoryProvider) {
    this.geminiServiceProvider = geminiServiceProvider;
    this.marketDataRepositoryProvider = marketDataRepositoryProvider;
  }

  @Override
  public GeminiScannerEngineImpl get() {
    return newInstance(geminiServiceProvider.get(), marketDataRepositoryProvider.get());
  }

  public static GeminiScannerEngineImpl_Factory create(
      Provider<GeminiService> geminiServiceProvider,
      Provider<MarketDataRepository> marketDataRepositoryProvider) {
    return new GeminiScannerEngineImpl_Factory(geminiServiceProvider, marketDataRepositoryProvider);
  }

  public static GeminiScannerEngineImpl newInstance(GeminiService geminiService,
      MarketDataRepository marketDataRepository) {
    return new GeminiScannerEngineImpl(geminiService, marketDataRepository);
  }
}
