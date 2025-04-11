package kr.co.victoryfairy.core.api.service.impl;

import kr.co.victoryfairy.core.api.domain.TeamDomain;
import kr.co.victoryfairy.core.api.service.TeamService;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.TeamEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamEntityRepository teamEntityRepository;

    @Override
    public List<TeamDomain.TeamListResponse> findAll() {
        List<TeamEntity> team = teamEntityRepository.findAll();
        return team.stream()
                .map(entity -> {
                    return new TeamDomain.TeamListResponse(entity.getId(), entity.getName());
                }).toList();
    }
}
