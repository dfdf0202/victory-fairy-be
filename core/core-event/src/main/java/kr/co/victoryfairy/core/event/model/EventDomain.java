package kr.co.victoryfairy.core.event.model;

import io.dodn.springboot.core.enums.EventType;

public interface EventDomain {

    record WriteEventDto(
            String gameId,
            Long memberId,
            Long diaryId,
            EventType type
    ) {}

}
