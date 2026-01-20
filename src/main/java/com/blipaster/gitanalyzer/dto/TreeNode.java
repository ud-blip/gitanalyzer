package com.blipaster.gitanalyzer.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {
    private String name;
    private long value;   // Это будет Churn
    private double risk;  // От 0.0 до 1.0

    @Builder.Default
    private List<TreeNode> children = new ArrayList<>();

    // Метод для D3.js, чтобы он понимал, где конец ветки
    public List<TreeNode> getChildren() {
        return (children == null || children.isEmpty()) ? null : children;
    }
}

