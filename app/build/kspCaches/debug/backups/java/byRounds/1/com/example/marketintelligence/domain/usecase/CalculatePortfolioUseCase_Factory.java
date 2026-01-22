package com.example.marketintelligence.domain.usecase;

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
public final class CalculatePortfolioUseCase_Factory implements Factory<CalculatePortfolioUseCase> {
  @Override
  public CalculatePortfolioUseCase get() {
    return newInstance();
  }

  public static CalculatePortfolioUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CalculatePortfolioUseCase newInstance() {
    return new CalculatePortfolioUseCase();
  }

  private static final class InstanceHolder {
    private static final CalculatePortfolioUseCase_Factory INSTANCE = new CalculatePortfolioUseCase_Factory();
  }
}
