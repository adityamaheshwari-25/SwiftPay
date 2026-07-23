package com.example.demo.aspect;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.customAnnotation.Idempotent;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.IdempotencyKey;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.IdempotencyKeyConflictException;
import com.example.demo.exception.IdempotencyKeyNotFoundException;
import com.example.demo.repository.IdempotencyKeyRepository;
import com.example.demo.security.CurrentUserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;


@Aspect
@Component
@Order(1) // HIGHEST PRIORITY: Must be outside the @Retryable and @Transactional
@RequiredArgsConstructor
public class IdempotencyAspect {
	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final ObjectMapper objectMapper;
	private final CurrentUserService currentUserService;
	
	/**
	 * Basically we are taking the combination of userId + idempotency key + api name to avoid API conflicts.
	 *
	 * here in this annotation "idempotent" is called the advice parameter.
	 * 
	 * @Around is from AspectJ’s Java-style annotations — specifically org.aspectj.lang.annotation.Around. 
	 * Spring AOP understands and uses these AspectJ annotations when you enable AOP in Spring.
	 * 
	 * Proxies are basically runtime objects that stand in for the real bean. When client calls methods, they call the 
	 * proxies, which can add behavior before/after deligating the task to the real bean.
	 * So Spring AOP either use JDK dynamic proxies or CGLIB subclasses.
	 * 
	 * Interceptor (or advice) is the code executed by the proxy around/instead-of the real method — e.g. 
	 * transaction interceptor, retry interceptor, your idempotency aspect. Interceptors can be chained: 
	 * proxy calls interceptor A → interceptor B → real method.
	 * That's why @Order(1) matters: it controls position in that interceptor chain.
	 * 
	 * ObjectMapper is the main class of Jackson. It's the JSON (de)serializer.
	 * writeValueAsString(obj) converts the Java object obj(its dto here) into a JSON string.
	 * 
	 * readValue(json, type) parses a JSON string into a Java object of the given type(its referring to DTO object here).
	 * 
	 * @annotation (AspectJ) matches methods annotated with your custom annotation.

		@Around comes from AspectJ (org.aspectj.lang.annotation.Around) and lets you run code 
		before/after method execution.
		
		requestHash used to ensure same key + same request; prevents key reuse with different payloads. because any attacker can do it from postman or like 
		directly hitting the backend or running some malicious scripts from where we don't know.
		
		
		Servlet is basically java class that handles Servlet API.It’s part of the Java EE (now Jakarta EE) specification.
		Servlets run inside a Servlet Container like:
			Tomcat
			Jetty
			Undertow
		Request flow:
		Browser → Tomcat → DispatcherServlet → Controller → Service → Response
		Servlet-based app basically runs on the Tomcat/Jetty using Servlet API.
		If you were using Spring WebFlux (reactive), there would be no Servlet API.
		
		If METHOD is not included in @Target, then you CANNOT use the annotation on methods. The compiler enforces this.
	 
	 * @annotation(idempotent) → binds to parameter name idempotent. The name must match. This is case-sensitive.
	 * 
	 * Reflection is a feature in Java that allows a program to:			
			Inspect classes
			Inspect methods
			Inspect fields
			Inspect annotations
			Create objects
			Invoke methods

		at runtime, even if you didn’t know them at compile time.
		
		Spring uses reflection to:
			Detect @Idempotent on methods
			Get method signatures
			Get return types			
			Get annotation values (like api = "ADD_MONEY")
			
		Its binding annotation as my parameter type is Idempotent and binding that with the variable idempotent that's mention
		in the method declaration.
		
		Example without reflection
			WalletResponseDto dto = new WalletResponseDto();
			dto.getBalance();
						
			Here, everything is known at compile time.
			
			🔹 Example with reflection
			Class<?> clazz = WalletResponseDto.class;
			Method method = clazz.getMethod("getBalance");
			Object result = method.invoke(dto);
			
			Now:
				You’re calling getBalance() dynamically
				You didn’t directly call it
				You found it at runtime
				That is reflection.
		
		Where does it know the annotation type?

			From the advice method parameter:			
			Idempotent idempotent
			
			Spring sees:
			The parameter type is Idempotent
			The pointcut says @annotation(idempotent)
			So it binds the annotation of type Idempotent to that parameter
			
			
			You get the annotation instance "idempotent" via reflection actually.
			
			When this method is called:

				addMoney(...)
				
				
				Spring proxy does:
				
				→ Check if method has @Idempotent
				→ Yes
				→ Get annotation instance via reflection
				→ Call advice method:
				   handleIdempotency(joinPoint, annotationInstance)
				
				So inside advice:
				idempotent.api()
				
				returns "ADD_MONEY"
				
				That value comes directly from the annotation instance.
	 */
	
