package com.example.marketintelligence.domain.engine;

import com.example.marketintelligence.data.engine.GeminiScannerEngineImpl;
import com.example.marketintelligence.data.engine.ProprietaryScannerEngineImpl;
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
public final class ScannerEngineFactory_Factory implements Factory<ScannerEngineFactory> {
  private final Provider<GeminiScannerEngineImpl> geminiEngineProvider;

  private final Provider<ProprietaryScannerEngineImpl> proprietaryEngineProvider;

  public ScannerEngineFactory_Factory(Provider<GeminiScannerEngineImpl> geminiEngineProvider,
      Provider<ProprietaryScannerEngineImpl> proprietaryEngineProvider) {
    this.geminiEngineProvider = geminiEngineProvider;
    this.proprietaryEngineProvider = proprietaryEngineProvider;
  }

  @Override
  public ScannerEngineFactory get() {
    return newInstance(geminiEngineProvider.get(), proprietaryEngineProvider.get());
  }

  public static ScannerEngineFactory_Factory create(
      Provider<GeminiScannerEngineImpl> geminiEngineProvider,
      Provider<ProprietaryScannerEngineImpl> proprietaryEngineProvider) {
    return new ScannerEngineFactory_Factory(geminiEngineProvider, proprietaryEngineProvider);
  }

  public static ScannerEngineFactory newInstance(GeminiScannerEngineImpl geminiEngine,
      ProprietaryScannerEngineImpl proprietaryEngine) {
    return new ScannerEngineFactory(geminiEngine, proprietaryEngine);
  }
}
