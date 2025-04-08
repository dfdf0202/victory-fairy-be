package kr.co.victoryfairy.core.service.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.service.BatchService;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchEntityRepository;
import kr.co.victoryfairy.support.handler.RedisHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BatchServiceImpl implements BatchService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GameMatchEntityRepository gameMatchEntityRepository;
    private final RedisHandler redisHandler;

    public BatchServiceImpl(GameMatchEntityRepository gameMatchEntityRepository, RedisHandler redisHandler) {
        this.gameMatchEntityRepository = gameMatchEntityRepository;
        this.redisHandler = redisHandler;
    }

    @Override
    public void batchScore() {
        logger.info("========== Batch  Start ==========");

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
                String awayScore = game.querySelector(".team.away .score").innerText();
                // 홈 점수
                String homeScore = game.querySelector(".team.home .score").innerText();

                var id = formattedDate + awayTeamName + homeTeamName + matchOrder;
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
                        matchEntity.cancelMatchStatus(reason, matchStatus);
                    } else {
                        matchEntity.updateMatchStatus(matchStatus);
                    }
                    gameMatchEntityRepository.save(matchEntity);
                }
                // 진행 중인 경기의 최종 상태 변경
                if (matchEntity.getStatus().equals(MatchEnum.MatchStatus.PROGRESS)) {
                    switch (matchStatus) {
                        case END -> {
                            matchEntity.updateMatchStatusAndScore(Short.parseShort(awayScore), Short.parseShort(homeScore), matchStatus);
                            gameMatchEntityRepository.save(matchEntity);
                            redisHandler.deleteMap(id);
                        }
                        case CANCELED -> {
                            matchEntity.cancelMatchStatus(reason, matchStatus);
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
        }
    }
}
