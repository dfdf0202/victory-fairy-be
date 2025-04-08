package kr.co.victoryfairy.storage.db.core.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDto {

    private String name;            // 함께한 사람 이름
    private String teamName;        // 함께한 사람의 응원 팀


}
