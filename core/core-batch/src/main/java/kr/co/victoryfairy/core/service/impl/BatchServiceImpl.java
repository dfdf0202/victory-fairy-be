package kr.co.victoryfairy.core.service.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.service.BatchService;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.HitterRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.PitcherRecordEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchEntityCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchEntityRepository;
import kr.co.victoryfairy.storage.db.core.repository.HitterRecordEntityRepository;
import kr.co.victoryfairy.storage.db.core.repository.PitcherRecordEntityRepository;
import kr.co.victoryfairy.support.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.SlackUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BatchServiceImpl implements BatchService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GameMatchEntityRepository gameMatchEntityRepository;
    private final GameMatchEntityCustomRepository gameMatchEntityCustomRepository;
    private final HitterRecordEntityRepository hitterRecordEntityRepository;
    private final PitcherRecordEntityRepository pitcherRecordEntityRepository;

    private final RedisHandler redisHandler;
    private final SlackUtils slackUtils;

    public BatchServiceImpl(GameMatchEntityRepository gameMatchEntityRepository,
                            GameMatchEntityCustomRepository gameMatchEntityCustomRepository,
                            HitterRecordEntityRepository hitterRecordEntityRepository,
                            PitcherRecordEntityRepository pitcherRecordEntityRepository,
                            RedisHandler redisHandler, SlackUtils slackUtils) {
        this.gameMatchEntityRepository = gameMatchEntityRepository;
        this.gameMatchEntityCustomRepository = gameMatchEntityCustomRepository;
        this.hitterRecordEntityRepository = hitterRecordEntityRepository;
        this.pitcherRecordEntityRepository = pitcherRecordEntityRepository;
        this.redisHandler = redisHandler;
        this.slackUtils = slackUtils;
    }

    @Override
    @Transactional
    public void batchScore() {
        logger.info("========== Batch  Start ==========");

        var id = "";
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://m.koreabaseball.com/Kbo/Schedule.aspx");

            page.waitForSelector("ul#now");
            List<ElementHandle> gameElements = page.querySelectorAll("ul#now > li.list");

            String dateText = page.querySelector("#lblGameDate").innerText();
            String formattedDate = dateText.replaceAll("[^0-9]", "");

            if (gameElements.isEmpty()) return;

            for (ElementHandle game : gameElements) {
                String classAttr = game.getAttribute("class");
                String statusClass = classAttr.replace("list", "").trim();

                // 경기 예정인 경우 제외
                if (!StringUtils.hasText(statusClass)) continue;


                // 상태, 취소 사유 처리
                var matchStatus = MatchEnum.MatchStatus.PROGRESS;
                var reason = "";
                if ("end".equals(statusClass)) {
                    matchStatus = MatchEnum.MatchStatus.END;
                    reason  = game.querySelector(".bottom ul li a").innerText();
                } else if ("cancel".equals(statusClass)) {
                    matchStatus = MatchEnum.MatchStatus.CANCELED;
                } else if ("ing".equals(statusClass)) {
                    matchStatus = MatchEnum.MatchStatus.PROGRESS;
                }

                // 더블헤더 여부 처리
                var matchOrder = 0;
                ElementHandle dhSpan = game.querySelector("span.dh");
                if (dhSpan != null) {
                    matchOrder = dhSpan.innerText().equals("DH1") ? 1 : 2;
                }

                // 어웨이 팀명 (왼쪽 엠블럼)
                String awaySrc = game.querySelector(".emb.txt-r img").getAttribute("src");
                String awayTeamName = "";
                Matcher awayMatcher = Pattern.compile("emblem_(\\w+)\\.png").matcher(awaySrc);
                if (awayMatcher.find()) {
                    awayTeamName = awayMatcher.group(1);
                }

                // 홈 팀명 (오른쪽 엠블럼)
                String homeSrc = game.querySelector(".emb:not(.txt-r) img").getAttribute("src");
                String homeTeamName = "";
                Matcher homeMatcher = Pattern.compile("emblem_(\\w+)\\.png").matcher(homeSrc);
                if (homeMatcher.find()) {
                    homeTeamName = homeMatcher.group(1);
                }

                // 어웨이 점수
                var awayScoreSpan = game.querySelector(".team.away .score");
                String awayScore = awayScoreSpan != null ? awayScoreSpan.innerText() : null;
                // 홈 점수
                var homeScoreSpn = game.querySelector(".team.home .score");
                String homeScore = homeScoreSpn != null ? homeScoreSpn.innerText() : null;

                id = formattedDate + awayTeamName + homeTeamName + matchOrder;
                var matchEntity = gameMatchEntityRepository.findById(id).orElse(null);


                // Redis 저장 처리
                if (matchStatus.equals(MatchEnum.MatchStatus.PROGRESS)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("awayImage", awaySrc);
                    map.put("awayScore", awayScore);
                    map.put("homeImage", homeSrc);
                    map.put("homeScore", homeScore);
                    redisHandler.setMap(id, map);
                } else {
                    redisHandler.deleteMap(id);
                }

                if (matchEntity == null) return;

                // 진행 예정인 대회 진행중 처리
                if (matchEntity.getStatus().equals(MatchEnum.MatchStatus.READY)) {

                    if (matchStatus.equals(MatchEnum.MatchStatus.CANCELED)) {
                        matchEntity = matchEntity.toBuilder()
                                .reason(reason)
                                .status(matchStatus)
                                .build();
                    } else {
                        matchEntity = matchEntity.toBuilder()
                                .status(matchStatus)
                                .build();
                    }
                    gameMatchEntityRepository.save(matchEntity);
                }
                // 진행 중인 경기의 최종 상태 변경
                if (matchEntity.getStatus().equals(MatchEnum.MatchStatus.PROGRESS)) {
                    switch (matchStatus) {
                        case END -> {
                            matchEntity = matchEntity.toBuilder()
                                            .awayScore(Short.parseShort(awayScore))
                                            .homeScore(Short.parseShort(homeScore))
                                            .status(matchStatus)
                                            .build();
                            gameMatchEntityRepository.save(matchEntity);
                            redisHandler.deleteMap(id);
                        }
                        case CANCELED -> {
                            matchEntity = matchEntity.toBuilder()
                                    .reason(reason)
                                    .status(matchStatus)
                                    .build();
                            gameMatchEntityRepository.save(matchEntity);
                            redisHandler.deleteMap(id);
                        }
                    }
                }
            }

            page.close(); // 안전하게 닫기
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
            slackUtils.message(id + " 점수 불러오는 중 에러 발생");
        }
    }

    @Override
    @Transactional
    public void batchMatchInfo() {
        logger.info("========== Match Info Craw  Start ==========");
        // 오늘 일자 경기 목록 조회
        var now = LocalDate.now();
        var matches = gameMatchEntityCustomRepository.findByMatchAt(now);

        if (matches.isEmpty()) {
            return;
        }

        // status 가 end, isMatchInfoCraw 가 false or null 인 요소
        matches = matches.stream()
                .filter(entity -> entity.getStatus().equals(MatchEnum.MatchStatus.END) &&
                        (entity.getIsMatchInfoCraw() == null || !entity.getIsMatchInfoCraw()))
                .toList();

        // id 로 match detail 조회
        List<HitterRecordEntity> hitterEntities = new ArrayList<>();
        List<PitcherRecordEntity> pitcherEntities = new ArrayList<>();
        matches.forEach(entity -> {
            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch();
                Page page = browser.newPage();

                page.navigate("https://m.koreabaseball.com/Kbo/Live/Record.aspx?p_le_id=1&p_sr_id=" + entity.getSeries().getValue() + "&p_g_id=" + entity.getId());

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수
                var awayHitter = this.scrapeHitterTable(page, false, String.valueOf(now.getYear()), entity);
                page.waitForTimeout(500);
                var awayPitcher = this.scrapPitcherTable(page, false, String.valueOf(now.getYear()), entity);

                page.click("#liveRecordSubTabB");
                page.waitForTimeout(1000); // 탭 전환 후 데이터 로딩 기다림

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수
                var homeHitter = this.scrapeHitterTable(page, true, String.valueOf(now.getYear()), entity);
                page.waitForTimeout(500);
                var homePitcher = this.scrapPitcherTable(page, true, String.valueOf(now.getYear()), entity);

                hitterEntities.addAll(awayHitter);
                hitterEntities.addAll(homeHitter);
                pitcherEntities.addAll(awayPitcher);
                pitcherEntities.addAll(homePitcher);

                // 저장 후 game_match is_match_info_craw true 처리
                entity = entity.toBuilder()
                        .isMatchInfoCraw(true)
                        .build();

                gameMatchEntityRepository.save(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }

            hitterRecordEntityRepository.saveAll(hitterEntities);
            pitcherRecordEntityRepository.saveAll(pitcherEntities);
        });

        logger.info("========== Match Info Craw  END ==========");
    }

    private static List<HitterRecordEntity> scrapeHitterTable(Page page, Boolean isHome, String year, GameMatchEntity gameMatchEntity) {
        List<ElementHandle> infoRows = page.querySelectorAll("#HitterRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#HitterRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());
        List<HitterRecordEntity> hitterEntities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            var turn = Short.parseShort(infoRow.querySelector("td").innerText().trim());

            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "", position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            var hitCount = Short.parseShort(stats.get(0).innerText());
            var score = Short.parseShort(stats.get(1).innerText());
            var hit = Short.parseShort(stats.get(2).innerText());
            var homeRun = Short.parseShort(stats.get(3).innerText());
            var hitScore = Short.parseShort(stats.get(4).innerText());
            var ballFour = Short.parseShort(stats.get(5).innerText());
            var strikeOut = Short.parseShort(stats.get(6).innerText());

            hitterEntities.add(new HitterRecordEntity(
                    turn,
                    name,
                    position,
                    hitCount,
                    score,
                    hit,
                    homeRun,
                    hitScore,
                    ballFour,
                    strikeOut,
                    gameMatchEntity,
                    year,
                    isHome
            ));
        }

        return hitterEntities;
    }

    private static List<PitcherRecordEntity> scrapPitcherTable(Page page, Boolean isHome, String year, GameMatchEntity gameMatchEntity) {
        List<ElementHandle> infoRows = page.querySelectorAll("#PitcherRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#PitcherRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());

        List<PitcherRecordEntity> pitcherEntities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            var turn = Short.valueOf(infoRow.querySelector("td").innerText().trim());
            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "";
            String position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            var inning = stats.get(0).innerText();
            var pitching = Short.parseShort(stats.get(1).innerText());
            var hit = Short.parseShort(stats.get(4).innerText());
            var homeRun = Short.parseShort(stats.get(5).innerText());
            var ballFour = Short.parseShort(stats.get(6).innerText());
            var strikeOut = Short.parseShort(stats.get(7).innerText());
            var score = Short.parseShort(stats.get(8).innerText());

            pitcherEntities.add(new PitcherRecordEntity(
                    turn,
                    name,
                    position,
                    inning,
                    pitching,
                    ballFour,
                    strikeOut,
                    hit,
                    homeRun,
                    score,
                    gameMatchEntity,
                    year,
                    isHome
            ));
        }

        return pitcherEntities;
    }
}
