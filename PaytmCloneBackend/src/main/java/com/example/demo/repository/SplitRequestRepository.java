package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.SplitRequest;

public interface SplitRequestRepository extends JpaRepository<SplitRequest, Long>{
	
	@EntityGraph(attributePaths = {"participants"}) // enough for counts
	List<SplitRequest> findByInitiator_IdOrderByCreatedAtDesc(Long initiatorId);
}
