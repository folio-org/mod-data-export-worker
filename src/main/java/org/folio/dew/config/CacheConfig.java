package org.folio.dew.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public Caffeine caffeineConfig() { //NOSONAR
     return Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS);
  }

  @Bean
  public CacheManager cacheManager(Caffeine caffeine) { //NOSONAR
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(caffeine);
    return caffeineCacheManager;
  }

  @Bean("defaultKeyGenerator")
  public KeyGenerator defaultKeyGenerator() {
    return (target, method, params) -> Arrays.stream(params)
      .filter(param -> !param.getClass().equals(ErrorServiceArgs.class))
      .toList();
  }
}
