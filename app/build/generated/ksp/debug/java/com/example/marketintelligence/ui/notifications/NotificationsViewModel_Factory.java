package com.example.marketintelligence.ui.notifications;

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
public final class NotificationsViewModel_Factory implements Factory<NotificationsViewModel> {
  @Override
  public NotificationsViewModel get() {
    return newInstance();
  }

  public static NotificationsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static NotificationsViewModel newInstance() {
    return new NotificationsViewModel();
  }

  private static final class InstanceHolder {
    private static final NotificationsViewModel_Factory INSTANCE = new NotificationsViewModel_Factory();
  }
}