	// this is basically called the advise method.
	@Around("@annotation(idempotent)")
	public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
		
		// for getting the idempotency key, getting the hold of the request
		/**
		 *RequestContextHolder.currentRequestAttributes() returns a RequestAttributes (generic).
		 *For servlet-based web apps, Spring exposes ServletRequestAttributes which contains getRequest() / getResponse() (servlet-specific).
		 *We cast to ServletRequestAttributes so we can call .getRequest() and get the HttpServletRequest.
		 *If you didn’t cast, you’d only have the generic RequestAttributes API, which doesn’t expose getRequest(). 
		 */
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		
		String idempotencyKey = request.getHeader("Idempotency-Key");
		
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IdempotencyKeyNotFoundException(ErrorMessage.MISSING_IDEMPOTENCY_KEY);
		}
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		AppUser user = currentUserService.getCurrentUser(authentication);
		
		String apiName = idempotent.api();
		
		// these two lines is for getting the return type of the method.
//		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//		Class<?> returnType = signature.getReturnType(); // Class<?> => A runtime type object of unknown generic type, Because we don’t know at compile time what the intercepted method returns, basically Class of some type.
		
		// This is what is crashing because it tries to serialize the User inside 'args'
//		String requestHash = objectMapper.writeValueAsString(joinPoint.getArgs());
		/*
		 * By filtering the arguments, you ensure Jackson only looks at your DTOs, 
		 * which never have Lazy collections or Hibernate proxies.
		 * */
		
		/**
		 *joinPoint.getArgs() — gives you the actual method arguments passed to the intercepted method 
		 *(e.g., Authentication authentication, AddMoneyRequestDto dto). 
		 */
		Object[] args = joinPoint.getArgs();
		List<Object> safeArgs = new ArrayList<>();

		for (Object arg : args) {
		    // Only serialize the DTOs (Request bodies). 
		    // Ignore Authentication/Principal because they contain the Lazy Hibernate entities, and can lead to a lot of internal 
			// data which is not at all required, and can also cause circular refs thing.
			// filter out security-related args because we don't require it. Security objects are explicitly filtered out.
		    if (arg != null && 
		        !(arg instanceof Authentication) && 
		        !(arg instanceof UserDetails)) {
		        safeArgs.add(arg);
		    }
		}
		
		// to save response as a JSON string.
