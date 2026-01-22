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
public final class ProprietaryMentorEngineImpl_Factory implements Factory<ProprietaryMentorEngineImpl> {
  @Override
  public ProprietaryMentorEngineImpl get() {
    return newInstance();
  }

  public static ProprietaryMentorEngineImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProprietaryMentorEngineImpl newInstance() {
    return new ProprietaryMentorEngineImpl();
  }

  private static final class InstanceHolder {
    private static final ProprietaryMentorEngineImpl_Factory INSTANCE = new ProprietaryMentorEngineImpl_Factory();
  }
}
