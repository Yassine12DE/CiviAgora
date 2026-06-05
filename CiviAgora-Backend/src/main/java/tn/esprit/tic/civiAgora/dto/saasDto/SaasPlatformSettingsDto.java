package tn.esprit.tic.civiAgora.dto.saasDto;

import lombok.Data;

@Data
public class SaasPlatformSettingsDto {
    private String platformName;
    private String supportEmail;
    private String publicDomain;
    private String defaultLocale;
    private String platformDescription;

    private Boolean maintenanceMode;
    private Boolean allowOnboardingRequests;
    private Boolean autoApproveModuleRequests;

    private String brandPrimaryColor;
    private String brandSecondaryColor;
    private String logoUrl;
    private String faviconUrl;

    private String smtpHost;
    private Integer smtpPort;
    private String emailFromAddress;
    private String emailFromName;
    private String smtpUsername;
    private String smtpPassword;

    private Boolean requireMfaSuperAdmin;
    private Boolean requireMfaTenantAdmin;
    private Boolean ipAllowListEnabled;
    private Boolean auditSensitiveExports;
    private Integer sessionTimeoutMinutes;
    private Integer passwordMinLength;

    private String tenantDefaultTimezone;
    private String tenantDefaultLanguage;
    private String tenantUrlPattern;
    private String tenantStorageQuota;

    private String defaultEnabledModulesCsv;

    private Boolean notifyNewOrganizationRequests;
    private Boolean notifyModuleRequests;
    private Boolean notifyPaymentReceived;
    private Boolean notifyOverdueInvoices;
    private Boolean notifySystemErrors;
    private Boolean notifyWeeklyDigest;
}
