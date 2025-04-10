package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.core.api.service.oauth.JwtService;
import kr.co.victoryfairy.core.api.service.oauth.OauthFactory;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.repository.MemberEntityRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberInfoEntityRepository;
import kr.co.victoryfairy.support.utils.RequestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MemberServiceImpl implements MemberService {

    private final OauthFactory oauthFactory;
    private final MemberEntityRepository memberEntityRepository;
    private final MemberInfoEntityRepository memberInfoEntityRepository;
    private final JwtService jwtService;

    public MemberServiceImpl(OauthFactory oauthFactory, MemberEntityRepository memberEntityRepository,
                             MemberInfoEntityRepository memberInfoEntityRepository, JwtService jwtService) {
        this.oauthFactory = oauthFactory;
        this.memberEntityRepository = memberEntityRepository;
        this.memberInfoEntityRepository = memberInfoEntityRepository;
        this.jwtService = jwtService;
    }

    @Override
    public String getOauthPath(MemberEnum.SnsType snsType) {
        var service = oauthFactory.getService(snsType);
        return service.initSnsAuthPath();
    }

    @Override
    @Transactional
    public MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request) {
        // TODO : 코드 정리 필요
        var service = oauthFactory.getService(request.snsType());

        // sns 정보 가져오기
        var memberSns = service.parseSnsInfo(request);

        // sns 정보로 가입된 이력 확인
        var memberInfoEntity = memberInfoEntityRepository.findBySnsTypeAndSnsId(request.snsType(), memberSns.snsId())
                .orElse(null);

        // memberEntity 없을시 회원 가입 처리

        MemberDomain.MemberDto memberDto = null;
        if (memberInfoEntity == null) {
            MemberEntity memberEntity = MemberEntity.builder()
                    .status(MemberEnum.Status.NORMAL)
                    .lastConnectIp(RequestUtils.getRemoteIp())
                    .lastConnectAt(LocalDateTime.now())
                    .build();
            memberEntityRepository.save(memberEntity);            // 멤버 등록
            memberInfoEntity = MemberInfoEntity.builder()
                    .memberEntity(memberEntity)
                    .snsId(memberSns.snsId())
                    .snsType(request.snsType())
                    .email(memberSns.email())
                    .build();
            memberInfoEntityRepository.save(memberInfoEntity);    // 멤버 정보 등록

            var memberInfoDto = MemberDomain.MemberInfoDto.builder()
                    .snsType(request.snsType())
                    .isNickNmAdded(false)
                    .isTeamAdded(false)
                    .build();

            memberDto = MemberDomain.MemberDto.builder()
                    .id(memberEntity.getId())
                    .memberInfo(memberInfoDto)
                    .build();
        }

        // 마지막 로그인 시간, ip 업데이트
        var memberEntity = memberInfoEntity.getMemberEntity();
        memberEntity.updateLastLogin(RequestUtils.getRemoteIp(), LocalDateTime.now());
        memberEntityRepository.save(memberEntity);

        var teamEntity = memberInfoEntity.getTeamEntity();
        var memberInfoDto = MemberDomain.MemberInfoDto.builder()
                .snsType(request.snsType())
                .isNickNmAdded(StringUtils.hasText(memberInfoEntity.getNickNm()))
                .isTeamAdded(teamEntity != null)
                .build();
        memberDto = MemberDomain.MemberDto.builder()
                .id(memberEntity.getId())
                .memberInfo(memberInfoDto)
                .build();

        var accessTokenDto = jwtService.makeAccessToken(memberDto);
        var memberInfo = new MemberDomain.MemberInfoResponse(request.snsType(), memberSns.snsId(), memberInfoDto.getIsNickNmAdded(), memberInfoDto.getIsTeamAdded());
        var memberLoginResponse = new MemberDomain.MemberLoginResponse(memberInfo, accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
        return memberLoginResponse;
    }
}