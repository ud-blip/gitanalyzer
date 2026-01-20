package com.blipaster.gitanalyzer.service;

import com.blipaster.gitanalyzer.dto.PathChurnDto;
import com.blipaster.gitanalyzer.dto.TreemapNodeDto;
import com.blipaster.gitanalyzer.repository.FileChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreemapDataService {

    private final FileChangeRepository fileChangeRepository;

    @Transactional(readOnly = true)
    public List<TreemapNodeDto> buildLevel(Long repoId, String pathPrefix, int minChurn, int limit) {
        // 1. Получаем данные
        String safePrefix = (pathPrefix == null || pathPrefix.isBlank()) ? "" : pathPrefix;
        List<PathChurnDto> data = getPathChurns(repoId, safePrefix);

        if (data.isEmpty()) return List.of();

        Map<String, List<PathChurnDto>> grouped = new HashMap<>();
        int totalChurn = 0;

        // 2. Группируем файлы по их "первому уровню" относительно префикса
        for (PathChurnDto dto : data) {
            String path = dto.path();
            // Отрезаем префикс
            String relative = path.startsWith(safePrefix) ? path.substring(safePrefix.length()) : path;
            if (relative.startsWith("/")) relative = relative.substring(1);
            if (relative.isBlank()) continue;

            // Выделяем имя папки или файла на текущем уровне
            int slashIndex = relative.indexOf("/");
            String nodeName = (slashIndex != -1) ? relative.substring(0, slashIndex) : relative;

            grouped.computeIfAbsent(nodeName, k -> new ArrayList<>()).add(dto);
            totalChurn += dto.churn();
        }

        // 3. Формируем список узлов (Nodes)
        int finalTotalChurn = totalChurn;
        List<TreemapNodeDto> nodes = grouped.entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey();
                    List<PathChurnDto> filesInNode = entry.getValue();
                    int sum = filesInNode.stream().mapToInt(PathChurnDto::churn).sum();

                    boolean hasChildren = filesInNode.stream()
                            .anyMatch(p -> p.path().contains(safePrefix + (safePrefix.endsWith("/") || safePrefix.isEmpty() ? "" : "/") + name + "/"));

                    double baseImpact = (finalTotalChurn == 0) ? 0 : (sum * 1.0 / finalTotalChurn);

                    // Коэффициент "размазанности": Если изменений много, но файлов тоже много — это риск
                    // Если в среднем на файл приходится мало изменений, значит мы трогаем кучу файлов сразу
                    double densityPenalty = (filesInNode.isEmpty()) ? 0 :
                            (double) sum / filesInNode.size(); // Средний Churn на файл

                    // Итоговый риск: База + небольшой бонус за плотность изменений.
                    double smartRisk = baseImpact;

                    return new TreemapNodeDto(name, sum, smartRisk, hasChildren);
                })
                .filter(node -> node.value() >= minChurn)
                .sorted((a, b) -> Integer.compare(b.value(), a.value()))
                .collect(Collectors.toList());

        return applyLimit(nodes, limit, finalTotalChurn);
    }

    public List<PathChurnDto> getPathChurns(Long repoId, String prefix) {
        return fileChangeRepository.findPathsWithChurn(repoId, prefix)
                .stream()
                .map(row -> new PathChurnDto(
                        (String) row[0],
                        row[1] != null ? ((Number) row[1]).intValue() : 0
                ))
                .toList();
    }

    private List<TreemapNodeDto> applyLimit(List<TreemapNodeDto> nodes, int limit, int total) {
        if (nodes.size() <= limit) return nodes;

        List<TreemapNodeDto> top = new ArrayList<>(nodes.subList(0, limit - 1));
        int otherSum = nodes.subList(limit - 1, nodes.size()).stream().mapToInt(TreemapNodeDto::value).sum();

        top.add(new TreemapNodeDto("Other", otherSum, (total == 0 ? 0 : (otherSum * 1.0 / total)), false));
        return top;
    }
}