package tn.esprit.tic.civiAgora.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization_requests")
public class OrganizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String organizationName;

    @Column(nullable = false)
    private String desiredSlug;

    @Column(nullable = false)
    private String contactPersonName;

    @Column(nullable = false)
    private String contactEmail;

    private String phone;

    private String address;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;

    private String adminFirstName;

    private String adminLastName;

    private String adminEmail;

    @JsonIgnore
    @Column(length = 120)
    private String adminPasswordHash;

    private Integer expectedNumberOfUsers;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "organization_request_modules",
            joinColumns = @JoinColumn(name = "request_id")
    )
    @Column(name = "module_code")
    @Builder.Default
    private List<String> requestedModuleCodes = new ArrayList<>();

    private String requestedPrimaryColor;

    private String requestedSecondaryColor;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String brandingNotes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", length = 32, columnDefinition = "varchar(32)")
    private OrganizationRequestStatus requestStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_status", length = 32, columnDefinition = "varchar(32)")
    private QuoteStatus quoteStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 32, columnDefinition = "varchar(32)")
    private PaymentStatus paymentStatus;

    private Boolean publicVisibilityRequested;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime reviewedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private String reviewedBy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declineReason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String approvalNotes;

    private String processedBy;

    private LocalDateTime quoteSentAt;

    private LocalDateTime approvedAt;

    private LocalDateTime declinedAt;

    private LocalDateTime paidAt;

    private LocalDateTime activatedAt;

    private BigDecimal quoteBaseFee;

    private BigDecimal quoteUserFee;

    private BigDecimal quoteModuleFee;

    private BigDecimal quoteSetupFee;

    private BigDecimal quoteTotal;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String quoteAssumptions;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String quoteAxesSnapshot;

    @Column(length = 120, unique = true)
    private String paymentTokenHash;

    private LocalDateTime paymentTokenCreatedAt;

    private Integer organizationCreatedId;
}
