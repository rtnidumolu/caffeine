/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Serializes the configuration of the cache, reconsitituting it as a {@link Cache},
 * {@link LoadingCache}, or {@link AsyncLoadingCache} using {@link Caffeine} upon
 * deserialization. The data held by the cache is not retained.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
final class SerializationProxy<K, V> implements Serializable {
  private static final long serialVersionUID = 1;

  Ticker ticker;
  boolean async;
  boolean weakKeys;
  boolean weakValues;
  boolean softValues;
  Weigher<?, ?> weigher;
  CacheWriter<?, ?> writer;
  boolean isRecordingStats;
  long expiresAfterWriteNanos;
  long expiresAfterAccessNanos;
  long refreshAfterWriteNanos;
  AsyncCacheLoader<?, ?> loader;
  RemovalListener<?, ?> removalListener;
  long maximumSize = Caffeine.UNSET_INT;
  long maximumWeight = Caffeine.UNSET_INT;

  @SuppressWarnings("unchecked")
  Caffeine<Object, Object> recreateCaffeine() {
    Caffeine<Object, Object> builder = Caffeine.newBuilder();
    if (ticker != null) {
      builder.ticker(ticker);
    }
    if (isRecordingStats) {
      builder.recordStats();
    }
    if (maximumSize != Caffeine.UNSET_INT) {
      builder.maximumSize(maximumSize);
    }
    if (maximumWeight != Caffeine.UNSET_INT) {
      builder.maximumWeight(maximumWeight);
      builder.weigher((Weigher<Object, Object>) weigher);
    }
    if (expiresAfterWriteNanos > 0) {
      builder.expireAfterWrite(expiresAfterWriteNanos, TimeUnit.NANOSECONDS);
    }
    if (expiresAfterAccessNanos > 0) {
      builder.expireAfterAccess(expiresAfterAccessNanos, TimeUnit.NANOSECONDS);
    }
    if (refreshAfterWriteNanos > 0) {
      builder.refreshAfterWrite(refreshAfterWriteNanos, TimeUnit.NANOSECONDS);
    }
    if (weakKeys) {
      builder.weakKeys();
    }
    if (weakValues) {
      builder.weakValues();
    }
    if (softValues) {
      builder.softValues();
    }
    if (removalListener != null) {
      builder.removalListener((RemovalListener<Object, Object>) removalListener);
    }
    if (writer != CacheWriter.disabledWriter()) {
      builder.writer(writer);
    }
    return builder;
  }

  Object readResolve() {
    Caffeine<Object, Object> builder = recreateCaffeine();
    if (async) {
      return builder.buildAsync(loader);
    } else if (loader != null) {
      @SuppressWarnings("unchecked")
      CacheLoader<K, V> cacheLoader = (CacheLoader<K, V>) loader;
      return builder.build(cacheLoader);
    }
    return builder.build();
  }
}
