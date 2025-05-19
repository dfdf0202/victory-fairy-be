package kr.co.victoryfairy.core.batch;

import kr.co.victoryfairy.core.batch.service.BatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScoreJob {

    private final BatchService batchService;

    public ScoreJob(BatchService batchService)  {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${batch.check.score}")
    public void checkScore() {
        batchService.batchScore();
    }

    @Scheduled(cron = "${batch.check.info}")
    public void checkInfo() {
        batchService.batchMatchInfo();
    }
}
