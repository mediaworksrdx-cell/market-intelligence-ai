package com.example.marketintelligence.domain.usecase;

import com.example.marketintelligence.domain.engine.EngineRouter;
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
public final class ScanStockUseCase_Factory implements Factory<ScanStockUseCase> {
  private final Provider<EngineRouter> engineRouterProvider;

  public ScanStockUseCase_Factory(Provider<EngineRouter> engineRouterProvider) {
    this.engineRouterProvider = engineRouterProvider;
  }

  @Override
  public ScanStockUseCase get() {
    return newInstance(engineRouterProvider.get());
  }

  public static ScanStockUseCase_Factory create(Provider<EngineRouter> engineRouterProvider) {
    return new ScanStockUseCase_Factory(engineRouterProvider);
  }

  public static ScanStockUseCase newInstance(EngineRouter engineRouter) {
    return new ScanStockUseCase(engineRouter);
  }
}
