package com.example.redxaiscanner.background;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AnalysisWorker_AssistedFactory_Impl implements AnalysisWorker_AssistedFactory {
  private final AnalysisWorker_Factory delegateFactory;

  AnalysisWorker_AssistedFactory_Impl(AnalysisWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public AnalysisWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<AnalysisWorker_AssistedFactory> create(
      AnalysisWorker_Factory delegateFactory) {
    return InstanceFactory.create(new AnalysisWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<AnalysisWorker_AssistedFactory> createFactoryProvider(
      AnalysisWorker_Factory delegateFactory) {
    return InstanceFactory.create(new AnalysisWorker_AssistedFactory_Impl(delegateFactory));
  }
}
