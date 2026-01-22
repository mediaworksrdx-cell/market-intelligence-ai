package com.example.marketintelligence.ui.portfolio;

import com.example.marketintelligence.domain.repository.PortfolioRepository;
import com.example.marketintelligence.domain.repository.SettingsRepository;
import com.example.marketintelligence.domain.usecase.CalculatePortfolioUseCase;
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
public final class PortfolioViewModel_Factory implements Factory<PortfolioViewModel> {
  private final Provider<PortfolioRepository> portfolioRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<CalculatePortfolioUseCase> calculatePortfolioUseCaseProvider;

  public PortfolioViewModel_Factory(Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CalculatePortfolioUseCase> calculatePortfolioUseCaseProvider) {
    this.portfolioRepositoryProvider = portfolioRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.calculatePortfolioUseCaseProvider = calculatePortfolioUseCaseProvider;
  }

  @Override
  public PortfolioViewModel get() {
    return newInstance(portfolioRepositoryProvider.get(), settingsRepositoryProvider.get(), calculatePortfolioUseCaseProvider.get());
  }

  public static PortfolioViewModel_Factory create(
      Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CalculatePortfolioUseCase> calculatePortfolioUseCaseProvider) {
    return new PortfolioViewModel_Factory(portfolioRepositoryProvider, settingsRepositoryProvider, calculatePortfolioUseCaseProvider);
  }

  public static PortfolioViewModel newInstance(PortfolioRepository portfolioRepository,
      SettingsRepository settingsRepository, CalculatePortfolioUseCase calculatePortfolioUseCase) {
    return new PortfolioViewModel(portfolioRepository, settingsRepository, calculatePortfolioUseCase);
  }
}
