package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import io.dodn.springboot.core.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                            // 회원 식별자

    @Column(name = "create_at")
    private LocalDateTime createAt;             // 생성 시간

    @Column(name = "last_connect_at")
    private LocalDateTime lastConnectAt;        // 마지막 접속 시간

    @Column(name = "last_connect_ip")
    private String lastConnectIp;               // 마지막 접속 ip

    private MemberStatus status;                // 회원 상태

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;            // 수정된 시간

}
