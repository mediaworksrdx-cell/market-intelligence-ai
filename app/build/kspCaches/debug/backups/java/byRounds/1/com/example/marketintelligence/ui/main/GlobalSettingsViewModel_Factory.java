package com.example.marketintelligence.ui.main;

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
public final class GlobalSettingsViewModel_Factory implements Factory<GlobalSettingsViewModel> {
  @Override
  public GlobalSettingsViewModel get() {
    return newInstance();
  }

  public static GlobalSettingsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GlobalSettingsViewModel newInstance() {
    return new GlobalSettingsViewModel();
  }

  private static final class InstanceHolder {
    private static final GlobalSettingsViewModel_Factory INSTANCE = new GlobalSettingsViewModel_Factory();
  }
}
