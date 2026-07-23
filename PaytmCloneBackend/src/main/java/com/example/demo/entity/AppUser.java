package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")    
    private Long id;   

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String mobile;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    private boolean kycVerified = false;
    
    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "mpin_hash")
    private String mpin; // stores hashed MPIN
    
    private boolean mpinSet = false; // prevents transactions until MPIN is set

    // -------------------------
    // Relationships
    // -------------------------

    // One wallet per user
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude // there was a circular issue, with the @Data in the Split Service with the User, so added this, understand this well.
    private Wallet wallet;

    // Multiple bank accounts per user
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<BankAccount> bankAccounts;

    // Only present if role = MERCHANT
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Merchant merchant;
}

