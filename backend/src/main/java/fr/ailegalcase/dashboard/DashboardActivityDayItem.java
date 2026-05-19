package fr.ailegalcase.dashboard;

import java.time.LocalDate;

public record DashboardActivityDayItem(LocalDate date, long analysesCount) {}
