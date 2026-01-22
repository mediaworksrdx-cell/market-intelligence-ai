package com.example.marketintelligence.domain.engine;

import com.example.marketintelligence.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.util.Set;
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
public final class EngineRouter_Factory implements Factory<EngineRouter> {
  private final Provider<Set<Engine>> enginesProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<EngineLogService> logServiceProvider;

  public EngineRouter_Factory(Provider<Set<Engine>> enginesProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<EngineLogService> logServiceProvider) {
    this.enginesProvider = enginesProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.logServiceProvider = logServiceProvider;
  }

  @Override
  public EngineRouter get() {
    return newInstance(enginesProvider.get(), settingsRepositoryProvider.get(), logServiceProvider.get());
  }

  public static EngineRouter_Factory create(Provider<Set<Engine>> enginesProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<EngineLogService> logServiceProvider) {
    return new EngineRouter_Factory(enginesProvider, settingsRepositoryProvider, logServiceProvider);
  }

  public static EngineRouter newInstance(Set<Engine> engines, SettingsRepository settingsRepository,
      EngineLogService logService) {
    return new EngineRouter(engines, settingsRepository, logService);
  }
}
