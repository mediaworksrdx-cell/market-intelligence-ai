package com.example.marketintelligence.data.source.workers;

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
public final class ScanWorker_AssistedFactory_Impl implements ScanWorker_AssistedFactory {
  private final ScanWorker_Factory delegateFactory;

  ScanWorker_AssistedFactory_Impl(ScanWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public ScanWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<ScanWorker_AssistedFactory> create(ScanWorker_Factory delegateFactory) {
    return InstanceFactory.create(new ScanWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<ScanWorker_AssistedFactory> createFactoryProvider(
      ScanWorker_Factory delegateFactory) {
    return InstanceFactory.create(new ScanWorker_AssistedFactory_Impl(delegateFactory));
  }
}
