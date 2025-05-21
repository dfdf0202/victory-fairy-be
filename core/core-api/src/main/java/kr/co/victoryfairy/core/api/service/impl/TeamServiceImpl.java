package kr.co.victoryfairy.core.api.service.impl;

import kr.co.victoryfairy.core.api.domain.TeamDomain;
import kr.co.victoryfairy.core.api.service.TeamService;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;

    @Override
    public List<TeamDomain.TeamListResponse> findAll() {
        List<TeamEntity> team = teamRepository.findAll();
        return team.stream()
                .map(entity -> {
                    return new TeamDomain.TeamListResponse(entity.getId(), entity.getName(), entity.getLabel());
                }).toList();
    }
}
