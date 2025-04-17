package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.TeamDomain;

import java.util.List;

public interface TeamService {

    List<TeamDomain.TeamListResponse> findAll();

}
