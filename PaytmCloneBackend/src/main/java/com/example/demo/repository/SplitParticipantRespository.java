package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.SplitParticipant;
import com.example.demo.entity.enums.SplitParticipantState;


public interface SplitParticipantRespository extends JpaRepository<SplitParticipant, Long>{
	Optional<SplitParticipant> findBySplitRequest_IdAndParticipant_Id(Long splitRequest_Id, Long participantUserId);
	
	long countBySplitRequest_Id(Long splitRequestId);
	long countBySplitRequest_IdAndState(Long splitRequestId, SplitParticipantState state);
	
	// ✅ NEW: list splits where user is participant (fetch request + initiator)
    @EntityGraph(attributePaths = {"splitRequest", "splitRequest.initiator"})
    @Query("""
        select sp
        from SplitParticipant sp
        where sp.participant.id = :userId
        order by sp.splitRequest.createdAt desc
    """)
    List<SplitParticipant> findAllForUser(@Param("userId") Long userId);
}
