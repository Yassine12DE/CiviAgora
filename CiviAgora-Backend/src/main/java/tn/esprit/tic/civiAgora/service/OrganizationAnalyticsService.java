package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.ModuleRequest;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentItem;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentResponse;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.Survey;
import tn.esprit.tic.civiAgora.dao.entity.SurveySubmission;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleScope;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentItemRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentResponseRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.dao.repository.SurveyRepository;
import tn.esprit.tic.civiAgora.dao.repository.SurveySubmissionRepository;
import tn.esprit.tic.civiAgora.dto.analyticsDto.AnalyticsDashboardDto;
import tn.esprit.tic.civiAgora.dto.analyticsDto.ChartSeriesDto;
import tn.esprit.tic.civiAgora.dto.analyticsDto.KpiCardDto;
import tn.esprit.tic.civiAgora.dto.analyticsDto.ModuleActivityDto;
import tn.esprit.tic.civiAgora.dto.analyticsDto.RecentActivityDto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationAnalyticsService {

    private static final String ANALYTICS_MODULE_CODE = "ANALYTICS";
    private static final DateTimeFormatter ACTIVITY_DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final TenantAccessService tenantAccessService;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final UserRepository userRepository;
    private final OrganizationContentItemRepository contentItemRepository;
    private final OrganizationContentResponseRepository contentResponseRepository;
    private final ModuleRequestRepository moduleRequestRepository;
    private final OrganizationBillingService organizationBillingService;
    private final SurveyRepository surveyRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;

    @Transactional(readOnly = true)
    public AnalyticsDashboardDto getDashboard(Integer organizationId) {
        Organization organization = tenantAccessService.assertOrganizationAccessOrThrow(organizationId);
        String organizationSlug = organization.getSlug();

        boolean analyticsEnabled = isAnalyticsEnabled(organizationId);
        log.info("Tenant analytics request: organizationId={}, organizationSlug={}, jwtOrganizationId={}, jwtOrganizationSlug={}, analyticsEnabled={}",
                organizationId,
                organizationSlug,
                tenantAccessService.getCurrentJwtOrganizationId(),
                tenantAccessService.getCurrentJwtOrganizationSlug(),
                analyticsEnabled
        );

        if (!analyticsEnabled) {
            throw new AccessDeniedException("Analytics module is not enabled for this organization");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = YearMonth.from(now).atDay(1).atStartOfDay();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        List<YearMonth> recentMonths = buildRecentMonths(6, YearMonth.from(now));

        List<User> users = safeList(userRepository.findByOrganizationId(organizationId));
        List<OrganizationContentItem> contentItems = safeList(
                contentItemRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
        );
        List<OrganizationContentResponse> responses = safeList(contentResponseRepository.findByOrganizationId(organizationId));
        List<ModuleRequest> moduleRequests = safeList(moduleRequestRepository.findByOrganizationId(organizationId));
        List<Survey> surveys = safeList(surveyRepository.findByOrganizationId(organizationId));
        List<SurveySubmission> surveySubmissions = safeList(surveySubmissionRepository.findByOrganizationId(organizationId));
        List<OrganizationModule> enabledModules = safeList(
                organizationModuleRepository
                        .findByOrganizationIdAndGrantedBySaasTrueAndEnabledByOrganizationTrueOrderByDisplayOrderAsc(
                                organizationId
                        )
        ).stream()
                .filter(this::isModuleRowActive)
                .toList();

        long totalUsers = users.size();
        long activeUsers = users.stream()
                .filter(user -> !Boolean.TRUE.equals(user.getArchived()) && Boolean.TRUE.equals(user.getEnabled()))
                .count();
        long newUsersThisMonth = users.stream()
                .map(User::getCreatedTimestamp)
                .filter(Objects::nonNull)
                .map(Timestamp::toLocalDateTime)
                .filter(timestamp -> !timestamp.isBefore(monthStart))
                .count();

        Map<OrganizationContentType, Long> contentCountByType = contentItems.stream()
                .filter(item -> item.getType() != null)
                .collect(Collectors.groupingBy(OrganizationContentItem::getType, Collectors.counting()));
        Map<OrganizationContentType, Long> responseCountByType = responses.stream()
                .map(this::resolveResponseType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        long totalVotes = contentCountByType.getOrDefault(OrganizationContentType.VOTE, 0L);
        long totalConsultations = contentCountByType.getOrDefault(OrganizationContentType.CONCERTATION, 0L);
        long totalNews = contentCountByType.getOrDefault(OrganizationContentType.YOUTH_NEWS, 0L);
        long totalEvents = 0L;
        long totalRequests = moduleRequests.size();
        long totalSurveyResponses = surveySubmissions.size();
        long totalInteractions = responses.size() + totalSurveyResponses;
        long pendingModerationItems = moduleRequests.stream()
                .filter(request -> request.getStatus() == ModuleRequestStatus.PENDING)
                .count();
        long uniqueParticipants = java.util.stream.Stream.concat(
                        responses.stream().map(response -> response.getUser() != null ? response.getUser().getId() : null),
                        surveySubmissions.stream().map(submission -> submission.getUser() != null ? submission.getUser().getId() : null))
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long voteConsultationInteractions =
                responseCountByType.getOrDefault(OrganizationContentType.VOTE, 0L)
                        + responseCountByType.getOrDefault(OrganizationContentType.CONCERTATION, 0L);
        long voteConsultationSlots = (totalVotes + totalConsultations) * Math.max(activeUsers, 0L);
        double participationRate = percentage(uniqueParticipants, activeUsers > 0 ? activeUsers : totalUsers);
        double engagementRate = voteConsultationSlots > 0
                ? (voteConsultationInteractions * 100.0) / voteConsultationSlots
                : 0.0;

        long recentActivityCount = countRecentActivity(users, contentItems, responses, moduleRequests, thirtyDaysAgo)
                + surveySubmissions.stream().filter(item -> item.getSubmittedAt() != null && !item.getSubmittedAt().isBefore(thirtyDaysAgo)).count();

        List<ModuleActivityDto> moduleActivity = buildModuleActivity(
                enabledModules,
                contentCountByType,
                responseCountByType,
                activeUsers,
                surveys.size(),
                totalSurveyResponses
        );

        List<KpiCardDto> kpis = buildKpis(
                totalUsers,
                activeUsers,
                newUsersThisMonth,
                totalConsultations,
                totalVotes,
                totalRequests,
                totalNews,
                totalEvents,
                participationRate,
                engagementRate,
                totalInteractions,
                surveys.size(),
                totalSurveyResponses,
                pendingModerationItems,
                recentActivityCount
        );

        List<ChartSeriesDto> charts = buildCharts(
                users,
                contentItems,
                responses,
                moduleRequests,
                moduleActivity,
                activeUsers,
                recentMonths,
                surveySubmissions
        );

        List<RecentActivityDto> recentActivities = buildRecentActivities(users, contentItems, responses, moduleRequests, surveySubmissions);
        List<String> insights = buildInsights(moduleActivity, participationRate, pendingModerationItems, newUsersThisMonth, charts);

        log.info(
                "Tenant analytics KPI counts: organizationId={}, organizationSlug={}, totalUsers={}, activeUsers={}, newUsersThisMonth={}, totalConsultations={}, totalVotes={}, totalInteractions={}, pendingModerationItems={}",
                organizationId,
                organizationSlug,
                totalUsers,
                activeUsers,
                newUsersThisMonth,
                totalConsultations,
                totalVotes,
                totalInteractions,
                pendingModerationItems
        );

        return AnalyticsDashboardDto.builder()
                .organizationId(organizationId)
                .organizationSlug(organizationSlug)
                .analyticsEnabled(true)
                .status("ENABLED")
                .message("Analytics dashboard generated successfully.")
                .kpis(kpis)
                .charts(charts)
                .moduleActivity(moduleActivity)
                .recentActivities(recentActivities)
                .insights(insights)
                .build();
    }

    private boolean isAnalyticsEnabled(Integer organizationId) {
        Optional<OrganizationModule> module = organizationModuleRepository.findByOrganizationIdAndModuleCode(
                organizationId,
                ANALYTICS_MODULE_CODE
        );
        if (module.isEmpty()) {
            return false;
        }
        if (!organizationBillingService.isSubscriptionActive(organizationId)) {
            return false;
        }
        OrganizationModule organizationModule = module.get();
        return Boolean.TRUE.equals(organizationModule.getGrantedBySaas())
                && Boolean.TRUE.equals(organizationModule.getEnabledByOrganization())
                && ModuleScope.resolveOrDefault(organizationModule.getModule().getScope()).allowsBackOffice()
                && isModuleRowActive(organizationModule);
    }

    private List<KpiCardDto> buildKpis(
            long totalUsers,
            long activeUsers,
            long newUsersThisMonth,
            long totalConsultations,
            long totalVotes,
            long totalRequests,
            long totalNews,
            long totalEvents,
            double participationRate,
            double engagementRate,
            long totalInteractions,
            long totalSurveys,
            long totalSurveyResponses,
            long pendingModerationItems,
            long recentActivityCount
    ) {
        return List.of(
                kpi("total_users", "Total citizens/users", totalUsers, "neutral", null),
                kpi("active_users", "Enabled member accounts", activeUsers, "success", null),
                kpi("new_users_month", "New users this month", newUsersThisMonth, "success", "up"),
                kpi("consultations", "Total consultations", totalConsultations, "primary", null),
                kpi("votes", "Vote/poll items", totalVotes, "primary", null),
                kpi("requests", "Module access requests", totalRequests, "warning", null),
                kpi("news", "Youth news items", totalNews, "neutral", null),
                kpi("events", "Total events", totalEvents, "neutral", null),
                percentageKpi("participation_rate", "Participation rate", participationRate, "success"),
                percentageKpi("engagement_rate", "Vote/consultation engagement", engagementRate, "primary"),
                kpi("interactions", "Recorded content responses", totalInteractions, "primary", null),
                kpi("surveys", "Surveys", totalSurveys, "neutral", null),
                kpi("survey_responses", "Survey responses", totalSurveyResponses, "primary", null),
                kpi("pending_moderation", "Pending module requests", pendingModerationItems, "danger", null),
                kpi("recent_activity", "Recent activity (30 days)", recentActivityCount, "neutral", null)
        );
    }

    private List<ChartSeriesDto> buildCharts(
            List<User> users,
            List<OrganizationContentItem> contentItems,
            List<OrganizationContentResponse> responses,
            List<ModuleRequest> moduleRequests,
            List<ModuleActivityDto> moduleActivity,
            long activeUsers,
            List<YearMonth> recentMonths,
            List<SurveySubmission> surveySubmissions
    ) {
        Map<YearMonth, Long> usersByMonth = aggregateUsersByMonth(users);
        Map<YearMonth, Long> activeUsersByMonth = aggregateActiveUsersByMonth(users);
        Map<YearMonth, Long> contentByMonth = aggregateContentByMonth(contentItems);
        Map<YearMonth, Long> interactionsByMonth = aggregateInteractionsByMonth(responses);
        Map<YearMonth, Long> voteByMonth = aggregateResponsesByMonth(
                responses,
                response -> resolveResponseType(response) == OrganizationContentType.VOTE
        );
        Map<YearMonth, Long> consultationParticipationByMonth = aggregateResponsesByMonth(
                responses,
                response -> resolveResponseType(response) == OrganizationContentType.CONCERTATION
                        && Boolean.TRUE.equals(response.getParticipating())
        );
        Map<YearMonth, Long> reactionsByMonth = aggregateResponsesByMonth(
                responses,
                response -> resolveResponseType(response) == OrganizationContentType.YOUTH_NEWS
                        && response.getReaction() != null
                        && !response.getReaction().isBlank()
        );
        Map<YearMonth, Long> requestsByMonth = aggregateRequestsByMonth(moduleRequests);
        Map<YearMonth, Long> surveysByMonth = surveySubmissions.stream()
                .filter(item -> item.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(item -> YearMonth.from(item.getSubmittedAt()), Collectors.counting()));

        List<Map<String, Object>> userGrowthPoints = new ArrayList<>();
        long cumulativeUsers = 0L;
        long cumulativeActiveUsers = 0L;
        for (YearMonth month : recentMonths) {
            long monthlyNewUsers = usersByMonth.getOrDefault(month, 0L);
            long monthlyActiveUsers = activeUsersByMonth.getOrDefault(month, 0L);
            cumulativeUsers += monthlyNewUsers;
            cumulativeActiveUsers += monthlyActiveUsers;

            userGrowthPoints.add(chartPoint(
                    monthLabel(month),
                    monthlyNewUsers,
                    Map.of(
                            "cumulative", cumulativeUsers,
                            "activeUsers", cumulativeActiveUsers,
                            "activeNewUsers", monthlyActiveUsers
                    )
            ));
        }

        List<Map<String, Object>> contentByMonthPoints = recentMonths.stream()
                .map(month -> chartPoint(monthLabel(month), contentByMonth.getOrDefault(month, 0L)))
                .toList();

        List<Map<String, Object>> engagementTrendPoints = recentMonths.stream()
                .map(month -> {
                    long voteInteractions = voteByMonth.getOrDefault(month, 0L);
                    long consultationParticipation = consultationParticipationByMonth.getOrDefault(month, 0L);
                    long reactions = reactionsByMonth.getOrDefault(month, 0L);
                    long requestSubmissions = requestsByMonth.getOrDefault(month, 0L);
                    long comments = 0L;
                    long surveyResponses = surveysByMonth.getOrDefault(month, 0L);
                    long totalEngagement = voteInteractions
                            + consultationParticipation
                            + reactions
                            + requestSubmissions
                            + comments
                            + surveyResponses;
                    long totalInteractions = interactionsByMonth.getOrDefault(month, 0L);
                    double monthParticipationRate = percentage(totalEngagement, activeUsers);
                    return chartPoint(monthLabel(month), totalEngagement, Map.of(
                            "votes", voteInteractions,
                            "consultations", consultationParticipation,
                            "comments", comments,
                            "reactions", reactions,
                            "requests", requestSubmissions,
                            "surveys", surveyResponses,
                            "interactions", totalInteractions,
                            "participationRate", round(monthParticipationRate)
                    ));
                })
                .toList();

        List<Map<String, Object>> moduleActivityPoints = moduleActivity.stream()
                .sorted(Comparator.comparingLong((ModuleActivityDto item) -> safeLong(item.getInteractionCount())).reversed())
                .map(activity -> chartPoint(
                        safeText(activity.getModuleName(), safeText(activity.getModuleCode(), "Unknown module")),
                        safeLong(activity.getInteractionCount()),
                        Map.of("contentCount", safeLong(activity.getContentCount()))
                ))
                .toList();

        Map<OrganizationContentType, Long> interactionsByType = responses.stream()
                .map(this::resolveResponseType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Map<String, Object>> participationByContentTypePoints = List.of(
                chartPoint("Votes", interactionsByType.getOrDefault(OrganizationContentType.VOTE, 0L), Map.of(
                        "progress", round(percentage(interactionsByType.getOrDefault(OrganizationContentType.VOTE, 0L), activeUsers))
                )),
                chartPoint("Consultations", interactionsByType.getOrDefault(OrganizationContentType.CONCERTATION, 0L), Map.of(
                        "progress", round(percentage(interactionsByType.getOrDefault(OrganizationContentType.CONCERTATION, 0L), activeUsers))
                )),
                chartPoint("News reactions", interactionsByType.getOrDefault(OrganizationContentType.YOUTH_NEWS, 0L), Map.of(
                        "progress", round(percentage(interactionsByType.getOrDefault(OrganizationContentType.YOUTH_NEWS, 0L), activeUsers))
                )),
                chartPoint("Surveys", surveySubmissions.size(), Map.of(
                        "progress", round(percentage(surveySubmissions.size(), activeUsers))
                ))
        );

        Map<ModuleRequestStatus, Long> requestStatusCounts = moduleRequests.stream()
                .filter(request -> request.getStatus() != null)
                .collect(Collectors.groupingBy(ModuleRequest::getStatus, Collectors.counting()));

        List<Map<String, Object>> requestStatusPoints = List.of(
                chartPoint("Pending", requestStatusCounts.getOrDefault(ModuleRequestStatus.PENDING, 0L), Map.of("tone", "warning")),
                chartPoint("Approved", requestStatusCounts.getOrDefault(ModuleRequestStatus.APPROVED, 0L), Map.of("tone", "success")),
                chartPoint("Rejected", requestStatusCounts.getOrDefault(ModuleRequestStatus.REJECTED, 0L), Map.of("tone", "danger"))
        );

        Map<Long, Long> interactionsByContentId = responses.stream()
                .filter(response -> response.getContentItem() != null && response.getContentItem().getId() != null)
                .collect(Collectors.groupingBy(
                        response -> response.getContentItem().getId(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> topContentPoints = contentItems.stream()
                .sorted(Comparator.comparingLong(
                                (OrganizationContentItem item) -> interactionsByContentId.getOrDefault(item.getId(), 0L))
                        .reversed())
                .limit(5)
                .map(item -> chartPoint(
                        safeText(item.getTitle(), "Untitled content"),
                        interactionsByContentId.getOrDefault(item.getId(), 0L),
                        Map.of("type", item.getType() != null ? item.getType().name() : "UNKNOWN")
                ))
                .toList();

        Map<String, Long> usersByRole = users.stream()
                .filter(user -> user.getRole() != null)
                .collect(Collectors.groupingBy(user -> user.getRole().name(), Collectors.counting()));
        List<Map<String, Object>> usersByRolePoints = usersByRole.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> chartPoint(entry.getKey(), entry.getValue()))
                .toList();

        return List.of(
                chart(
                        "engagement-evolution",
                        "Engagement evolution",
                        "Votes, consultation participation, reactions, and module requests over time.",
                        "area-smooth",
                        "Month",
                        "Engagement actions",
                        engagementTrendPoints
                ),
                chart("activity-by-module", "Activity by module", "horizontal-bar", "Module", "Interactions", moduleActivityPoints),
                chart("participation-by-content-type", "Participation by content type", "radial-progress", "Content type", "Responses", participationByContentTypePoints),
                chart("requests-status-distribution", "Requests status distribution", "donut", "Status", "Count", requestStatusPoints),
                chart(
                        "user-growth",
                        "User growth by month",
                        "Monthly user registrations with cumulative and active user trajectories.",
                        "line-composition",
                        "Month",
                        "Users",
                        userGrowthPoints
                ),
                chart("content-created-by-month", "Content created by month", "bar", "Month", "Items", contentByMonthPoints),
                chart("users-by-role", "Users by role", "horizontal-bar", "Role", "Users", usersByRolePoints),
                chart("top-content-interactions", "Top content by interactions", "ranked-list", "Content", "Interactions", topContentPoints)
        );
    }

    private List<ModuleActivityDto> buildModuleActivity(
            List<OrganizationModule> enabledModules,
            Map<OrganizationContentType, Long> contentCountByType,
            Map<OrganizationContentType, Long> responseCountByType,
            long activeUsers,
            long surveyCount,
            long surveyResponseCount
    ) {
        if (enabledModules == null || enabledModules.isEmpty()) {
            return List.of();
        }

        return enabledModules.stream()
                .map(module -> {
                    String moduleCode = module.getModule() != null ? module.getModule().getCode() : null;
                    OrganizationContentType contentType = moduleCodeToContentType(moduleCode);
                    long contentCount = contentType == null ? 0L : contentCountByType.getOrDefault(contentType, 0L);
                    long interactionCount = contentType == null ? 0L : responseCountByType.getOrDefault(contentType, 0L);
                    if ("SURVEYS".equalsIgnoreCase(moduleCode)) {
                        contentCount = surveyCount;
                        interactionCount = surveyResponseCount;
                    }
                    double rate = percentage(interactionCount, activeUsers);
                    return ModuleActivityDto.builder()
                            .moduleCode(moduleCode)
                            .moduleName(module.getModule() != null ? module.getModule().getName() : moduleCode)
                            .contentCount(contentCount)
                            .interactionCount(interactionCount)
                            .participationRate(round(rate))
                            .build();
                })
                .sorted(Comparator.comparingLong((ModuleActivityDto item) -> safeLong(item.getInteractionCount())).reversed())
                .toList();
    }

    private List<RecentActivityDto> buildRecentActivities(
            List<User> users,
            List<OrganizationContentItem> contentItems,
            List<OrganizationContentResponse> responses,
            List<ModuleRequest> moduleRequests,
            List<SurveySubmission> surveySubmissions
    ) {
        List<TimedActivity> activities = new ArrayList<>();

        users.stream()
                .filter(user -> user.getCreatedTimestamp() != null)
                .forEach(user -> activities.add(new TimedActivity(
                        user.getCreatedTimestamp().toLocalDateTime(),
                        RecentActivityDto.builder()
                                .type("USER")
                                .title("New user registration")
                                .description(safeText(user.getFirstName(), "User") + " " + safeText(user.getLastName(), "").trim())
                                .createdAt(user.getCreatedTimestamp().toLocalDateTime().format(ACTIVITY_DATE_FORMATTER))
                                .tone("success")
                                .build()
                )));

        contentItems.stream()
                .filter(item -> item.getCreatedAt() != null)
                .forEach(item -> activities.add(new TimedActivity(
                        item.getCreatedAt(),
                        RecentActivityDto.builder()
                                .type("CONTENT")
                                .title("Content published")
                                .description(safeText(item.getTitle(), "Untitled content"))
                                .createdAt(item.getCreatedAt().format(ACTIVITY_DATE_FORMATTER))
                                .tone(Boolean.TRUE.equals(item.getPublished()) ? "primary" : "warning")
                                .build()
                )));

        responses.stream()
                .filter(response -> response.getCreatedAt() != null)
                .forEach(response -> activities.add(new TimedActivity(
                        response.getCreatedAt(),
                        RecentActivityDto.builder()
                                .type("INTERACTION")
                                .title("Citizen interaction")
                                .description(response.getContentItem() != null
                                        ? safeText(response.getContentItem().getTitle(), "Content interaction")
                                        : "Content interaction")
                                .createdAt(response.getCreatedAt().format(ACTIVITY_DATE_FORMATTER))
                                .tone("neutral")
                                .build()
                )));

        moduleRequests.stream()
                .filter(request -> request.getRequestDate() != null)
                .forEach(request -> activities.add(new TimedActivity(
                        request.getRequestDate(),
                        RecentActivityDto.builder()
                                .type("REQUEST")
                                .title("Module request")
                                .description(request.getModule() != null
                                        ? safeText(request.getModule().getName(), "Module")
                                        : "Module")
                                .createdAt(request.getRequestDate().format(ACTIVITY_DATE_FORMATTER))
                                .tone(request.getStatus() == ModuleRequestStatus.PENDING ? "warning" : "info")
                                .build()
                )));

        surveySubmissions.stream()
                .filter(submission -> submission.getSubmittedAt() != null)
                .forEach(submission -> activities.add(new TimedActivity(
                        submission.getSubmittedAt(),
                        RecentActivityDto.builder()
                                .type("SURVEY_RESPONSE")
                                .title("Survey response")
                                .description(submission.getSurvey() != null
                                        ? safeText(submission.getSurvey().getTitle(), "Survey") : "Survey")
                                .createdAt(submission.getSubmittedAt().format(ACTIVITY_DATE_FORMATTER))
                                .tone("primary")
                                .build()
                )));

        return activities.stream()
                .sorted(Comparator.comparing(TimedActivity::timestamp).reversed())
                .map(TimedActivity::activity)
                .limit(12)
                .toList();
    }

    private List<String> buildInsights(
            List<ModuleActivityDto> moduleActivity,
            double participationRate,
            long pendingModerationItems,
            long newUsersThisMonth,
            List<ChartSeriesDto> charts
    ) {
        List<String> insights = new ArrayList<>();

        ChartSeriesDto engagementEvolution = charts.stream()
                .filter(chart -> "engagement-evolution".equals(chart.getKey()))
                .findFirst()
                .orElse(null);
        if (engagementEvolution != null && engagementEvolution.getPoints() != null && engagementEvolution.getPoints().size() >= 2) {
            long current = readLong(engagementEvolution.getPoints().get(engagementEvolution.getPoints().size() - 1).get("value"));
            long previous = readLong(engagementEvolution.getPoints().get(engagementEvolution.getPoints().size() - 2).get("value"));
            if (current > previous) {
                insights.add("Participation is increasing compared to last month.");
            } else if (current < previous) {
                insights.add("Participation is lower than last month and may need a campaign boost.");
            }
        }

        if (!moduleActivity.isEmpty()) {
            ModuleActivityDto mostActive = moduleActivity.get(0);
            insights.add("The most active module is " + safeText(mostActive.getModuleName(), "Unknown module") + ".");
        }

        if (participationRate < 15.0) {
            insights.add("Some content has low engagement and could benefit from better promotion.");
        }

        if (pendingModerationItems > 0) {
            insights.add("There are pending items requiring moderation.");
        }

        if (newUsersThisMonth > 0) {
            insights.add("User registrations increased this month.");
        }

        if (insights.isEmpty()) {
            insights.add("Analytics are available. Add more content to unlock deeper trends.");
        }

        return insights;
    }

    private long countRecentActivity(
            List<User> users,
            List<OrganizationContentItem> contentItems,
            List<OrganizationContentResponse> responses,
            List<ModuleRequest> moduleRequests,
            LocalDateTime from
    ) {
        long userEvents = users.stream()
                .map(User::getCreatedTimestamp)
                .filter(Objects::nonNull)
                .map(Timestamp::toLocalDateTime)
                .filter(date -> !date.isBefore(from))
                .count();
        long contentEvents = contentItems.stream()
                .map(OrganizationContentItem::getCreatedAt)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(from))
                .count();
        long interactionEvents = responses.stream()
                .map(OrganizationContentResponse::getCreatedAt)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(from))
                .count();
        long requestEvents = moduleRequests.stream()
                .map(ModuleRequest::getRequestDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(from))
                .count();
        return userEvents + contentEvents + interactionEvents + requestEvents;
    }

    private Map<YearMonth, Long> aggregateUsersByMonth(List<User> users) {
        return users.stream()
                .map(User::getCreatedTimestamp)
                .filter(Objects::nonNull)
                .map(Timestamp::toLocalDateTime)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private Map<YearMonth, Long> aggregateActiveUsersByMonth(List<User> users) {
        return users.stream()
                .filter(user -> !Boolean.TRUE.equals(user.getArchived()) && Boolean.TRUE.equals(user.getEnabled()))
                .map(User::getCreatedTimestamp)
                .filter(Objects::nonNull)
                .map(Timestamp::toLocalDateTime)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private Map<YearMonth, Long> aggregateContentByMonth(List<OrganizationContentItem> contentItems) {
        return contentItems.stream()
                .map(OrganizationContentItem::getCreatedAt)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private Map<YearMonth, Long> aggregateInteractionsByMonth(List<OrganizationContentResponse> responses) {
        return responses.stream()
                .map(OrganizationContentResponse::getCreatedAt)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private Map<YearMonth, Long> aggregateResponsesByMonth(
            List<OrganizationContentResponse> responses,
            java.util.function.Predicate<OrganizationContentResponse> predicate
    ) {
        return responses.stream()
                .filter(Objects::nonNull)
                .filter(predicate != null ? predicate : response -> true)
                .map(OrganizationContentResponse::getCreatedAt)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private Map<YearMonth, Long> aggregateRequestsByMonth(List<ModuleRequest> requests) {
        return requests.stream()
                .filter(Objects::nonNull)
                .map(ModuleRequest::getRequestDate)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private Map<YearMonth, Long> aggregateRequestCommentsByMonth(List<ModuleRequest> requests) {
        return requests.stream()
                .filter(Objects::nonNull)
                .filter(request -> request.getComment() != null && !request.getComment().isBlank())
                .map(ModuleRequest::getRequestDate)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
    }

    private List<YearMonth> buildRecentMonths(int monthCount, YearMonth anchorMonth) {
        List<YearMonth> months = new ArrayList<>();
        for (int i = monthCount - 1; i >= 0; i--) {
            months.add(anchorMonth.minusMonths(i));
        }
        return months;
    }

    private ChartSeriesDto chart(
            String key,
            String title,
            String chartType,
            String xAxisLabel,
            String yAxisLabel,
            List<Map<String, Object>> points
    ) {
        return chart(key, title, null, chartType, xAxisLabel, yAxisLabel, points);
    }

    private ChartSeriesDto chart(
            String key,
            String title,
            String subtitle,
            String chartType,
            String xAxisLabel,
            String yAxisLabel,
            List<Map<String, Object>> points
    ) {
        return ChartSeriesDto.builder()
                .key(key)
                .title(title)
                .subtitle(subtitle)
                .chartType(chartType)
                .xAxisLabel(xAxisLabel)
                .yAxisLabel(yAxisLabel)
                .points(points == null ? List.of() : points)
                .build();
    }

    private Map<String, Object> chartPoint(String label, Number value) {
        return chartPoint(label, value, Map.of());
    }

    private Map<String, Object> chartPoint(String label, Number value, Map<String, Object> extra) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("label", label);
        point.put("value", value == null ? 0 : value);
        if (extra != null && !extra.isEmpty()) {
            point.putAll(extra);
        }
        return point;
    }

    private KpiCardDto kpi(String key, String label, long value, String tone, String trend) {
        return KpiCardDto.builder()
                .key(key)
                .label(label)
                .value((double) value)
                .valueDisplay(String.valueOf(value))
                .tone(tone)
                .trend(trend)
                .build();
    }

    private KpiCardDto percentageKpi(String key, String label, double value, String tone) {
        return KpiCardDto.builder()
                .key(key)
                .label(label)
                .value(round(value))
                .valueDisplay(round(value) + "%")
                .tone(tone)
                .trend(null)
                .build();
    }

    private OrganizationContentType moduleCodeToContentType(String moduleCode) {
        if (moduleCode == null) {
            return null;
        }
        String normalized = moduleCode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "VOTE" -> OrganizationContentType.VOTE;
            case "CONFERENCE" -> OrganizationContentType.CONCERTATION;
            case "YOUTHSPACE", "NEWS" -> OrganizationContentType.YOUTH_NEWS;
            default -> null;
        };
    }

    private OrganizationContentType resolveResponseType(OrganizationContentResponse response) {
        if (response == null) {
            return null;
        }
        if (response.getType() != null) {
            return response.getType();
        }
        if (response.getContentItem() != null) {
            return response.getContentItem().getType();
        }
        return null;
    }

    private boolean isModuleRowActive(OrganizationModule organizationModule) {
        return organizationModule != null
                && organizationModule.getModule() != null
                && Boolean.TRUE.equals(organizationModule.getModule().getActive());
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (numerator * 100.0) / denominator;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String monthLabel(YearMonth month) {
        if (month == null) {
            return "";
        }
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record TimedActivity(LocalDateTime timestamp, RecentActivityDto activity) {
    }
}
