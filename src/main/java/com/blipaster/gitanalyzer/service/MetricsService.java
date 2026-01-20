package com.blipaster.gitanalyzer.service;

import com.blipaster.gitanalyzer.dto.HotspotDto;
import com.blipaster.gitanalyzer.dto.MetricSummaryDto;
import com.blipaster.gitanalyzer.dto.RiskLevel;
import com.blipaster.gitanalyzer.repository.CommitRepository;
import com.blipaster.gitanalyzer.repository.FileChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final FileChangeRepository fileChangeRepository;
    private final CommitRepository commitRepository;

    @Transactional(readOnly = true)
    public MetricSummaryDto calculateSummary(Long repoId) {
        int busFactor = calculateBusFactor(repoId);
        List<HotspotDto> hotspots = calculateHotspots(repoId, 10);
        String burnout = calculateBurnout(repoId);
        int healthScore = calculateHealthScore(busFactor, hotspots, burnout);

        return new MetricSummaryDto(
                healthScore,
                busFactor,
                hotspots,
                burnout
        );
    }


    private int calculateBusFactor(Long repoId) {
        List<Object[]> rows = fileChangeRepository.countChangesByDeveloper(repoId);

        int total = rows.stream()
                .mapToInt(r -> ((Number) r[1]).intValue())
                .sum();

        int threshold = (int) (total * 0.8);
        int accumulated = 0;
        int authors = 0;

        for (Object[] r : rows) {
            accumulated += ((Number) r[1]).intValue();
            authors++;
            if (accumulated >= threshold) break;
        }
        return authors;
    }

    public List<HotspotDto> calculateHotspots(Long repoId, int limit) {
        return fileChangeRepository.findTopHotspots(repoId, limit).stream()
                .map(row -> {
                    String path = (String) row[0];
                    int changes = ((Number) row[1]).intValue();
                    int churn = ((Number) row[2]).intValue();

                    RiskLevel risk =
                            churn > 200 ? RiskLevel.HIGH :
                                    churn > 100 ? RiskLevel.MEDIUM :
                                            RiskLevel.LOW;

                    return new HotspotDto(path, churn, changes, risk);
                })
                .toList();
    }

    /*
    List<Instant> times =
                commitRepository.findCommitDatesByRepoId(repoId);

        if (times.isEmpty()) return "Нет данных";

        long suspicious = times.stream()
                .map(t -> t.atZone(ZoneId.systemDefault()).toLocalDateTime())
                .filter(t ->
                        t.getDayOfWeek() == DayOfWeek.SATURDAY ||
                                t.getDayOfWeek() == DayOfWeek.SUNDAY ||
                                t.getHour() < 8 ||
                                t.getHour() > 20
                )
                .count();

        double percent = suspicious * 100.0 / times.size();
     */

    private String calculateBurnout(Long repoId) {
        Double percent = commitRepository.calculateBurnoutPercentage(repoId);

        if (percent == null) {
            return "Нет данных";
        }

        return percent > 20
                ? String.format("Внимание: %.0f%% коммитов ночью/выходные", percent)
                : String.format("%.0f%% коммитов ночью/выходные", percent);
    }

    private int calculateHealthScore(
            int busFactor,
            List<HotspotDto> hotspots,
            String burnout
    ) {
        int score = 100;

        if (busFactor <= 2) score -= 40;
        else if (busFactor <= 4) score -= 20;

        long highRisk =
                hotspots.stream().filter(h -> h.riskLevel() == RiskLevel.HIGH).count();

        if (highRisk > 3) score -= 30;
        else if (highRisk > 0) score -= 15;

        if (burnout.contains("Внимание")) score -= 15;

        return Math.max(score, 10);
    }


}
