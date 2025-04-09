package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;

public interface MemberService {

    /**
     * sns 별 인증 주소 불러오기
     * @param snsType
     * @return
     */
    String getOauthPath(MemberEnum.SnsType snsType);

    /**
     * sns 로그인
     * @param request
     * @return
     */
    MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request);
}
