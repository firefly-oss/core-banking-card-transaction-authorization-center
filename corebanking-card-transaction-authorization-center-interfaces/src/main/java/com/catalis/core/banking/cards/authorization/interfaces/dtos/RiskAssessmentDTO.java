package com.catalis.core.banking.cards.authorization.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing a risk assessment for a transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Risk assessment for a transaction")
public class RiskAssessmentDTO {

    @Schema(description = "Risk score (0-100, higher means higher risk)", example = "25")
    private Integer riskScore;

    @Schema(description = "Risk level (LOW, MEDIUM, HIGH)", example = "LOW")
    private String riskLevel;

    @Schema(description = "Recommendation (APPROVE, DECLINE, CHALLENGE)", example = "APPROVE")
    private String recommendation;

    @Schema(description = "Reason for the recommendation", example = "Transaction pattern consistent with customer history")
    private String reason;

    @Schema(description = "List of triggered rules", example = "[\"unusual_location\", \"high_value_transaction\"]")
    private List<String> triggeredRules;

    @Schema(description = "Velocity check results")
    private Map<String, Object> velocityChecks;

    @Schema(description = "Geolocation match status", example = "true")
    private Boolean geolocationMatch;

    @Schema(description = "Device fingerprint match status", example = "true")
    private Boolean deviceFingerprintMatch;

    @Schema(description = "IP address risk assessment", example = "LOW")
    private String ipAddressRisk;

    @Schema(description = "Additional risk factors")
    private Map<String, Object> additionalFactors;
}
