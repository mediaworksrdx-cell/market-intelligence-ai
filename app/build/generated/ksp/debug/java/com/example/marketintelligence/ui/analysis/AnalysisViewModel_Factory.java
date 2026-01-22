package com.example.marketintelligence.ui.analysis;

import androidx.lifecycle.SavedStateHandle;
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
public final class AnalysisViewModel_Factory implements Factory<AnalysisViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<EngineRouter> engineRouterProvider;

  public AnalysisViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<EngineRouter> engineRouterProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.engineRouterProvider = engineRouterProvider;
  }

  @Override
  public AnalysisViewModel get() {
    return newInstance(savedStateHandleProvider.get(), engineRouterProvider.get());
  }

  public static AnalysisViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<EngineRouter> engineRouterProvider) {
    return new AnalysisViewModel_Factory(savedStateHandleProvider, engineRouterProvider);
  }

  public static AnalysisViewModel newInstance(SavedStateHandle savedStateHandle,
      EngineRouter engineRouter) {
    return new AnalysisViewModel(savedStateHandle, engineRouter);
  }
}
