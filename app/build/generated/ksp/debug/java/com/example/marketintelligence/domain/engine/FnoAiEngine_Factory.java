package com.example.marketintelligence.domain.engine;

import com.example.marketintelligence.domain.repository.MarketDataRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class FnoAiEngine_Factory implements Factory<FnoAiEngine> {
  private final Provider<MarketDataRepository> marketDataRepositoryProvider;

  public FnoAiEngine_Factory(Provider<MarketDataRepository> marketDataRepositoryProvider) {
    this.marketDataRepositoryProvider = marketDataRepositoryProvider;
  }

  @Override
  public FnoAiEngine get() {
    return newInstance(marketDataRepositoryProvider.get());
  }

  public static FnoAiEngine_Factory create(
      Provider<MarketDataRepository> marketDataRepositoryProvider) {
    return new FnoAiEngine_Factory(marketDataRepositoryProvider);
  }

  public static FnoAiEngine newInstance(MarketDataRepository marketDataRepository) {
    return new FnoAiEngine(marketDataRepository);
  }
}
