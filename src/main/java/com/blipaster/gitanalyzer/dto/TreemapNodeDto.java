package com.blipaster.gitanalyzer.dto;

public record TreemapNodeDto(
        String name,
        int value,
        double risk,
        boolean hasChildren
) {}
