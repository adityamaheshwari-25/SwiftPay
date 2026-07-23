package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Wallet;
import java.util.List;
import java.util.Optional;

import com.example.demo.entity.AppUser;


@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long>{
	Optional<Wallet> findByUser(AppUser user);
}
