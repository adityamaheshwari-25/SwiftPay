package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.IdempotencyKey;


public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long>{
	Optional<IdempotencyKey> findByUserIdAndApiNameAndIdempotencyKey(Long userId, String apiName, String idempotencyKey);

	void deleteByExpiresAtBefore(LocalDateTime time);
}
