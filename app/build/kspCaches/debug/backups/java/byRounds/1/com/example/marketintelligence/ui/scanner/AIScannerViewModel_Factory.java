package com.example.marketintelligence.ui.scanner;

import android.content.Context;
import com.example.marketintelligence.domain.repository.SettingsRepository;
import com.example.marketintelligence.domain.usecase.ScanStockUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AIScannerViewModel_Factory implements Factory<AIScannerViewModel> {
  private final Provider<ScanStockUseCase> scanStockUseCaseProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<Context> contextProvider;

  public AIScannerViewModel_Factory(Provider<ScanStockUseCase> scanStockUseCaseProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<Context> contextProvider) {
    this.scanStockUseCaseProvider = scanStockUseCaseProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AIScannerViewModel get() {
    return newInstance(scanStockUseCaseProvider.get(), settingsRepositoryProvider.get(), contextProvider.get());
  }

  public static AIScannerViewModel_Factory create(
      Provider<ScanStockUseCase> scanStockUseCaseProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<Context> contextProvider) {
    return new AIScannerViewModel_Factory(scanStockUseCaseProvider, settingsRepositoryProvider, contextProvider);
  }

  public static AIScannerViewModel newInstance(ScanStockUseCase scanStockUseCase,
      SettingsRepository settingsRepository, Context context) {
    return new AIScannerViewModel(scanStockUseCase, settingsRepository, context);
  }
}
