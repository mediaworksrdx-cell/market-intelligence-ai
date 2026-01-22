package com.example.marketintelligence.di;

import com.example.marketintelligence.data.source.local.AppDatabase;
import com.example.marketintelligence.data.source.local.HoldingDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideHoldingDaoFactory implements Factory<HoldingDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideHoldingDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public HoldingDao get() {
    return provideHoldingDao(databaseProvider.get());
  }

  public static AppModule_ProvideHoldingDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideHoldingDaoFactory(databaseProvider);
  }

  public static HoldingDao provideHoldingDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHoldingDao(database));
  }
}