//		String requestHash = objectMapper.writeValueAsString(safeArgs);
		
		/*
		 * Its better to hash it to have uniform size and not large JSON blobs, easy to compare, fixed size.
		 * */
		String incomingRequestJson = objectMapper.writeValueAsString(safeArgs);
        String incomingRequestHash = sha256Hex(incomingRequestJson); // just to make it shorter and consistent in the db, hashing it.
		
		
		Optional<IdempotencyKey> existing = idempotencyKeyRepository
				.findByUserIdAndApiNameAndIdempotencyKey(user.getId(), apiName, idempotencyKey);
		
		// that's why we are storing response body so that is that same request occurs we can return that.
		if (existing.isPresent()) {
			
			IdempotencyKey key = existing.get();
			
			if (key.getExpiresAt().isBefore(LocalDateTime.now())) {
				idempotencyKeyRepository.delete(key);
			} else {
				
				String storedHash = key.getRequestHash();
				
				if (!storedHash.equals(incomingRequestHash)) {
                    // client reused same idempotency key for different request -> reject
                    throw new IdempotencyKeyConflictException(ErrorMessage.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST_BODY);
                }

				
//				to deserialize that stored JSON back into a Java object matching the original method's return type.
//				return objectMapper.readValue(
//						existing.get().getResponseBody(),
//						returnType
//					);
				
				// same request, return cached response (handle generic return types)
				/*
				 * When in the signature the return type is List<SomeDto> ,List<WalletResponseDto>, ResponseEntity<WalletResponseDto>, Map<String, WalletResponseDto>then you won't get that DTO class, you will
				 * get the List.class which is not correct, you want SomeDto.class, this is where this code helps getting the 
				 * appropriate type.
				 * 
				 * We are converting genericReturnType into JavaType because:
				 * We transform Java reflection's representation of a generic type(Type) into Jackson’s internal 
				 * structured type system(JavaType) so that deserialization works correctly for complex and generic 
				 * return types.
				 * */
                MethodSignature mSignature = (MethodSignature) joinPoint.getSignature();
                /*
                 * 
                 * Type genericReturnType = signature.getMethod().getGenericReturnType();
					You get a Java Reflection Type object.
					
					This Type can represent:
						WalletResponseDto
						List<WalletResponseDto>
						ResponseEntity<WalletResponseDto>
						Map<String, List<WalletResponseDto>>
					
					It contains generic information.
					But it is NOT directly usable by Jackson for deserialization.
					It is just Java’s reflection representation.
					
					Why Jackson Cannot Directly Use Type
						Jackson internally does NOT work directly with Java's Type interface.
					
					Jackson uses its own richer abstraction called:JavaType
					
					Why?

					Because JavaType:					
						Stores raw type
						Stores generic parameters
						Stores nested generic parameters
						Stores type metadata
						Is optimized for serialization/deserialization logic
						It is Jackson’s internal type system.
                 * 
                 * */
                Type genericReturnType = mSignature.getMethod().getGenericReturnType(); // returns full generic type info, not just List.class but full List<DTO>.class, Java Reflection Type object, It contains generic information.
                JavaType javaType = objectMapper.getTypeFactory().constructType(genericReturnType); // Typefactory converts genericReturnType into JavaType and preserves it, because in java, generics are erased at the runtime.
                return objectMapper.readValue(existing.get().getResponseBody(), javaType);
				
			}
			
		}
		
		// Proceed (This triggers Retry -> Transaction -> Service)
		// This is where the chain continues into the other interceptors
		/*
		 * Retry interceptor wraps the call

			Each attempt triggers Transaction interceptor
			
			Transaction begins, method runs, commit/rollback happens
			
			If an ObjectOptimisticLockingFailureException occurs, retry runs another attempt (up to 3)
			
			idempotency aspect receives final response
			
			Aspect saves response JSON keyed by (userId, apiName, idempotencyKey)
		 * */
		Object response = joinPoint.proceed();
		
		// you store the requestHash (a fingerprint of request)
//		you store responseBody (the cached response as JSON)
		idempotencyKeyRepository.save(
						IdempotencyKey.builder()
								.userId(user.getId())
								.apiName(apiName)
								.idempotencyKey(idempotencyKey)
								.requestHash(incomingRequestHash)
								.responseBody(objectMapper.writeValueAsString(response)) // saving response as a string only.
								.createdAt(LocalDateTime.now())
								.expiresAt(LocalDateTime.now().plusHours(24))
								.build() 
				);
		
		return response;
		
	}
	
	// Simple helper to compute SHA-256 hex
	/**
	 *MessageDigest -> It's a built-in java class used for cryptographic hashing.
	 *
	 *SHA-256 is a cryptographic hash algorithm that:
		Always produces a 256-bit output
		That is 32 bytes
		Or 64 hex characters
		
		Important Property of SHA-256:
			One-way → cannot reverse hash to original input
			Deterministic → same input = same output
			Collision resistant → very hard for two different inputs to produce same hash
	 * 
	 */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // gives a SHA-256 hashing engine.
            
            /**
             *Convert String to bytes
             *input.getBytes(StandardCharsets.UTF_8) -> Converts text into a byte array using UTF-8 encoding.
             *
             * md.digest(byteArray)
				This:				
					Runs SHA-256 algorithm
					Returns 32-byte array
					Each byte is part of the hash
					
					But raw bytes are not human readable.
					So we convert them to hex.
             */
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8)); // byte[] digest = { -34, 45, 112, 1, ... }
            StringBuilder sb = new StringBuilder(digest.length * 2); // because each byte will become 2 hex characters.
            
            // converting each byte to hex.
            for (byte b : digest) {
            	/*
            	 * Why b & 0xff?
						Java bytes are signed (-128 to 127).
						But we want an unsigned representation (0–255).
						b & 0xff:
							Removes sign extension
							Converts signed byte to positive int representation
							Without it, negative bytes would format incorrectly.
					What does "%02x" mean?
						It’s a format string:
						%x → convert number to hex
						02 → pad with leading zero if needed
						lower-case hex
            	 * 
            	 * */
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString(); // Returns 64-character hex string.
        } catch (Exception e) {
            // fallback: return raw JSON (not ideal) - but this shouldn't happen
            return Integer.toString(input.hashCode());
        }
    }
	
}
