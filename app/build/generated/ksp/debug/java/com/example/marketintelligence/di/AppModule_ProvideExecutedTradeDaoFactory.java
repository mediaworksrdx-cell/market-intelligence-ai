package com.example.marketintelligence.di;

import com.example.marketintelligence.data.source.local.AppDatabase;
import com.example.marketintelligence.data.source.local.ExecutedTradeDao;
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
public final class AppModule_ProvideExecutedTradeDaoFactory implements Factory<ExecutedTradeDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideExecutedTradeDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ExecutedTradeDao get() {
    return provideExecutedTradeDao(databaseProvider.get());
  }

  public static AppModule_ProvideExecutedTradeDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideExecutedTradeDaoFactory(databaseProvider);
  }

  public static ExecutedTradeDao provideExecutedTradeDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideExecutedTradeDao(database));
  }
}
