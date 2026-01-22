package com.example.redxaiscanner.background;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.example.redxaiscanner.domain.usecase.GenerateTradeSetupUseCase;
import dagger.internal.DaggerGenerated;
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
public final class AnalysisWorker_Factory {
  private final Provider<GenerateTradeSetupUseCase> generateTradeSetupUseCaseProvider;

  public AnalysisWorker_Factory(
      Provider<GenerateTradeSetupUseCase> generateTradeSetupUseCaseProvider) {
    this.generateTradeSetupUseCaseProvider = generateTradeSetupUseCaseProvider;
  }

  public AnalysisWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, generateTradeSetupUseCaseProvider.get());
  }

  public static AnalysisWorker_Factory create(
      Provider<GenerateTradeSetupUseCase> generateTradeSetupUseCaseProvider) {
    return new AnalysisWorker_Factory(generateTradeSetupUseCaseProvider);
  }

  public static AnalysisWorker newInstance(Context appContext, WorkerParameters workerParams,
      GenerateTradeSetupUseCase generateTradeSetupUseCase) {
    return new AnalysisWorker(appContext, workerParams, generateTradeSetupUseCase);
  }
}
