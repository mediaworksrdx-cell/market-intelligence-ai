package com.example.marketintelligence.data.repository;

import com.example.marketintelligence.data.source.remote.RealTimeDataService;
import com.example.marketintelligence.data.source.remote.TwelveDataApiService;
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
public final class MarketDataRepositoryImpl_Factory implements Factory<MarketDataRepositoryImpl> {
  private final Provider<TwelveDataApiService> apiServiceProvider;

  private final Provider<RealTimeDataService> realTimeServiceProvider;

  public MarketDataRepositoryImpl_Factory(Provider<TwelveDataApiService> apiServiceProvider,
      Provider<RealTimeDataService> realTimeServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.realTimeServiceProvider = realTimeServiceProvider;
  }

  @Override
  public MarketDataRepositoryImpl get() {
    return newInstance(apiServiceProvider.get(), realTimeServiceProvider.get());
  }

  public static MarketDataRepositoryImpl_Factory create(
      Provider<TwelveDataApiService> apiServiceProvider,
      Provider<RealTimeDataService> realTimeServiceProvider) {
    return new MarketDataRepositoryImpl_Factory(apiServiceProvider, realTimeServiceProvider);
  }

  public static MarketDataRepositoryImpl newInstance(TwelveDataApiService apiService,
      RealTimeDataService realTimeService) {
    return new MarketDataRepositoryImpl(apiService, realTimeService);
  }
}
