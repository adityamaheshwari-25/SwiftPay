package com.example.demo.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 
 * Caffeine is a in-memory caching.
 * 
 *.maximumSize(500)
	The cache can hold up to ~500 entries.
	Once it exceeds this, Caffeine evicts entries based on its eviction policy (approx LRU / Window TinyLFU style strategy). 
	Practically: it will keep the “most valuable” entries. 
	
	@Configuration: 
		Marks the class as a source of bean definitions. This class contains methods that define beans.
		Process it at startup. But it does NOT create beans by itself.
	
	@Bean - Marks a specific method whose return value should be registered as a Spring bean.
			Call this method once at startup and register the returned object in the ApplicationContext.
	
	
	Caffeine caches are thread-safe; Spring’s cache abstraction is designed for concurrent access.
	
	
	Why not just create the object manually?
	Because Spring’s dependency injection container must manage the object lifecycle. For caching to work, 
	Spring needs a CacheManager bean in the ApplicationContext. Simply creating an object with new would 
	not register it with Spring, so the caching abstraction would not function. @Configuration and @Bean 
	ensure the object is managed, singleton-scoped, and available for injection and AOP processing.
	
	We use @Configuration and @Bean so that Spring can manage the lifecycle of the CacheManager. 
	Simply instantiating a class manually would not register it in the ApplicationContext, and Spring’s 
	caching abstraction relies on a managed CacheManager bean. This allows dependency injection, singleton 
	management, and AOP features like @Cacheable and @CacheEvict to work correctly.
 */
@Configuration // 
public class CacheConfig {
	
	/**
	 *Marks cacheManager() as a bean factory method. The returned object is stored in the Spring context, and other parts of the app can inject it. 
	 */
	@Bean
	public CacheManager cacheManager() {
		
		// sets up a cache manager aware of three cache names.
		CaffeineCacheManager mgr = new CaffeineCacheManager(
					"hvMerchantSummary", // these are called named cache.
					"hvMerchantCount",
					"pendingKyc"
				);
		
		mgr.registerCustomCache("hvMerchantSummary", 
					Caffeine.newBuilder()
	                .maximumSize(500)
	                .expireAfterWrite(Duration.ofSeconds(30))
	                .build()); // builds the Cache instance.
		
	    mgr.registerCustomCache("hvMerchantCount",
		    		Caffeine.newBuilder()
	                .maximumSize(500)
	                .expireAfterWrite(Duration.ofSeconds(30))
	                .build());
	    
	    mgr.registerCustomCache("pendingKyc",
		    		Caffeine.newBuilder()
	                .maximumSize(50)
	                .expireAfterWrite(Duration.ofSeconds(15))
	                .build());
		
		return mgr; // Return as CacheManager
	}

}
