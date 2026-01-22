package com.example.marketintelligence.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PortfolioRepositoryImpl_Factory implements Factory<PortfolioRepositoryImpl> {
  @Override
  public PortfolioRepositoryImpl get() {
    return newInstance();
  }

  public static PortfolioRepositoryImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PortfolioRepositoryImpl newInstance() {
    return new PortfolioRepositoryImpl();
  }

  private static final class InstanceHolder {
    private static final PortfolioRepositoryImpl_Factory INSTANCE = new PortfolioRepositoryImpl_Factory();
  }
}
