package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.support.constant.MessageEnum;

public interface MemberService {

    /**
     * sns 별 인증 주소 불러오기
     * @param snsType
     * @return
     */
    MemberDomain.MemberOauthPathResponse getOauthPath(MemberEnum.SnsType snsType);

    /**
     * sns 로그인
     * @param request
     * @return
     */
    MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request);

    /**
     * 관심 팀 등록/수정
     * @param request
     */
    void updateTeam(MemberDomain.MemberTeamUpdateRequest request);

    /**
     * 선점한 닉네임 조회
     * @return
     */
    MemberDomain.MemberCheckNickNameResponse checkNick();

    /**
     * 닉네임 중복 검사
     * @param nickNm
     * @return
     */
    MessageEnum.CheckNick checkNickNmDuplicate(String nickNm);

    /**
     * 멤버 정보 수정
     * @param request
     */
    void updateMemberInfo(MemberDomain.MemberInfoUpdateRequest request);

}
