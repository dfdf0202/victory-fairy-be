package kr.co.victoryfairy.core.api.service.impl;

import kr.co.victoryfairy.core.api.domain.CommonDomain;
import kr.co.victoryfairy.core.api.service.CommonService;
import kr.co.victoryfairy.storage.db.core.entity.SeatEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.SeatRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {

    private final TeamRepository teamRepository;
    private final SeatRepository seatRepository;

    @Override
    public List<CommonDomain.TeamListResponse> findAll() {
        List<TeamEntity> team = teamRepository.findAll();
        return team.stream()
                .sorted(Comparator.comparing(TeamEntity::getOrderNo))
                .map(entity -> {
                    return new CommonDomain.TeamListResponse(entity.getId(), entity.getName(), entity.getLabel());
                })
                .toList();
    }

    @Override
    public List<CommonDomain.SeatListResponse> findSeat(Long id, String season) {
        List<SeatEntity> seatEntities = seatRepository.findByStadiumEntityIdAndSeason(id, season);
        if (seatEntities.isEmpty()) {
            return new ArrayList<>();
        }

        return seatEntities.stream()
                .map(entity -> new CommonDomain.SeatListResponse(entity.getId(), entity.getName()))
                .toList();
    }
}
