package kr.co.victoryfairy.core.craw.service.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.TeamEnum;
import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchEntityRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrawServiceImpl implements CrawService {
    private final Browser browser;

    private final TeamEntityRepository teamEntityRepository;
    private final GameMatchEntityRepository gameMatchEntityRepository;

    public CrawServiceImpl(Browser browser, TeamEntityRepository teamEntityRepository, GameMatchEntityRepository gameMatchEntityRepository) {
        this.browser = browser;
        this.teamEntityRepository = teamEntityRepository;
        this.gameMatchEntityRepository = gameMatchEntityRepository;
    }

    @Override
    @Transactional
    public void crawMatchList(String sYear, String sMonth) {
        try (Playwright playwright = Playwright.create()) {

            Page page = browser.newPage();
            page.navigate("https://www.koreabaseball.com/Schedule/Schedule.aspx");

            // 연도 설정
            page.selectOption("#ddlYear", sYear);

            int startMonth = StringUtils.hasText(sMonth) ? Integer.parseInt(sMonth) : 3;

            var teamEntities = teamEntityRepository.findAll().stream()
                    .collect(Collectors.toMap(TeamEntity::getKboNm, entity -> entity));

            List<GameMatchEntity> gameEntities = new ArrayList<>();

            for (int month = startMonth; month <= 12; month++) {
                String monthStr = String.format("%02d", month);
                page.selectOption("#ddlMonth", monthStr);

                for (MatchEnum.MatchType matchType : MatchEnum.MatchType.values()) {

                    if (MatchEnum.MatchType.TIEBREAKER.equals(matchType)) continue;

                    page.selectOption("#ddlSeries", matchType.getValue());

                    // 테이블 로딩 대기
                    page.evaluate("getTableGridList();");
                    page.waitForSelector("#tblScheduleList tbody tr");

                    List<ElementHandle> rows = page.querySelectorAll("#tblScheduleList tbody tr");

                    if (rows.isEmpty()) break;

                    String lastValidDate = "";
                    for (ElementHandle row : rows) {

                        String rowText = row.innerText().trim();
                        if (rowText.contains("데이터가 없습니다")) {
                            continue;
                        }

                        String date = "";
                        ElementHandle dateCell = row.querySelector("td.day");

                        if (dateCell != null && !dateCell.innerText().isBlank()) {
                            date = dateCell.innerText();
                            lastValidDate = date; // 날짜 업데이트
                        } else {
                            date = lastValidDate; // 이전 날짜 사용
                        }

                        String time = "";
                        ElementHandle timeElement = row.querySelector("td.time b");
                        if (timeElement != null) {
                            time = timeElement.innerText();
                        }

                        // LocalDateTime 변환
                        LocalDateTime matchDateTime = parseDateTime(sYear, date, time);

                        ElementHandle playElement = row.querySelector("td.play");
                        if (playElement == null) continue;

                        List<ElementHandle> teamSpans = playElement.querySelectorAll("span");

                        String away = "";
                        String home = "";
                        Short awayScore = null;
                        Short homeScore = null;
                        String stadium = "";
                        String reason = "";

                        List<ElementHandle> tds = row.querySelectorAll("td");

                        int stadiumIndex = (tds.size() == 9) ? 7 : 6;
                        int reasonIndex = stadiumIndex + 1;

                        if (teamSpans.size() > 3) {
                            away = safeInnerText(teamSpans, 0);
                            awayScore = Short.parseShort(safeInnerText(teamSpans, 1));
                            homeScore = Short.parseShort(safeInnerText(teamSpans, 3));
                            home = safeInnerText(teamSpans, 4);
                        } else {
                            away = safeInnerText(teamSpans, 0);
                            home = safeInnerText(teamSpans, 2);
                        }

                        stadium = parseStadium(safeInnerText(tds, stadiumIndex));
                        reason = safeInnerText(tds, reasonIndex);

                        ElementHandle replayElement = row.querySelector("td.relay");

                        var matchStatus = MatchEnum.MatchStatus.READY;

                        ElementHandle relayTd = row.querySelector("td.relay a");
                        var matchId = "";
                        var kboAway = TeamEnum.KboTeamNm.fromDesc(away);
                        var kboHome = TeamEnum.KboTeamNm.fromDesc(home);

                        var awayEntity = teamEntities.get(kboAway.name());
                        var homeEntity = teamEntities.get(kboHome.name());

                        if (relayTd == null) {
                            matchStatus = matchDateTime.isAfter(LocalDateTime.now()) ? MatchEnum.MatchStatus.READY : MatchEnum.MatchStatus.CANCELED;
                            String formattedDate = matchDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                            matchId = formattedDate + kboAway + kboHome + 0;
                        } else {
                            String replayRowText = relayTd.innerText().trim();
                            matchStatus = replayRowText.equals("리뷰") ? MatchEnum.MatchStatus.END : MatchEnum.MatchStatus.READY;

                            ElementHandle reviewLink = row.querySelector("td.relay a#btnReview");
                            if (reviewLink != null) {
                                String href = reviewLink.getAttribute("href");

                                if (href != null && href.contains("gameId=")) {
                                    String[] parts = href.split("gameId=");
                                    if (parts.length > 1) {
                                        String[] gameIdSplit = parts[1].split("&");
                                        matchId = gameIdSplit[0];
                                    }
                                }
                            }
                        }

                        MatchEnum.SeriesType seriesType = switch (matchType) {
                            case EXHIBITION -> MatchEnum.SeriesType.EXHIBITION;
                            case REGULAR -> MatchEnum.SeriesType.REGULAR;
                            case TIEBREAKER -> MatchEnum.SeriesType.TIEBREAKER;
                            case POST -> null;
                        };

                        GameMatchEntity gameMatch = new GameMatchEntity(
                                matchId
                                ,sYear
                                ,matchType
                                ,seriesType
                                ,matchDateTime
                                ,awayEntity
                                ,away
                                ,awayScore
                                ,homeEntity
                                ,home
                                ,homeScore
                                ,matchStatus
                                ,stadium
                                ,reason
                                );

                        gameEntities.add(gameMatch);

                        // 객체 바로 정리 (GC 도움)
                        playElement.dispose();
                        replayElement.dispose();
                        teamSpans.forEach(ElementHandle::dispose);
                        tds.forEach(ElementHandle::dispose);
                    }

                    rows.forEach(ElementHandle::dispose);
                }
            }

            gameMatchEntityRepository.saveAll(gameEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void crawMatchDetail(String sYear) {
        var matches = gameMatchEntityRepository.findBySeason(sYear).stream()
                .sorted(Comparator.comparing(GameMatchEntity :: getMatchAt))
                .toList();



        if (matches.isEmpty()) {
            // TODO : 공통 Exception 생성 후 처리
        }

        try (Playwright playwright = Playwright.create()) {

            Page page = browser.newPage();
            matches.stream()
            .filter(match -> !MatchEnum.MatchStatus.CANCELED.equals(match.getStatus()))
            .forEach(match -> {
                page.navigate("https://m.koreabaseball.com/Kbo/Live/Record.aspx?p_le_id=1&p_sr_id=" + match.getSeries().getValue() + "&p_g_id=" + match.getId());

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수

                System.out.println("======== 경기 id ========");
                System.out.println(match.getId());
                System.out.println("======== 🟥 홈팀 타자 기록 ========");
                this.scrapeHitterTable(page);

                System.out.println("======== 🟥 홈팀 투수 기록 ========");
                page.waitForTimeout(500);             // ✅ 안정적 로딩 대기
                this.scrapPitcherTable(page);

                // 탭 클릭해서 원정팀으로 전환
                page.click("#liveRecordSubTabB");
                page.waitForTimeout(1000); // 탭 전환 후 데이터 로딩 기다림
                page.waitForSelector("#HitterRank");

                System.out.println("\n======== 🟦 원정팀 타자 기록 ========");
                this.scrapeHitterTable(page);
                System.out.println("\n======== 🟦 원정팀 투수 기록 ========");
                page.waitForSelector("#PitcherRank"); // ✅ 추가
                page.waitForTimeout(500);             // ✅ 추가
                this.scrapPitcherTable(page);

            });

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private LocalDateTime parseDateTime(String sYear, String dateStr, String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            timeStr = "00:00";
        }

        String cleanDate = dateStr.split("\\(")[0].trim(); // 예: "10.21"
        String fullDateTimeStr = sYear + "-" + cleanDate.replace(".", "-") + " " + timeStr;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(fullDateTimeStr, formatter);
    }

    private String safeInnerText(List<ElementHandle> list, int index) {
        try {
            return list.get(index).innerText();
        } catch (Exception e) {
            return "";
        }
    }

    private String parseStadium(String stadiumStr) {
        if (!StringUtils.hasText(stadiumStr)) {
            return null;
        }

        var stadium = "";

        switch (stadiumStr) {
            case "고척" -> stadium = "고척스카이돔";
            case "광주" -> stadium = "광주기아챔피언스필드";
            case "대구" -> stadium = "대구삼성라이온즈파크";
            case "대전" -> stadium = "한화생명이글스파크";
            case "대전(신)" -> stadium = "대전한화생명볼파크";
            case "사직" -> stadium = "사직야구장";
            case "잠실" -> stadium = "서울종합운동장야구장";
            case "수원" -> stadium = "수원KT위즈파크";
            case "문학" -> stadium = "인천SSG랜더스필드";
            case "창원" -> stadium = "창원NC파크";
            default -> stadium = stadiumStr;
        }

        return stadium;
    }

    private static void scrapeHitterTable(Page page) {
        List<ElementHandle> infoRows = page.querySelectorAll("#HitterRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#HitterRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());
        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            String order = infoRow.querySelector("td").innerText().trim();

            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "", position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            int ab = Integer.parseInt(stats.get(0).innerText());
            int run = Integer.parseInt(stats.get(1).innerText());
            int hit = Integer.parseInt(stats.get(2).innerText());
            int hr = Integer.parseInt(stats.get(3).innerText());
            int rbi = Integer.parseInt(stats.get(4).innerText());
            int bbHbp = Integer.parseInt(stats.get(5).innerText());
            int so = Integer.parseInt(stats.get(6).innerText());
            int sb = Integer.parseInt(stats.get(7).innerText());
            int cs = Integer.parseInt(stats.get(8).innerText());

            System.out.printf("[%s번] %s %s → 타수: %d, 득점: %d, 안타: %d, 홈런: %d, 타점: %d, 4사구: %d, 삼진: %d, 도루: %d, 도루실패: %d\n",
                    order, name, position, ab, run, hit, hr, rbi, bbHbp, so, sb, cs);
        }
    }

    private static void scrapPitcherTable(Page page) {
        List<ElementHandle> infoRows = page.querySelectorAll("#PitcherRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#PitcherRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());

        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            String order = infoRow.querySelector("td").innerText().trim();
            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "", position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            String inning = stats.get(0).innerText();
            int pitching = Integer.parseInt(stats.get(1).innerText());
            int hit = Integer.parseInt(stats.get(4).innerText());
            int homeRun = Integer.parseInt(stats.get(5).innerText());
            int ballFour = Integer.parseInt(stats.get(6).innerText());
            int strikeOut = Integer.parseInt(stats.get(7).innerText());
            int score = Integer.parseInt(stats.get(8).innerText());

            System.out.printf("[%s번] %s %s → 이닝: %s, 투구수: %d, 피안타: %d, 피홈런: %d, 4사구: %d, 삼진: %d, 실점: %d\n",
                    order, name, position, inning, pitching, hit, homeRun, ballFour, strikeOut, score);
        }
    }
}
