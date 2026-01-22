package com.example.marketintelligence.ui.market;

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
public final class MarketViewModel_Factory implements Factory<MarketViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public MarketViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public MarketViewModel get() {
    return newInstance(settingsRepositoryProvider.get());
  }

  public static MarketViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new MarketViewModel_Factory(settingsRepositoryProvider);
  }

  public static MarketViewModel newInstance(SettingsRepository settingsRepository) {
    return new MarketViewModel(settingsRepository);
  }
}
