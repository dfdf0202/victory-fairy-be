package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.model.NickNameInfo;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.core.api.service.oauth.JwtService;
import kr.co.victoryfairy.core.api.service.oauth.OauthFactory;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberInfoRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final OauthFactory oauthFactory;
    private final MemberRepository memberRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final TeamRepository teamRepository;
    private final JwtService jwtService;
    private final RedisHandler redisHandler;

    @Override
    public MemberDomain.MemberOauthPathResponse getOauthPath(MemberEnum.SnsType snsType) {
        var service = oauthFactory.getService(snsType);
        var response = service.initSnsAuthPath();
        return new MemberDomain.MemberOauthPathResponse(response);
    }

    @Override
    @Transactional
    public MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request) {
        // TODO : 코드 정리 필요
        var service = oauthFactory.getService(request.snsType());

        // sns 정보 가져오기
        var memberSns = service.parseSnsInfo(request);

        // sns 정보로 가입된 이력 확인
        var memberInfoEntity = memberInfoRepository.findBySnsTypeAndSnsId(request.snsType(), memberSns.snsId())
                .orElse(null);

        // memberEntity 없을시 회원 가입 처리

        MemberDomain.MemberDto memberDto = null;
        if (memberInfoEntity == null) {
            MemberEntity memberEntity = MemberEntity.builder()
                    .status(MemberEnum.Status.NORMAL)
                    .lastConnectIp(RequestUtils.getRemoteIp())
                    .lastConnectAt(LocalDateTime.now())
                    .build();
            memberRepository.save(memberEntity);            // 멤버 등록
            memberInfoEntity = MemberInfoEntity.builder()
                    .memberEntity(memberEntity)
                    .snsId(memberSns.snsId())
                    .snsType(request.snsType())
                    .email(memberSns.email())
                    .build();
            memberInfoRepository.save(memberInfoEntity);    // 멤버 정보 등록

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
        memberRepository.save(memberEntity);

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

    @Override
    public void updateTeam(MemberDomain.MemberTeamUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberInfoEntity = memberInfoRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoEntity = memberInfoEntity.toBuilder()
                .teamEntity(teamEntity)
                .build();

        memberInfoRepository.save(memberInfoEntity);
    }

    @Override
    public MemberDomain.MemberCheckNickNameResponse checkNick() {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var myNick = redisHandler.getHashValue("memberNickNm", String.valueOf(id), NickNameInfo.class);

        if (myNick == null) {
            return new MemberDomain.MemberCheckNickNameResponse(null);
        }

        return new MemberDomain.MemberCheckNickNameResponse(myNick.getKey());
    }

    @Override
    public MessageEnum.CheckNick checkNickNmDuplicate(String nickNm) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        // Redis 에 저장된 nickNm 이 있는지 체크
        var existingNick = redisHandler.getHashValue("checkNick", nickNm, NickNameInfo.class);

        if (existingNick != null) {
            var now = LocalDateTime.now();
            // 선점된지 72시간 이내의 nickNm 인지 체크
            if (Duration.between(existingNick.getCreatedAt(), now).toHours() < 72) {
                return MessageEnum.CheckNick.DUPLICATE;
            }

            // 72시간이 지난 경우 만료로 보고 삭제
            redisHandler.deleteHashValue("checkNick", nickNm);
            redisHandler.deleteHashValue("memberNickNm", String.valueOf(existingNick.getKey()));
        }

        // 이미 DB 에 저장된 닉네임인지 체크
        if (memberInfoRepository.findByNickNm(nickNm).isPresent()) {
            return MessageEnum.CheckNick.DUPLICATE;
        }

        // 이미 선점한 닉네임이 있을 경우 삭제 처리
        var myNick = redisHandler.getHashValue("memberNickNm", String.valueOf(id));
        if (StringUtils.hasText(myNick)) {
            redisHandler.deleteHashValue("memberNickNm", String.valueOf(id));
            redisHandler.deleteHashValue("checkNick", myNick);
        }

        var info = NickNameInfo.builder()
                .key(String.valueOf(id))
                .createdAt(LocalDateTime.now())
                .build();

        var myNickInfo = NickNameInfo.builder()
                .key(nickNm)
                .createdAt(LocalDateTime.now())
                .build();

        redisHandler.setHashValue("checkNick", nickNm, info);
        redisHandler.setHashValue("memberNickNm", String.valueOf(id), myNickInfo);

        return MessageEnum.CheckNick.AVAILABLE;
    }

    @Override
    public void updateMemberInfo(MemberDomain.MemberInfoUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        // TODO file id 로 이미지 path 저장 처리

        var existingNick = redisHandler.getHashValue("checkNick", request.nickNm(), NickNameInfo.class);
        if (existingNick == null) {
            throw new CustomException(MessageEnum.CheckNick.NON_CHECK);
        }
        if (!Long.valueOf(existingNick.getKey()).equals(id)) {
            throw new CustomException(MessageEnum.CheckNick.POSSESSION);
        }


        var memberInfoEntity = memberInfoRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoEntity = memberInfoEntity.toBuilder()
                .nickNm(request.nickNm())
                .build();

        memberInfoRepository.save(memberInfoEntity);

        // Redis 에 저장된 데이터 삭제 처리
        redisHandler.deleteHashValue("checkNick", request.nickNm());
        redisHandler.deleteHashValue("memberNickNm", String.valueOf(id));
    }
}