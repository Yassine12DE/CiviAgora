package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.SaasPlatformSettings;
import tn.esprit.tic.civiAgora.dao.repository.SaasPlatformSettingsRepository;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasPlatformSettingsDto;

@Service
@RequiredArgsConstructor
public class SaasPlatformSettingsService {

    private static final int SETTINGS_ID = 1;

    private final SaasPlatformSettingsRepository repository;

    public SaasPlatformSettingsDto getSettings() {
        return toDto(getOrCreate());
    }

    @Transactional
    public SaasPlatformSettingsDto updateSettings(SaasPlatformSettingsDto dto) {
        SaasPlatformSettings settings = getOrCreate();

        settings.setPlatformName(dto.getPlatformName());
        settings.setSupportEmail(dto.getSupportEmail());
        settings.setPublicDomain(dto.getPublicDomain());
        settings.setDefaultLocale(dto.getDefaultLocale());
        settings.setPlatformDescription(dto.getPlatformDescription());

        settings.setMaintenanceMode(dto.getMaintenanceMode());
        settings.setAllowOnboardingRequests(dto.getAllowOnboardingRequests());
        settings.setAutoApproveModuleRequests(dto.getAutoApproveModuleRequests());

        settings.setBrandPrimaryColor(dto.getBrandPrimaryColor());
        settings.setBrandSecondaryColor(dto.getBrandSecondaryColor());
        settings.setLogoUrl(dto.getLogoUrl());
        settings.setFaviconUrl(dto.getFaviconUrl());

        settings.setSmtpHost(dto.getSmtpHost());
        settings.setSmtpPort(dto.getSmtpPort());
        settings.setEmailFromAddress(dto.getEmailFromAddress());
        settings.setEmailFromName(dto.getEmailFromName());
        settings.setSmtpUsername(dto.getSmtpUsername());
        settings.setSmtpPassword(dto.getSmtpPassword());

        settings.setRequireMfaSuperAdmin(dto.getRequireMfaSuperAdmin());
        settings.setRequireMfaTenantAdmin(dto.getRequireMfaTenantAdmin());
        settings.setIpAllowListEnabled(dto.getIpAllowListEnabled());
        settings.setAuditSensitiveExports(dto.getAuditSensitiveExports());
        settings.setSessionTimeoutMinutes(dto.getSessionTimeoutMinutes());
        settings.setPasswordMinLength(dto.getPasswordMinLength());

        settings.setTenantDefaultTimezone(dto.getTenantDefaultTimezone());
        settings.setTenantDefaultLanguage(dto.getTenantDefaultLanguage());
        settings.setTenantUrlPattern(dto.getTenantUrlPattern());
        settings.setTenantStorageQuota(dto.getTenantStorageQuota());

        settings.setDefaultEnabledModulesCsv(dto.getDefaultEnabledModulesCsv());

        settings.setNotifyNewOrganizationRequests(dto.getNotifyNewOrganizationRequests());
        settings.setNotifyModuleRequests(dto.getNotifyModuleRequests());
        settings.setNotifyPaymentReceived(dto.getNotifyPaymentReceived());
        settings.setNotifyOverdueInvoices(dto.getNotifyOverdueInvoices());
        settings.setNotifySystemErrors(dto.getNotifySystemErrors());
        settings.setNotifyWeeklyDigest(dto.getNotifyWeeklyDigest());

        return toDto(repository.save(settings));
    }

    @Transactional
    public void seedDefaultsIfMissing() {
        getOrCreate();
    }

    private SaasPlatformSettings getOrCreate() {
        return repository.findById(SETTINGS_ID).orElseGet(() -> repository.save(defaultSettings()));
    }

