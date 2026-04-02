package fr.ailegalcase.timetracking.service;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.timetracking.dto.TimeReportLineResponse;
import fr.ailegalcase.timetracking.dto.TimeReportResponse;
import fr.ailegalcase.timetracking.entity.TimeEntry;
import fr.ailegalcase.timetracking.entity.UserBillingRate;
import fr.ailegalcase.timetracking.repository.TimeEntryRepository;
import fr.ailegalcase.timetracking.repository.UserBillingRateRepository;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TimeReportService {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");
    private static final String UTF8_BOM = "\uFEFF";

    private final TimeEntryRepository timeEntryRepository;
    private final UserBillingRateRepository billingRateRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public TimeReportService(TimeEntryRepository timeEntryRepository,
                             UserBillingRateRepository billingRateRepository,
                             CaseFileRepository caseFileRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             CurrentUserResolver currentUserResolver) {
        this.timeEntryRepository = timeEntryRepository;
        this.billingRateRepository = billingRateRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public TimeReportResponse getMonthlyReport(String month, OidcUser oidcUser, Principal principal) {
        validateMonth(month);
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveAdminOrOwner(user);
        UUID workspaceId = member.getWorkspace().getId();

        List<TimeEntry> entries = loadEntries(workspaceId, month);
        List<TimeReportLineResponse> lines = aggregateEntries(entries, workspaceId);

        return new TimeReportResponse(month, lines);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String month, OidcUser oidcUser, Principal principal) {
        validateMonth(month);
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveAdminOrOwner(user);
        UUID workspaceId = member.getWorkspace().getId();

        List<TimeEntry> entries = loadEntries(workspaceId, month);
        List<TimeReportLineResponse> lines = aggregateEntries(entries, workspaceId);

        return buildCsv(lines);
    }

    private List<TimeEntry> loadEntries(UUID workspaceId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        Instant monthStart = yearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return timeEntryRepository.findCompletedByWorkspaceAndMonth(workspaceId, monthStart, monthEnd);
    }

    private List<TimeReportLineResponse> aggregateEntries(List<TimeEntry> entries, UUID workspaceId) {
        Map<String, TimeReportLineResponse> aggregated = new LinkedHashMap<>();

        for (TimeEntry entry : entries) {
            UUID userId = entry.getUser().getId();
            UUID caseFileId = entry.getCaseFile().getId();
            String key = caseFileId + "-" + userId;

            long seconds = entry.getDurationSeconds() != null ? entry.getDurationSeconds() : 0;

            if (!aggregated.containsKey(key)) {
                String title = caseFileRepository.findTitleById(caseFileId).orElse("(inconnu)");
                String email = entry.getUser().getEmail();

                LocalDate entryDate = entry.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
                UserBillingRate rateEntity = billingRateRepository
                        .findRateAtDate(userId, workspaceId, entryDate)
                        .orElse(null);
                BigDecimal rate = rateEntity != null ? rateEntity.getRatePerHour() : null;
                BigDecimal amount = computeAmount(seconds, rate);

                aggregated.put(key, new TimeReportLineResponse(
                        caseFileId, title, userId, email, seconds, rate, amount));
            } else {
                TimeReportLineResponse existing = aggregated.get(key);
                long newTotalSeconds = existing.totalSeconds() + seconds;
                BigDecimal amount = computeAmount(newTotalSeconds, existing.ratePerHour());
                aggregated.put(key, new TimeReportLineResponse(
                        existing.caseFileId(), existing.caseFileTitle(),
                        existing.userId(), existing.userEmail(),
                        newTotalSeconds, existing.ratePerHour(), amount));
            }
        }

        return new ArrayList<>(aggregated.values());
    }

    private BigDecimal computeAmount(long totalSeconds, BigDecimal rate) {
        if (rate == null) return null;
        BigDecimal hours = BigDecimal.valueOf(totalSeconds).divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
        return hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private byte[] buildCsv(List<TimeReportLineResponse> lines) {
        StringBuilder sb = new StringBuilder(UTF8_BOM);
        sb.append("Dossier,Utilisateur,Heures,Taux horaire (\u20ac),Montant (\u20ac)\n");

        for (TimeReportLineResponse line : lines) {
            double hours = line.totalSeconds() / 3600.0;
            sb.append(escapeCsv(line.caseFileTitle())).append(",");
            sb.append(escapeCsv(line.userEmail())).append(",");
            sb.append(String.format("%.2f", hours)).append(",");
            sb.append(line.ratePerHour() != null ? line.ratePerHour().toPlainString() : "").append(",");
            sb.append(line.amount() != null ? line.amount().toPlainString() : "").append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void validateMonth(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid month format. Expected YYYY-MM.");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private WorkspaceMember resolveAdminOrOwner(User user) {
        WorkspaceMember member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        String role = member.getMemberRole();
        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can access the time report");
        }
        return member;
    }
}
