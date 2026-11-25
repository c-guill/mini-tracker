package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.IssueLinkType;

public record IssueLinkResponse(
        Long id,
        Long targetTicketId,
        IssueLinkType type
) {
}
