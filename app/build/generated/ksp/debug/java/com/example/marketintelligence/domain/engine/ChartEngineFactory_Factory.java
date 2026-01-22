package com.example.marketintelligence.domain.engine;

import com.example.marketintelligence.data.engine.ProprietaryChartEngine;
import com.example.marketintelligence.data.engine.TradingViewChartEngine;
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
public final class ChartEngineFactory_Factory implements Factory<ChartEngineFactory> {
  private final Provider<TradingViewChartEngine> tradingViewEngineProvider;

  private final Provider<ProprietaryChartEngine> proprietaryEngineProvider;

  public ChartEngineFactory_Factory(Provider<TradingViewChartEngine> tradingViewEngineProvider,
      Provider<ProprietaryChartEngine> proprietaryEngineProvider) {
    this.tradingViewEngineProvider = tradingViewEngineProvider;
    this.proprietaryEngineProvider = proprietaryEngineProvider;
  }

  @Override
  public ChartEngineFactory get() {
    return newInstance(tradingViewEngineProvider.get(), proprietaryEngineProvider.get());
  }

  public static ChartEngineFactory_Factory create(
      Provider<TradingViewChartEngine> tradingViewEngineProvider,
      Provider<ProprietaryChartEngine> proprietaryEngineProvider) {
    return new ChartEngineFactory_Factory(tradingViewEngineProvider, proprietaryEngineProvider);
  }

  public static ChartEngineFactory newInstance(TradingViewChartEngine tradingViewEngine,
      ProprietaryChartEngine proprietaryEngine) {
    return new ChartEngineFactory(tradingViewEngine, proprietaryEngine);
  }
}
