package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.enums.Role;
import java.util.List;


@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long>{
	
	boolean existsByRole(Role role);
	
	Optional<AppUser> findByEmail(String email);
	
	boolean existsByEmail(String email);
	
	boolean existsByMobile(String mobile);
	
	Optional<AppUser> findByMobile(String mobile);
	
	List<AppUser> findByRole(Role role);
}
