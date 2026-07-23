package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.customAnnotation.Idempotent;
import com.example.demo.dto.SplitCreateRequestDto;
import com.example.demo.dto.SplitCreatedListItemDto;
import com.example.demo.dto.SplitDetailsResponseDto;
import com.example.demo.dto.SplitInvolvedListItemDto;
import com.example.demo.dto.SplitParticipantDto;
import com.example.demo.dto.SplitPayRequestDto;
import com.example.demo.dto.SplitPayResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.SplitParticipant;
import com.example.demo.entity.SplitRequest;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.Role;
import com.example.demo.entity.enums.SplitParticipantState;
import com.example.demo.entity.enums.SplitStatus;
import com.example.demo.entity.enums.SplitType;
import com.example.demo.exception.CustomShareKeysMismatchException;
import com.example.demo.exception.CustomSharesRequiredException;
import com.example.demo.exception.CustomSharesSumMismatchException;
import com.example.demo.exception.EqualSplitNotDivisibleException;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.InvalidCustomShareException;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.InvalidSplitAmountException;
import com.example.demo.exception.KycNotApprovedException;
import com.example.demo.exception.MembersAreRequiredException;
import com.example.demo.exception.MembersOnlyAllowedException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.ReceiverNotFoundException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.mapper.SplitMapper;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.SplitParticipantRespository;
import com.example.demo.repository.SplitRequestRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.KycValidationService;
import com.example.demo.service.SplitService;
import com.example.demo.service.SseEventService;
import com.example.demo.service.TransactionService;
import com.example.demo.validation.WalletAmountValidator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SplitServiceImpl implements SplitService{

    private final BCryptPasswordEncoder passwordEncoder;
	
	private final CurrentUserService currentUserService;
	private final KycValidationService kycValidationService;
	private final AppUserRepository appUserRepository;
	private final SplitRequestRepository splitRequestRepository;
	private final SseEventService sseEventService;
	private final SplitParticipantRespository splitParticipantRespository;
	private final WalletRepository walletRepository;
	private final WalletAmountValidator walletAmountValidator;
	private final TransactionFactory transactionFactory;
	private final TransactionService transactionService;
	private final SplitMapper splitMapper;

	
	@Override
	@Idempotent(api = "CREATE_SPLIT")
	@Transactional
	public SplitDetailsResponseDto createSplit(
	        Authentication auth,
	        SplitCreateRequestDto dto
	) {
		
		AppUser initiator = currentUserService.getCurrentUser(auth);
        kycValidationService.ensureKycApproved(initiator);

        validateCreateRequest(dto);

        SplitType splitType = dto.getSplitType() != null ? dto.getSplitType() : SplitType.EQUAL;

        List<AppUser> members = resolveMembers(dto.getMemberMobiles());
        validateMembersOnlyUsers(members);

        BigDecimal total = dto.getAmount();
        long totalPaise = toPaiseStrict(total);

        Map<String, Long> sharePaiseByMobile = computeSharesPaise(dto, splitType, totalPaise);

        SplitRequest sr = new SplitRequest();
        sr.setInitiator(initiator);
        sr.setTotalAmount(total);
        sr.setNote(dto.getNote());
        sr.setStatus(SplitStatus.OPEN);
        sr.setSplitType(splitType);

        attachParticipants(sr, members, sharePaiseByMobile);

        SplitRequest saved = splitRequestRepository.save(sr);

        notifySplitCreated(saved);

        return splitMapper.toDetailsDto(saved);
	}
	
    private void validateCreateRequest(SplitCreateRequestDto dto) {
        if (dto == null) throw new IllegalArgumentException("Request body is required");

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        	throw new InvalidSplitAmountException(ErrorMessage.SPLIT_AMOUNT_INVALID);
        }
        if (dto.getAmount().scale() > 2) {
        	throw new InvalidSplitAmountException(ErrorMessage.SPLIT_AMOUNT_TOO_MANY_DECIMALS);
        }
        if (dto.getMemberMobiles() == null || dto.getMemberMobiles().isEmpty()) {
            throw new MembersAreRequiredException(ErrorMessage.MEMBERS_ARE_REQUIRED);
        }
    }
    
    private List<AppUser> resolveMembers(List<String> memberMobiles) {
        return memberMobiles.stream()
                .map(mobile -> appUserRepository.findByMobile(mobile)
                        .orElseThrow(() -> new ReceiverNotFoundException(ErrorMessage.RECEIVER_NOT_FOUND)))
                .collect(Collectors.toList());
    }
    
    private void validateMembersOnlyUsers(List<AppUser> members) {
        for (AppUser u : members) {
            if (u.getRole() == Role.MERCHANT) {
                throw new MembersOnlyAllowedException(ErrorMessage.MEMBERS_ARE_ONLY_ALLOWED_NO_MERCHANTS);
            }
        }
    }
    
    private long toPaiseStrict(BigDecimal amount) {
    	  if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
    	    throw new InvalidSplitAmountException(ErrorMessage.SPLIT_AMOUNT_INVALID);
    	  }

    	  // Prevent ArithmeticException from UNNECESSARY rounding
    	  if (amount.scale() > 2) {
    	    throw new InvalidSplitAmountException(ErrorMessage.SPLIT_AMOUNT_TOO_MANY_DECIMALS);
    	  }

    	  // Now safe: multiplying by 100 keeps it integral
    	  return amount.movePointRight(2).longValueExact();
    }

    
    private Map<String, Long> computeSharesPaise(SplitCreateRequestDto dto, SplitType splitType, long totalPaise) {
        Map<String, Long> sharePaiseByMobile = new HashMap<>();
        int n = dto.getMemberMobiles().size();

        if (splitType == SplitType.EQUAL) {
            if (totalPaise % n != 0) {
            	throw new EqualSplitNotDivisibleException(ErrorMessage.SPLIT_EQUAL_NOT_DIVISIBLE);
            }
            long each = totalPaise / n;
            for (String mobile : dto.getMemberMobiles()) {
                sharePaiseByMobile.put(mobile, each);
            }
            return sharePaiseByMobile;
        }

        if (splitType == SplitType.CUSTOM) {
            Map<String, BigDecimal> customShares = dto.getCustomShares();
            if (customShares == null || customShares.isEmpty()) {
            	throw new CustomSharesRequiredException(ErrorMessage.SPLIT_CUSTOM_SHARES_REQUIRED);
            }

            // validate keys match
            Set<String> memberSet = new HashSet<>(dto.getMemberMobiles());
            Set<String> keySet = new HashSet<>(customShares.keySet());
            if (!memberSet.equals(keySet)) {
            	throw new CustomShareKeysMismatchException(ErrorMessage.SPLIT_CUSTOM_KEYS_MISMATCH);
            }

            long sum = 0L;
            for (String mobile : dto.getMemberMobiles()) {
                BigDecimal share = customShares.get(mobile);

                if (share == null) throw new InvalidCustomShareException(ErrorMessage.SPLIT_CUSTOM_SHARE_INVALID);
                if (share.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidCustomShareException(ErrorMessage.SPLIT_CUSTOM_SHARE_INVALID);
                if (share.scale() > 2) throw new InvalidCustomShareException(ErrorMessage.SPLIT_CUSTOM_SHARE_INVALID);

                long sharePaise = toPaiseStrict(share);
                sharePaiseByMobile.put(mobile, sharePaise);
                sum += sharePaise;
            }

            if (sum != totalPaise) {
            	throw new CustomSharesSumMismatchException(ErrorMessage.SPLIT_CUSTOM_SUM_MISMATCH);
            }

            return sharePaiseByMobile;
        }

        throw new IllegalArgumentException("Unsupported splitType: " + splitType);
    }
    
    private void attachParticipants(SplitRequest sr, List<AppUser> members, Map<String, Long> sharePaiseByMobile) {
        for (AppUser member : members) {
            Long paise = sharePaiseByMobile.get(member.getMobile());
            if (paise == null) {
                throw new IllegalStateException("Share not found for " + member.getMobile());
            }

            BigDecimal shareAmount = BigDecimal.valueOf(paise)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);

            SplitParticipant sp = new SplitParticipant();
            sp.setSplitRequest(sr);
            sp.setParticipant(member);
            sp.setShareAmount(shareAmount);
            sp.setState(SplitParticipantState.PENDING);

            sr.getParticipants().add(sp);
        }
    }
    
    private void notifySplitCreated(SplitRequest saved) {
        // notify participants
        saved.getParticipants().forEach(p ->
                sseEventService.sendToUser(
                        p.getParticipant().getId(),
                        "split.created",
                        Map.of("splitId", saved.getId())
                )
        );
        // notify initiator
        sseEventService.sendToUser(saved.getInitiator().getId(), "split.created", Map.of("splitId", saved.getId()));
    }

	@Override
	public SplitDetailsResponseDto getSplit(Authentication auth, Long splitId) {
		AppUser user = currentUserService.getCurrentUser(auth);
		
		SplitRequest sr = splitRequestRepository.findById(splitId)
	            .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.SPLIT_NOT_FOUND));
		
		// basic authorization: initiator OR participant can view
        boolean allowed = sr.getInitiator().getId().equals(user.getId()) ||
                sr.getParticipants().stream().anyMatch(p -> p.getParticipant().getId().equals(user.getId()));
		  
        if (!allowed) throw new ResourceNotFoundException(ErrorMessage.YOU_ARE_NOT_A_PART_OF_THIS_SPLIT);
        
        return splitMapper.toDetailsDto(sr);
	}
	
	

	@Override
    @Idempotent(api = "SPLIT_PAY")
    @Transactional
    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
	public SplitPayResponseDto payShare(Authentication auth, Long splitId, SplitPayRequestDto dto) throws KycNotApprovedException, ResourceNotFoundException, WalletNotFoundException, MpinNotSetException, InvalidMpinException, UserNotFoundException, InsufficientBalanceException {
		
		AppUser payer = currentUserService.getCurrentUser(auth);
		kycValidationService.ensureKycApproved(payer);
		
		validateMpin(payer, dto);
		
		
		SplitRequest sr = splitRequestRepository.findById(splitId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.SPLIT_NOT_FOUND));
		
		SplitParticipant sp = splitParticipantRespository
				.findBySplitRequest_IdAndParticipant_Id(splitId, payer.getId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.YOU_ARE_NOT_A_PART_OF_THIS_SPLIT));
		
		if (sp.getState() == SplitParticipantState.PAID) {
			String existingTxId = sp.getPaidTransaction().getTxId();
			return new SplitPayResponseDto(splitId, existingTxId, sp.getShareAmount(), existingTxId);
		}
		
		// by chance by some means if the wallet is not created, just a check.
        Wallet payerWallet = walletRepository.findByUser(payer)
            .orElseThrow(() -> new WalletNotFoundException(ErrorMessage.WALLET_NOT_FOUND));

        Wallet initiatorWallet = walletRepository.findByUser(sr.getInitiator())
            .orElseThrow(() -> new WalletNotFoundException(ErrorMessage.WALLET_NOT_FOUND));
        
        BigDecimal amount = sp.getShareAmount();
        walletAmountValidator.validateDebit(payerWallet, amount);
        
        // wallet transactions, so optimistic locking happens here also.
        payerWallet.setBalance(payerWallet.getBalance().subtract(amount));
        initiatorWallet.setBalance(initiatorWallet.getBalance().add(amount));
        
        // creating transaction
        String splitReference = "SPLIT:" + splitId;
        Transaction tx = transactionFactory.createSplitPaymentTransaction(payerWallet, initiatorWallet, amount, splitReference);
        transactionService.save(tx);
        
        // marking participant paid now
        sp.setState(SplitParticipantState.PAID);
        sp.setPaidTransaction(tx);
        sp.setPaidAt(LocalDateTime.now());
        splitParticipantRespository.save(sp);// @Version protects double-pay race
        
        
        // update request status
        refreshSplitStatus(sr);
        
        // notify SSE
        notifySplitUpdated(sr.getId(), sr);
        
        return new SplitPayResponseDto(splitId, tx.getTxId(), amount, "Payment successful");

	}
	
    private void validateMpin(AppUser payer, SplitPayRequestDto dto) {
        if (!payer.isMpinSet()) throw new MpinNotSetException(ErrorMessage.SET_MPIN_BEFORE_WITHDRAWAL);
        if (dto == null || dto.getMpin() == null) throw new InvalidMpinException(ErrorMessage.INVALID_MPIN);
        if (!passwordEncoder.matches(dto.getMpin(), payer.getMpin())) throw new InvalidMpinException(ErrorMessage.INVALID_MPIN);
    }
    
    private void refreshSplitStatus(SplitRequest sr) {
        long splitId = sr.getId();
        long total = splitParticipantRespository.countBySplitRequest_Id(splitId);
        long paid = splitParticipantRespository.countBySplitRequest_IdAndState(splitId, SplitParticipantState.PAID);

        if (paid == total) sr.setStatus(SplitStatus.COMPLETE);
        else if (paid > 0) sr.setStatus(SplitStatus.PARTIALLY_PAID);
        else sr.setStatus(SplitStatus.OPEN);

        splitRequestRepository.save(sr);
    }
    
    private void notifySplitUpdated(Long splitId, SplitRequest sr) {
        sseEventService.sendToUser(sr.getInitiator().getId(), "split.updated", Map.of("splitId", splitId));
        sr.getParticipants().forEach(p ->
                sseEventService.sendToUser(p.getParticipant().getId(), "split.updated", Map.of("splitId", splitId))
        );
    }
	
	@Override
	@Transactional
	public List<SplitCreatedListItemDto> listCreated(Authentication auth) throws UserNotFoundException {
	    AppUser me = currentUserService.getCurrentUser(auth);

	    List<SplitRequest> requests = splitRequestRepository
	            .findByInitiator_IdOrderByCreatedAtDesc(me.getId());

	    return requests.stream()
                .map(sr -> {
                    int total = sr.getParticipants() != null ? sr.getParticipants().size() : 0;
                    int paid = (int) sr.getParticipants().stream().filter(p -> p.getState() == SplitParticipantState.PAID).count();
                    return splitMapper.toCreatedListItem(sr, total, paid);
                })
                .toList();
	}
	
	@Override
	@Transactional
	public List<SplitInvolvedListItemDto> listInvolved(Authentication auth) {
	    AppUser me = currentUserService.getCurrentUser(auth);

	    // each row is my participant record, with splitRequest + initiator fetched
	    List<SplitParticipant> mine = splitParticipantRespository.findAllForUser(me.getId());

	    // show only splits I still need to act on (best UX)
	    return mine.stream()
                .filter(sp -> sp.getState() == SplitParticipantState.PENDING)
                .filter(sp -> {
                    SplitStatus st = sp.getSplitRequest().getStatus();
                    return st != SplitStatus.COMPLETE && st != SplitStatus.CANCELLED && st != SplitStatus.EXPIRED;
                })
                .map(splitMapper::toInvolvedListItem)
                .toList();
	}
	
}
