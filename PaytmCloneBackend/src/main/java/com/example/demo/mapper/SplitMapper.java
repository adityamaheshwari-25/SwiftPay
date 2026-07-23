package com.example.demo.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.dto.SplitCreatedListItemDto;
import com.example.demo.dto.SplitDetailsResponseDto;
import com.example.demo.dto.SplitInvolvedListItemDto;
import com.example.demo.dto.SplitParticipantDto;
import com.example.demo.entity.SplitParticipant;
import com.example.demo.entity.SplitRequest;

@Component
public class SplitMapper {

    /**
     * Entity -> Details DTO
     */
    public SplitDetailsResponseDto toDetailsDto(SplitRequest sr) {
        if (sr == null) return null;

        List<SplitParticipantDto> participants = sr.getParticipants() == null
                ? List.of()
                : sr.getParticipants().stream()
                    .map(this::toParticipantDto)
                    .toList();

        return new SplitDetailsResponseDto(
                sr.getId(),
                sr.getSplitCode(),
                sr.getInitiator() != null ? sr.getInitiator().getId() : null,
                sr.getTotalAmount(),
                sr.getSplitType(),
                sr.getStatus(),
                sr.getNote(),
                sr.getCreatedAt(),
                participants
        );
    }

    private SplitParticipantDto toParticipantDto(SplitParticipant p) {
        if (p == null) return null;

        return new SplitParticipantDto(
                p.getParticipant() != null ? p.getParticipant().getId() : null,
                p.getParticipant() != null ? p.getParticipant().getName() : null,
                p.getParticipant() != null ? p.getParticipant().getMobile() : null,
                p.getShareAmount(),
                p.getState(),
                p.getPaidTransaction() != null ? p.getPaidTransaction().getTxId() : null,
                p.getPaidAt()
        );
    }

    /**
     * Entity -> Created list item DTO
     * (totalParticipants + paidParticipants are computed in service or repo)
     */
    public SplitCreatedListItemDto toCreatedListItem(SplitRequest sr, int totalParticipants, int paidParticipants) {
        if (sr == null) return null;

        return new SplitCreatedListItemDto(
                sr.getId(),
                sr.getSplitCode(),
                sr.getTotalAmount(),
                sr.getStatus(),
                sr.getNote(),
                sr.getCreatedAt(),
                totalParticipants,
                paidParticipants
        );
    }

    /**
     * Participant row -> Involved list item DTO
     * (uses splitRequest + initiator info)
     */
    public SplitInvolvedListItemDto toInvolvedListItem(SplitParticipant sp) {
        if (sp == null || sp.getSplitRequest() == null) return null;

        SplitRequest sr = sp.getSplitRequest();

        return new SplitInvolvedListItemDto(
                sr.getId(),
                sr.getSplitCode(),
                sr.getInitiator() != null ? sr.getInitiator().getId() : null,
                sr.getInitiator() != null ? sr.getInitiator().getName() : null,
                sr.getTotalAmount(),
                sp.getShareAmount(),      // myShare
                sp.getState(),            // myState
                sr.getStatus(),
                sr.getNote(),
                sr.getCreatedAt()
        );
    }
}