    private SaasPlatformSettings defaultSettings() {
        return SaasPlatformSettings.builder()
                .id(SETTINGS_ID)
                .platformName("CIVOX")
                .supportEmail("support@civox.io")
                .publicDomain("civox.io")
                .defaultLocale("en-US")
                .platformDescription("Multi-tenant civic SaaS platform")
                .maintenanceMode(false)
                .allowOnboardingRequests(true)
                .autoApproveModuleRequests(false)
                .brandPrimaryColor("#7B2CBF")
                .brandSecondaryColor("#FF6B35")
                .logoUrl("")
                .faviconUrl("")
                .smtpHost("")
                .smtpPort(587)
                .emailFromAddress("noreply@civox.io")
                .emailFromName("CIVOX Platform")
                .smtpUsername("")
                .smtpPassword("")
                .requireMfaSuperAdmin(true)
                .requireMfaTenantAdmin(false)
                .ipAllowListEnabled(false)
                .auditSensitiveExports(true)
                .sessionTimeoutMinutes(30)
                .passwordMinLength(12)
                .tenantDefaultTimezone("UTC")
                .tenantDefaultLanguage("English")
                .tenantUrlPattern("{slug}.civox.io")
                .tenantStorageQuota("10 GB")
                .defaultEnabledModulesCsv("EVENTS,NEWS,SURVEYS")
                .notifyNewOrganizationRequests(true)
                .notifyModuleRequests(true)
                .notifyPaymentReceived(true)
                .notifyOverdueInvoices(true)
                .notifySystemErrors(true)
                .notifyWeeklyDigest(false)
                .build();
    }

    private SaasPlatformSettingsDto toDto(SaasPlatformSettings settings) {
        SaasPlatformSettingsDto dto = new SaasPlatformSettingsDto();

        dto.setPlatformName(settings.getPlatformName());
        dto.setSupportEmail(settings.getSupportEmail());
        dto.setPublicDomain(settings.getPublicDomain());
        dto.setDefaultLocale(settings.getDefaultLocale());
        dto.setPlatformDescription(settings.getPlatformDescription());

        dto.setMaintenanceMode(settings.getMaintenanceMode());
        dto.setAllowOnboardingRequests(settings.getAllowOnboardingRequests());
        dto.setAutoApproveModuleRequests(settings.getAutoApproveModuleRequests());

        dto.setBrandPrimaryColor(settings.getBrandPrimaryColor());
        dto.setBrandSecondaryColor(settings.getBrandSecondaryColor());
        dto.setLogoUrl(settings.getLogoUrl());
        dto.setFaviconUrl(settings.getFaviconUrl());

        dto.setSmtpHost(settings.getSmtpHost());
        dto.setSmtpPort(settings.getSmtpPort());
        dto.setEmailFromAddress(settings.getEmailFromAddress());
        dto.setEmailFromName(settings.getEmailFromName());
        dto.setSmtpUsername(settings.getSmtpUsername());
        dto.setSmtpPassword(settings.getSmtpPassword());

        dto.setRequireMfaSuperAdmin(settings.getRequireMfaSuperAdmin());
        dto.setRequireMfaTenantAdmin(settings.getRequireMfaTenantAdmin());
        dto.setIpAllowListEnabled(settings.getIpAllowListEnabled());
        dto.setAuditSensitiveExports(settings.getAuditSensitiveExports());
        dto.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
        dto.setPasswordMinLength(settings.getPasswordMinLength());

        dto.setTenantDefaultTimezone(settings.getTenantDefaultTimezone());
        dto.setTenantDefaultLanguage(settings.getTenantDefaultLanguage());
        dto.setTenantUrlPattern(settings.getTenantUrlPattern());
        dto.setTenantStorageQuota(settings.getTenantStorageQuota());

        dto.setDefaultEnabledModulesCsv(settings.getDefaultEnabledModulesCsv());

        dto.setNotifyNewOrganizationRequests(settings.getNotifyNewOrganizationRequests());
        dto.setNotifyModuleRequests(settings.getNotifyModuleRequests());
        dto.setNotifyPaymentReceived(settings.getNotifyPaymentReceived());
        dto.setNotifyOverdueInvoices(settings.getNotifyOverdueInvoices());
        dto.setNotifySystemErrors(settings.getNotifySystemErrors());
        dto.setNotifyWeeklyDigest(settings.getNotifyWeeklyDigest());

        return dto;
    }
}
