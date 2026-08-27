package tn.esprit.tic.civiAgora.dto.billingDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dto.moduleDto.OrganizationModuleDto;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.dto.stripeDto.StripeCheckoutSessionDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBillingOverviewDto {
    private OrganizationSubscriptionDto subscription;
    private List<OrganizationModuleDto> activeModules;
    private List<ModuleRequestDto> pendingModuleRequests;
    private List<ModulePurchaseDto> modulePurchases;
    private List<StripeCheckoutSessionDto> recentPayments;
}
