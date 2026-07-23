package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Merchant;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long>{
	
}
