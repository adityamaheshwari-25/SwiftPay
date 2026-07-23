package com.example.demo.bootstrap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.config.SuperAdminProperties;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.enums.Role;
import com.example.demo.repository.AppUserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap {
	
	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SuperAdminProperties properties;
	
	@PostConstruct
	public void createSuperAdminIfNotExists() {
		
		// check if Super Admin already exists
		if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
			return;
		}
		
		AppUser superAdmin = new AppUser();
		superAdmin.setName("Super Admin");
        superAdmin.setEmail(properties.getEmail());
        superAdmin.setPassword(
                passwordEncoder.encode(properties.getPassword())
        );
        superAdmin.setMobile("9999911111");
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setKycVerified(true);

        userRepository.save(superAdmin);
		
	}
}
