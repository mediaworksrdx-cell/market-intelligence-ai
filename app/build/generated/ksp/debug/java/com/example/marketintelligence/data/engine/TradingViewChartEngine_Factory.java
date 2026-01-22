package com.example.marketintelligence.data.engine;

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
public final class TradingViewChartEngine_Factory implements Factory<TradingViewChartEngine> {
  @Override
  public TradingViewChartEngine get() {
    return newInstance();
  }

  public static TradingViewChartEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TradingViewChartEngine newInstance() {
    return new TradingViewChartEngine();
  }

  private static final class InstanceHolder {
    private static final TradingViewChartEngine_Factory INSTANCE = new TradingViewChartEngine_Factory();
  }
}
