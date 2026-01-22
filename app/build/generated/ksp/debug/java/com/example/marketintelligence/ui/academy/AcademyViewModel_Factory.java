package com.example.marketintelligence.ui.academy;

import com.example.marketintelligence.domain.engine.EngineRouter;
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus;
import com.example.marketintelligence.domain.repository.SettingsRepository;
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
public final class AcademyViewModel_Factory implements Factory<AcademyViewModel> {
  private final Provider<LiveIntelligenceBus> intelligenceBusProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<EngineRouter> engineRouterProvider;

  public AcademyViewModel_Factory(Provider<LiveIntelligenceBus> intelligenceBusProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<EngineRouter> engineRouterProvider) {
    this.intelligenceBusProvider = intelligenceBusProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.engineRouterProvider = engineRouterProvider;
  }

  @Override
  public AcademyViewModel get() {
    return newInstance(intelligenceBusProvider.get(), settingsRepositoryProvider.get(), engineRouterProvider.get());
  }

  public static AcademyViewModel_Factory create(
      Provider<LiveIntelligenceBus> intelligenceBusProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<EngineRouter> engineRouterProvider) {
    return new AcademyViewModel_Factory(intelligenceBusProvider, settingsRepositoryProvider, engineRouterProvider);
  }

  public static AcademyViewModel newInstance(LiveIntelligenceBus intelligenceBus,
      SettingsRepository settingsRepository, EngineRouter engineRouter) {
    return new AcademyViewModel(intelligenceBus, settingsRepository, engineRouter);
  }
}
