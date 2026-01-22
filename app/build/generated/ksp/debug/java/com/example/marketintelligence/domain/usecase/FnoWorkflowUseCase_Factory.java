package com.example.marketintelligence.domain.usecase;

import com.example.marketintelligence.domain.engine.MarketRegimeEngine;
import com.example.marketintelligence.domain.engine.PositionConstructionEngine;
import com.example.marketintelligence.domain.engine.RiskManagementEngine;
import com.example.marketintelligence.domain.engine.StrategySelectionEngine;
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
public final class FnoWorkflowUseCase_Factory implements Factory<FnoWorkflowUseCase> {
  private final Provider<MarketDataRepository> marketDataRepositoryProvider;

  private final Provider<MarketRegimeEngine> regimeEngineProvider;

  private final Provider<StrategySelectionEngine> strategyEngineProvider;

  private final Provider<PositionConstructionEngine> positionEngineProvider;

  private final Provider<RiskManagementEngine> riskEngineProvider;

  public FnoWorkflowUseCase_Factory(Provider<MarketDataRepository> marketDataRepositoryProvider,
      Provider<MarketRegimeEngine> regimeEngineProvider,
      Provider<StrategySelectionEngine> strategyEngineProvider,
      Provider<PositionConstructionEngine> positionEngineProvider,
      Provider<RiskManagementEngine> riskEngineProvider) {
    this.marketDataRepositoryProvider = marketDataRepositoryProvider;
    this.regimeEngineProvider = regimeEngineProvider;
    this.strategyEngineProvider = strategyEngineProvider;
    this.positionEngineProvider = positionEngineProvider;
    this.riskEngineProvider = riskEngineProvider;
  }

  @Override
  public FnoWorkflowUseCase get() {
    return newInstance(marketDataRepositoryProvider.get(), regimeEngineProvider.get(), strategyEngineProvider.get(), positionEngineProvider.get(), riskEngineProvider.get());
  }

  public static FnoWorkflowUseCase_Factory create(
      Provider<MarketDataRepository> marketDataRepositoryProvider,
      Provider<MarketRegimeEngine> regimeEngineProvider,
      Provider<StrategySelectionEngine> strategyEngineProvider,
      Provider<PositionConstructionEngine> positionEngineProvider,
      Provider<RiskManagementEngine> riskEngineProvider) {
    return new FnoWorkflowUseCase_Factory(marketDataRepositoryProvider, regimeEngineProvider, strategyEngineProvider, positionEngineProvider, riskEngineProvider);
  }

  public static FnoWorkflowUseCase newInstance(MarketDataRepository marketDataRepository,
      MarketRegimeEngine regimeEngine, StrategySelectionEngine strategyEngine,
      PositionConstructionEngine positionEngine, RiskManagementEngine riskEngine) {
    return new FnoWorkflowUseCase(marketDataRepository, regimeEngine, strategyEngine, positionEngine, riskEngine);
  }
}
