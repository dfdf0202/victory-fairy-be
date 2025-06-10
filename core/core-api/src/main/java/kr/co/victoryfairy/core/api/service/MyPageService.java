package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.MyPageDomain;

public interface MyPageService {

    /**
     * 마이페이지 - 회원 정보 조회
     * @return
     */
    MyPageDomain.MemberInfoForMyPageResponse findMemberInfoForMyPage();

    /**
     * 마이페이지 - 승요 파워 조회
     * @return
     */
    MyPageDomain.VictoryPowerResponse findVictoryPower();
}
