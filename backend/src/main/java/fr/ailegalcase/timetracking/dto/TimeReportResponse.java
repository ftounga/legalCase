package fr.ailegalcase.timetracking.dto;

import java.util.List;

public record TimeReportResponse(
        String month,
        List<TimeReportLineResponse> lines
) {}
