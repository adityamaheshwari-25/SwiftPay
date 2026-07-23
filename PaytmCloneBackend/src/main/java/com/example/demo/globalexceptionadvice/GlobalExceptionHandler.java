package com.example.demo.globalexceptionadvice;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.exception.CustomShareKeysMismatchException;
import com.example.demo.exception.CustomSharesRequiredException;
import com.example.demo.exception.CustomSharesSumMismatchException;
import com.example.demo.exception.EqualSplitNotDivisibleException;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.exception.FileNotUploadedException;
import com.example.demo.exception.IdempotencyKeyConflictException;
import com.example.demo.exception.IdempotencyKeyNotFoundException;
import com.example.demo.exception.InActiveBankAccountException;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.InvalidCustomShareException;
import com.example.demo.exception.InvalidFileTypeException;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.InvalidSplitAmountException;
import com.example.demo.exception.KycAlreadySubmittedException;
import com.example.demo.exception.KycNotApprovedException;
import com.example.demo.exception.KycNotPendingException;
import com.example.demo.exception.LargerFileSizeException;
import com.example.demo.exception.LessAmountException;
import com.example.demo.exception.MembersAreRequiredException;
import com.example.demo.exception.MembersOnlyAllowedException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.NotVerifiedBankAccountException;
import com.example.demo.exception.ReceiverNotFoundException;
import com.example.demo.exception.ReceiverWalletNotFoundException;
import com.example.demo.exception.RejectWithoutReasonException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.SelfTransferNotAllowedException;
import com.example.demo.exception.SenderWalletNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 *@RestControllerAdvice is a specialized component in Spring that provides global exception handling across all @RestControllers.
 *It is basically a combination of:
		@ControllerAdvice
		@ResponseBody 
		
		@ControllerAdvice → Intercepts exceptions across controllers
		@ResponseBody → Automatically converts return value to JSON (via HttpMessageConverters)
		
		Internal Working:
		1. First every request hits the DispatcherServlet. Finds the correct handler (controller method), and invokes it.
		2. Now if any exception occurs, The exception bubbles up to the DispatcherServlet.
		3. DispatcherServlet delegates to HandlerExceptionResolver. HandlerExceptionResolver is an interface.
			which has multiple implementations in the spring boot like ExceptionHandlerExceptionResolver(most imp),
			ResponseStatusExceptionResolver and DefaultHandlerExceptionResolver.
		4. @RestControllerAdvice fits in ExceptionHandlerExceptionResolver.
		5. ExceptionHandlerExceptionResolver, this resolver:
			Scans all beans annotated with @ControllerAdvice
			Caches methods annotated with @ExceptionHandler
			Matches exception type at runtime
			Invokes the correct handler method
		6. During application startup:
			a. Component Scan -> @RestControllerAdvice -> It registers it as a Spring bean.
			b. ExceptionHandlerExceptionResolver scans methods
				It looks for: @ExceptionHandler(SomeException.class)
			c. Example:
				@ExceptionHandler(UserNotFoundException.class)
				public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
				    return ResponseEntity.status(HttpStatus.NOT_FOUND)
				                         .body(ex.getMessage());
				}
				Spring internally builds a mapping like:
				UserNotFoundException → handleUserNotFound()
				
				It uses ExceptionHandlerMethodResolver to map exception types to methods.
				
		7. How JSON Response is Created:
				Return value goes to:		
					HttpMessageConverter
					
					Usually:MappingJackson2HttpMessageConverter
					
					So: Object → JSON
					
		-----------------
		
		Whole flow: 
		When an exception is thrown inside a controller, it propagates to the DispatcherServlet. 
		The DispatcherServlet delegates exception handling to a chain of HandlerExceptionResolvers. 
		One of them is ExceptionHandlerExceptionResolver, which looks for methods annotated with 
		@ExceptionHandler inside @ControllerAdvice or @RestControllerAdvice classes. During startup, 
		Spring scans and caches these mappings. At runtime, it matches the thrown exception to the most 
		specific handler method and invokes it. Since @RestControllerAdvice includes @ResponseBody, 
		the returned object is converted to JSON using HttpMessageConverters and sent as the HTTP response.
		
		------------
		
		@ControllerAdvice returns View, its not used with the REST APIs, whereas @RestControllerAdvice is.
		
		------------
		Some good internal methods to know:
		
		DispatcherServlet
		HandlerExceptionResolver
		ExceptionHandlerExceptionResolver
		ExceptionHandlerMethodResolver
		InvocableHandlerMethod
		ServletInvocableHandlerMethod
		HttpMessageConverter
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	// business exceptions are logged as warn.
	/**
	 *Even though we are catching that exception in the sendToUser method only, but then also, there are actually
	 *many possibilities, I mean different times where client can disconnect and at that time exception can't be handled properly.
	 *So we have added this handler here also for the safety net actually, to prevent accidental JSON writes. 
	 */
	
	// handling multiple exceptions.
	@ExceptionHandler({ org.apache.catalina.connector.ClientAbortException.class,
        org.springframework.web.context.request.async.AsyncRequestNotUsableException.class })
	public void handleSseClientAbort(Exception ex) {
	// expected for SSE disconnects; don't write a response body, it just stops, it does not return anything.
		//It’s basically: “Handled. Do nothing.”
	}

	// getting request info as well using HttpServletRequest.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.warn("Validation Failed at {}: {} errors found", request.getRequestURI(), ex.getBindingResult().getErrorCount());
		FieldError fieldError = ex.getBindingResult()
									.getFieldErrors()
									.get(0);
		return new ErrorResponse(
					HttpStatus.BAD_REQUEST.value(),
					fieldError.getDefaultMessage()
				);
	}
	
	@ExceptionHandler(WalletNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleWalletNotFoundException(WalletNotFoundException ex, HttpServletRequest request) {
		log.warn("Wallet not found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler(UserAlreadyExistsException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpServletRequest request) {
		log.warn("Duplicate Registration at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
		log.warn("Resource Not Found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler(InActiveBankAccountException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInActiveBankAccountException(InActiveBankAccountException ex, HttpServletRequest request) {
		log.warn("Inactive bank account found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler(NotVerifiedBankAccountException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleNotVerifiedBankAccountException(NotVerifiedBankAccountException ex, HttpServletRequest request) {
		log.warn("Bank account not verified at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler(InsufficientBalanceException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInsufficientBalanceException(InsufficientBalanceException ex, HttpServletRequest request) {
		log.warn("Business Exception at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler(UnauthorizedAccessException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleUnauthorizedAccessException(UnauthorizedAccessException ex, HttpServletRequest request) {
		log.warn("Unauthorized access at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}
	
	@ExceptionHandler({
	    OptimisticLockException.class,
	    ObjectOptimisticLockingFailureException.class,
	    OptimisticLockingFailureException.class
	})
	@ResponseStatus(HttpStatus.CONFLICT) // 409 code
	public ErrorResponse handleOptimisticLockingException(Exception ex, HttpServletRequest request) {
		log.error("Optimistic locking failure at {}: All retry attempts exhausted.", request.getRequestURI());
		return new ErrorResponse(
	            HttpStatus.CONFLICT.value(),
	            "The transaction could not be completed because the account balance was updated by another process. Please try again."
	    );
	}
	
	@ExceptionHandler(InvalidMpinException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidMpinException(InvalidMpinException ex, HttpServletRequest request) {
		log.warn("Security Warning at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(MpinNotSetException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMpinNotSetException(MpinNotSetException ex, HttpServletRequest request) {
		log.warn("Mpin not set at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(ReceiverNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleReceiverNotFoundException(ReceiverNotFoundException ex, HttpServletRequest request) {
		log.warn("Receiver not found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(SenderWalletNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleSenderWalletNotFoundException(SenderWalletNotFoundException ex, HttpServletRequest request) {
		log.warn("Sender wallet not found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(ReceiverWalletNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleReceiverWalletNotFoundException(ReceiverWalletNotFoundException ex, HttpServletRequest request) {
		log.warn("Receiver wallet not found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	
	@ExceptionHandler(SelfTransferNotAllowedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleSelfTransferNotAllowedException(SelfTransferNotAllowedException ex, HttpServletRequest request) {
		log.warn("Self transfer blocked at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(FileNotUploadedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleFileNotUploadedException(FileNotUploadedException ex, HttpServletRequest request) {
		log.warn("File not uploaded at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(LargerFileSizeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleLargerFileSizeException(LargerFileSizeException ex, HttpServletRequest request) {
		log.warn("Large file size found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(InvalidFileTypeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidFileTypeException(InvalidFileTypeException ex, HttpServletRequest request) {
		log.warn("Invalid file type found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	
	@ExceptionHandler(KycAlreadySubmittedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleKycAlreadySubmittedException(KycAlreadySubmittedException ex) {
	    return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
		log.warn("Resource Not Found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(KycNotPendingException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleKycNotPendingException(KycNotPendingException ex, HttpServletRequest request) {
		log.warn("Kyc not pending found at {}: {}", request.getRequestURI(), ex.getMessage());
	    return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(RejectWithoutReasonException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleRejectWithoutReasonException(RejectWithoutReasonException ex) {
	    return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(KycNotApprovedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleKycNotApprovedException(KycNotApprovedException ex, HttpServletRequest request) {
		log.warn("Access Blocked (KYC) at {}: {}", request.getRequestURI(), ex.getMessage());
	    return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(IdempotencyKeyNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIdempotencyKeyNotFoundException(IdempotencyKeyNotFoundException ex, HttpServletRequest request) {
		log.warn("Idempotency key not found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(MissingRequestHeaderException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMissingRequestHeaderException(MissingRequestHeaderException ex, HttpServletRequest request) {
		
		log.warn("Missing request header found at {}: {}", request.getRequestURI(), ex.getMessage());
		
		if ("Idempotency-Key".equals(ex.getHeaderName())) {
	        return new ErrorResponse(
	                HttpStatus.BAD_REQUEST.value(),
	                ErrorMessage.MISSING_IDEMPOTENCY_KEY.name()
	        );
	    }
		
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
		log.warn("Access Denied at {}: User lacks required roles", request.getRequestURI());
		return new ErrorResponse(
	            HttpStatus.FORBIDDEN.value(),
	            "You do not have permission to access this resource."
	    );
	}
	
	@ExceptionHandler(LessAmountException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorResponse handleLessAmountException(LessAmountException ex, HttpServletRequest request) {
	    
		log.warn("Less amount found at {}: {}", request.getRequestURI(), ex.getMessage());
		
		return new ErrorResponse(
	            HttpStatus.FORBIDDEN.value(),
	            "You do not have permission to access this resource."
	    );
	}
	
	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
	    log.error("Bad Credentials at {} | Message: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.UNAUTHORIZED.value(),
	            "Invalid email or password"
	    );
	}
	
	
	// this catches all the exception like NullPointer, DB down etc.
	@ExceptionHandler(Exception.class)
	public ErrorResponse handleAllUncaughtException(Exception ex, HttpServletRequest request) {
		// The last argument 'ex' ensures the full stack trace is printed in the logs, and this is given by Logback itself.
		log.error("CRITICAL SYSTEM ERROR at {} | Message: {}", request.getRequestURI(), ex.getMessage(), ex);
		
		return new ErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR.value(), 
				"An internal error occured. Please contact support with Trace Id"
				);
	}
	
	
	@ExceptionHandler(MembersAreRequiredException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMembersAreRequiredException(MembersAreRequiredException ex, HttpServletRequest request) {
		log.warn("Members not found at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(IdempotencyKeyConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleIdempotencyKeyConflictException(IdempotencyKeyConflictException ex, HttpServletRequest request) {
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	
	
	@ExceptionHandler(MembersOnlyAllowedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMembersOnlyAllowedException(MembersOnlyAllowedException ex, HttpServletRequest request) {
		log.warn("Members only allowed at {}: {}", request.getRequestURI(), ex.getMessage());
		return new ErrorResponse(
	            HttpStatus.BAD_REQUEST.value(),
	            ex.getMessage()
	    );
	}
	
	@ExceptionHandler(InvalidSplitAmountException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidSplitAmount(InvalidSplitAmountException ex, HttpServletRequest request) {
	  log.warn("Invalid split amount at {}: {}", request.getRequestURI(), ex.getMessage());
	  return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(EqualSplitNotDivisibleException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleEqualNotDivisible(EqualSplitNotDivisibleException ex, HttpServletRequest request) {
	  log.warn("Equal split not divisible at {}: {}", request.getRequestURI(), ex.getMessage());
	  return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(CustomSharesRequiredException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleCustomSharesRequired(CustomSharesRequiredException ex, HttpServletRequest request) {
	  log.warn("Custom shares required at {}: {}", request.getRequestURI(), ex.getMessage());
	  return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(CustomShareKeysMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleCustomKeysMismatch(CustomShareKeysMismatchException ex, HttpServletRequest request) {
	  log.warn("Custom keys mismatch at {}: {}", request.getRequestURI(), ex.getMessage());
	  return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(CustomSharesSumMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleCustomSumMismatch(CustomSharesSumMismatchException ex, HttpServletRequest request) {
	  log.warn("Custom shares sum mismatch at {}: {}", request.getRequestURI(), ex.getMessage());
	  return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(InvalidCustomShareException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleInvalidCustomShare(InvalidCustomShareException ex, HttpServletRequest request) {
	  log.warn("Invalid custom share at {}: {}", request.getRequestURI(), ex.getMessage());
	  return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	
	
}
