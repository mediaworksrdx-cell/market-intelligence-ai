package com.example.marketintelligence.data.source.workers;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ScanWorker_Factory {
  public ScanWorker_Factory() {
  }

  public ScanWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams);
  }

  public static ScanWorker_Factory create() {
    return new ScanWorker_Factory();
  }

  public static ScanWorker newInstance(Context appContext, WorkerParameters workerParams) {
    return new ScanWorker(appContext, workerParams);
  }
}
