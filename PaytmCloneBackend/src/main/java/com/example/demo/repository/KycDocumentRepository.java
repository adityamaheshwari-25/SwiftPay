package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import java.util.List;
import com.example.demo.entity.enums.KycStatus;



@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long>{
	Optional<KycDocument> findByUser(AppUser user);
	
	Optional<KycDocument> findByUserId(Long userId);
	
	List<KycDocument> findByStatus(KycStatus status);
}
