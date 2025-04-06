package kr.co.victoryfairy.core.service.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.TeamEnum;
import kr.co.victoryfairy.core.service.BatchService;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchEntityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BatchServiceImpl implements BatchService {
    private final Browser browser;
    private final GameMatchEntityRepository gameMatchEntityRepository;

    public BatchServiceImpl(Browser browser, GameMatchEntityRepository gameMatchEntityRepository) {
        this.browser = browser;
        this.gameMatchEntityRepository = gameMatchEntityRepository;
    }

    @Override
    public void batchScore() {

        try (Playwright playwright = Playwright.create()) {
            Page page = browser.newPage();
            page.navigate("https://m.koreabaseball.com/Kbo/Schedule.aspx");

            page.waitForSelector("ul#now");
            List<ElementHandle> gameElements = page.querySelectorAll("ul#now > li.list");

            String dateText = page.querySelector("#lblGameDate").innerText();
            String formattedDate = dateText.replaceAll("[^0-9]", "");

            Map<String, Integer> matchCountMap = new HashMap<>();
            for (ElementHandle game : gameElements) {

                String classAttr = game.getAttribute("class");
                String statusClass = classAttr.replace("list", "").trim();

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

                if (matchEntity == null) return;

                if (!matchEntity.getStatus().equals(matchStatus)) {
                    switch (matchStatus) {
                        case END -> matchEntity.updateMatchStatus(Short.parseShort(awayScore), Short.parseShort(homeScore), matchStatus);
                        case CANCELED -> matchEntity.cancelMatchStatus(reason, matchStatus);
                        default -> {
                            return;
                        }
                    }
                    gameMatchEntityRepository.save(matchEntity);
                    return;
                }
            }

            page.close();
        } catch (Exception e) {
            e.printStackTrace();
            browser.close();
        }
    }
}
