package com.example.demo.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.RegisterAppUserDto;
import com.example.demo.dto.RegisterMerchantDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Merchant;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.Role;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.MerchantRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.AuthService;
import com.example.demo.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link AuthService} responsible for:
 * <ul>
 *   <li>User registration</li>
 *   <li>Merchant registration</li>
 *   <li>User authentication (login)</li>
 * </ul>
 *
 * <p>
 * This service handles core authentication flows and coordinates
 * between repositories, security components, and token generation.
 * </p>
 *
 * <p><b>Note:</b> Controllers handle HTTP concerns, while this service
 * enforces business rules and data integrity.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

	private final AppUserRepository userRepo;
	private final WalletRepository walletRepo;
	private final MerchantRepository merchantRepo;
	
	private final ModelMapper mapper;
	private final BCryptPasswordEncoder encoder;
	private final AuthenticationManager authManager;
	private final JwtUtil jwtUtil;
	
    /**
     * Registers a new application user.
     *
     * <p>
     * Business rules:
     * <ul>
     *   <li>Email and mobile number must be unique</li>
     *   <li>Password is stored in encrypted form</li>
     *   <li>A default wallet is created for every user</li>
     * </ul>
     * </p>
     *
     * @param req user registration payload
     * @return {@link AuthResponseDto} containing JWT token and user details
     * @throws UserAlreadyExistsException if email or mobile already exists
     */
	@Override
	public AuthResponseDto registerUser(RegisterAppUserDto req) throws UserAlreadyExistsException {
		
		log.info("Processing user registration for email: {}", req.getEmail());
		
		if (userRepo.existsByEmail(req.getEmail())) {
		    throw new UserAlreadyExistsException(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS);
		}
		
		if (userRepo.existsByMobile(req.getMobile())) {
		    throw new UserAlreadyExistsException(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS);
		}
		
		AppUser user = mapper.map(req, AppUser.class);
		user.setPassword(encoder.encode(req.getPassword()));
		user.setRole(Role.USER);
		
		userRepo.save(user);
		
		log.info("User entity saved with ID: {}", user.getId());
		
		createWallet(user);
		
		// need to generate the token as the user redirects to the dashboard and dashboard requires token.
		String token = jwtUtil.generateToken(user);
		
		return new AuthResponseDto(token, user.getId(), user.getName(), user.getRole().name());
	}
	
    /**
     * Registers a new merchant account.
     *
     * <p>
     * This flow:
     * <ul>
     *   <li>Creates an AppUser with MERCHANT role</li>
     *   <li>Creates a wallet</li>
     *   <li>Creates a linked Merchant profile</li>
     * </ul>
     * </p>
     *
     * @param req merchant registration payload
     * @return {@link AuthResponseDto} containing JWT token and user details
     * @throws UserAlreadyExistsException if email or mobile already exists
     */
	@Override
	public AuthResponseDto registerMerchant(RegisterMerchantDto req) throws UserAlreadyExistsException {
		log.info("Processing merchant registration for business: {}", req.getBusinessName());
		
		if (userRepo.existsByEmail(req.getEmail())) {
			log.warn("Merchant registration failed: Account already exists for {}", req.getEmail());
		    throw new UserAlreadyExistsException(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS);
		}
		
		if (userRepo.existsByMobile(req.getMobile())) {
		    throw new UserAlreadyExistsException(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS);
		}
		
		AppUser user = mapper.map(req, AppUser.class);
		
		// better to take it directly from the req and not user, simple, clean and clear as that.
		user.setPassword(encoder.encode(req.getPassword()));
		user.setRole(Role.MERCHANT);
		
		userRepo.save(user);
		
		createWallet(user);
		
		// creating merchant profile
		Merchant merchant = new Merchant();
		merchant.setUser(user);
		String merchantCode = IdGenerator.generateMerchantId();
		System.out.println(merchantCode);
		merchant.setMerchantCode(merchantCode);
		merchant.setBusinessName(req.getBusinessName());
		merchant.setCategory(req.getCategory());
		merchantRepo.save(merchant);
		
		String token = jwtUtil.generateToken(user);
		
		log.info("Merchant profile created successfully. Code: {}, UserID: {}", merchantCode, user.getId());
		
		return new AuthResponseDto(token, user.getId(), user.getName(), user.getRole().name());
	}
	
    /**
     * Authenticates a user or merchant using email and password.
     *
     * <p>
     * Authentication flow:
     * <ol>
     *   <li>Spring Security validates credentials</li>
     *   <li>User entity is fetched</li>
     *   <li>JWT token is generated</li>
     * </ol>
     * </p>
     *
     * @param req login request containing credentials
     * @return {@link AuthResponseDto} with JWT token and user details
     * @throws UserNotFoundException if user does not exist
     */
	// this is the good one to understand, how its logging in the user/merchant
	@Override
	public AuthResponseDto login(LoginRequestDto req) throws UserNotFoundException {
		log.info("Login attempt for user: {}", req.getEmail());
		
		authManager.authenticate(
					new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
				);
		
		AppUser user = userRepo.findByEmail(req.getEmail())
					.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
		
		String token = jwtUtil.generateToken(user);
		
		return new AuthResponseDto(token, user.getId(), user.getName(), user.getRole().name());
	}
	
    /**
     * Creates a default wallet for a newly registered user.
     *
     * <p>
     * This method is intentionally private as wallet creation
     * is an internal concern of the authentication flow.
     * </p>
     *
     * @param user the user for whom the wallet is created
     */
	private void createWallet(AppUser user) {
		log.debug("Creating default wallet for user ID: {}", user.getId());
		Wallet wallet = new Wallet();
		wallet.setUser(user);
		walletRepo.save(wallet);
		log.debug("Wallet created for user ID: {}", user.getId());
	}

	
}
