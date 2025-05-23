package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.CommonDomain;

import java.util.List;

public interface CommonService {

    List<CommonDomain.TeamListResponse> findAll();

    List<CommonDomain.SeatListResponse> findSeat(Long id, String season);
}
