package kr.co.victoryfairy.core.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NickNameInfo {
    private String key;
    private LocalDateTime createdAt;
}
