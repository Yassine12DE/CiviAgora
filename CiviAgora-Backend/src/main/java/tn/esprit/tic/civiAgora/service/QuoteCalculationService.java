package tn.esprit.tic.civiAgora.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class QuoteCalculationService {

    private final BigDecimal basePlatformFee;
    private final BigDecimal setupFee;
    private final BigDecimal perModuleFee;
    private final BigDecimal smallUserRate;
    private final BigDecimal mediumUserRate;
    private final BigDecimal largeUserRate;

    public QuoteCalculationService(
            @Value("${civox.pricing.base-platform-fee:499.00}") BigDecimal basePlatformFee,
            @Value("${civox.pricing.setup-fee:750.00}") BigDecimal setupFee,
            @Value("${civox.pricing.per-module-fee:120.00}") BigDecimal perModuleFee,
            @Value("${civox.pricing.user-band-small-rate:6.50}") BigDecimal smallUserRate,
            @Value("${civox.pricing.user-band-medium-rate:5.20}") BigDecimal mediumUserRate,
            @Value("${civox.pricing.user-band-large-rate:4.10}") BigDecimal largeUserRate
    ) {
        this.basePlatformFee = money(basePlatformFee);
        this.setupFee = money(setupFee);
        this.perModuleFee = money(perModuleFee);
        this.smallUserRate = money(smallUserRate);
        this.mediumUserRate = money(mediumUserRate);
        this.largeUserRate = money(largeUserRate);
    }

    public QuoteBreakdown calculate(OrganizationRequest request) {
        int expectedUsers = Math.max(1, request.getExpectedNumberOfUsers() == null ? 1 : request.getExpectedNumberOfUsers());
        int moduleCount = Math.max(1, request.getRequestedModuleCodes() == null ? 0 : request.getRequestedModuleCodes().size());
        BigDecimal userRate = resolveUserRate(expectedUsers);
        String userBand = resolveUserBand(expectedUsers);

        BigDecimal userFee = money(userRate.multiply(BigDecimal.valueOf(expectedUsers)));
        BigDecimal moduleFee = money(perModuleFee.multiply(BigDecimal.valueOf(moduleCount)));
        BigDecimal total = money(basePlatformFee.add(userFee).add(moduleFee).add(setupFee));

        String assumptions = String.format(
                "Base platform fee %s, setup fee %s, %s users in %s band at %s/user, %s modules at %s/module.",
                basePlatformFee,
                setupFee,
                expectedUsers,
                userBand,
                userRate,
                moduleCount,
                perModuleFee
        );
        String axesSnapshot = String.format(
                "{\"expectedUsers\":%d,\"moduleCount\":%d,\"userBand\":\"%s\",\"moduleCodes\":%s}",
                expectedUsers,
                moduleCount,
                userBand,
                quoteJsonArray(request.getRequestedModuleCodes())
        );

        return new QuoteBreakdown(
                basePlatformFee,
                userFee,
                moduleFee,
                setupFee,
                total,
                assumptions,
                axesSnapshot
        );
    }

    public void applyQuote(OrganizationRequest request) {
        QuoteBreakdown quote = calculate(request);
        request.setQuoteBaseFee(quote.baseFee());
        request.setQuoteUserFee(quote.userFee());
        request.setQuoteModuleFee(quote.moduleFee());
        request.setQuoteSetupFee(quote.setupFee());
        request.setQuoteTotal(quote.total());
        request.setQuoteAssumptions(quote.assumptions());
        request.setQuoteAxesSnapshot(quote.axesSnapshot());
    }

    private BigDecimal resolveUserRate(int expectedUsers) {
        if (expectedUsers <= 100) {
            return smallUserRate;
        }
        if (expectedUsers <= 500) {
            return mediumUserRate;
        }
        return largeUserRate;
    }

    private String resolveUserBand(int expectedUsers) {
        if (expectedUsers <= 100) {
            return "1-100";
        }
        if (expectedUsers <= 500) {
            return "101-500";
        }
        return "501+";
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String quoteJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }

        return values.stream()
                .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
                .reduce((left, right) -> left + "," + right)
                .map(value -> "[" + value + "]")
                .orElse("[]");
    }

    public record QuoteBreakdown(
            BigDecimal baseFee,
            BigDecimal userFee,
            BigDecimal moduleFee,
            BigDecimal setupFee,
            BigDecimal total,
            String assumptions,
            String axesSnapshot
    ) {
    }
}
