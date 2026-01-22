package com.example.marketintelligence.data.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProprietaryScannerEngineImpl_Factory implements Factory<ProprietaryScannerEngineImpl> {
  @Override
  public ProprietaryScannerEngineImpl get() {
    return newInstance();
  }

  public static ProprietaryScannerEngineImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProprietaryScannerEngineImpl newInstance() {
    return new ProprietaryScannerEngineImpl();
  }

  private static final class InstanceHolder {
    private static final ProprietaryScannerEngineImpl_Factory INSTANCE = new ProprietaryScannerEngineImpl_Factory();
  }
}
