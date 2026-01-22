package com.example.marketintelligence.ui.fno;

import com.example.marketintelligence.domain.engine.BuildupEngine;
import com.example.marketintelligence.domain.engine.EngineRouter;
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus;
import com.example.marketintelligence.domain.repository.SettingsRepository;
import com.example.marketintelligence.domain.usecase.FnoWorkflowUseCase;
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
public final class FnoViewModel_Factory implements Factory<FnoViewModel> {
  private final Provider<FnoWorkflowUseCase> fnoWorkflowUseCaseProvider;

  private final Provider<EngineRouter> engineRouterProvider;

  private final Provider<LiveIntelligenceBus> intelligenceBusProvider;

  private final Provider<BuildupEngine> buildupEngineProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public FnoViewModel_Factory(Provider<FnoWorkflowUseCase> fnoWorkflowUseCaseProvider,
      Provider<EngineRouter> engineRouterProvider,
      Provider<LiveIntelligenceBus> intelligenceBusProvider,
      Provider<BuildupEngine> buildupEngineProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.fnoWorkflowUseCaseProvider = fnoWorkflowUseCaseProvider;
    this.engineRouterProvider = engineRouterProvider;
    this.intelligenceBusProvider = intelligenceBusProvider;
    this.buildupEngineProvider = buildupEngineProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public FnoViewModel get() {
    return newInstance(fnoWorkflowUseCaseProvider.get(), engineRouterProvider.get(), intelligenceBusProvider.get(), buildupEngineProvider.get(), settingsRepositoryProvider.get());
  }

  public static FnoViewModel_Factory create(Provider<FnoWorkflowUseCase> fnoWorkflowUseCaseProvider,
      Provider<EngineRouter> engineRouterProvider,
      Provider<LiveIntelligenceBus> intelligenceBusProvider,
      Provider<BuildupEngine> buildupEngineProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new FnoViewModel_Factory(fnoWorkflowUseCaseProvider, engineRouterProvider, intelligenceBusProvider, buildupEngineProvider, settingsRepositoryProvider);
  }

  public static FnoViewModel newInstance(FnoWorkflowUseCase fnoWorkflowUseCase,
      EngineRouter engineRouter, LiveIntelligenceBus intelligenceBus, BuildupEngine buildupEngine,
      SettingsRepository settingsRepository) {
    return new FnoViewModel(fnoWorkflowUseCase, engineRouter, intelligenceBus, buildupEngine, settingsRepository);
  }
}
