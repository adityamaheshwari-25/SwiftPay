package com.example.demo.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.enums.Role;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
	
	@GetMapping
	public ResponseEntity<List<String>> getAllRoles() {
		List<String> roles = Arrays.stream(Role.values()).map(Enum::name).toList();
		return ResponseEntity.ok(roles);
	}
}
