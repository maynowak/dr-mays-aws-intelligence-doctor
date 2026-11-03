package io.github.maynowak.drmays.core.rule.builtin;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.DiagnosticRule;
import io.github.maynowak.drmays.core.rule.RuleId;

import java.util.Map;
import java.util.Objects;

/**
 * Flags network resources with the synthetic attribute {@code ingress.public=true}.
 *
 * <p>This is a demonstration rule for the domain foundation, not a complete
 * AWS security-group analysis.</p>
 */
public final class PublicIngressRule implements DiagnosticRule {

    public static final RuleId RULE_ID = new RuleId("NET-PUBLIC-001");
    private static final String RESOURCE_TYPE = "network";
    private static final String ATTRIBUTE = "ingress.public";

    @Override
    public RuleId id() {
        return RULE_ID;
    }

    @Override
    public String description() {
        return "Checks whether a network resource declares public ingress";
    }

    @Override
    public DiagnosticOutcome evaluate(InfrastructureResource resource, DiagnosticContext context) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(context, "context");

        if (!RESOURCE_TYPE.equals(resource.identity().resourceType())) {
            return new DiagnosticOutcome.NotApplicable(
                    RULE_ID, resource.identity(), "resource type is not " + RESOURCE_TYPE);
        }

        var ingress = resource.attribute(ATTRIBUTE);
        if (ingress.isEmpty()) {
            return new DiagnosticOutcome.NotApplicable(
                    RULE_ID, resource.identity(), ATTRIBUTE + " is not declared");
        }

        return switch (ingress.get()) {
            case "true" -> new DiagnosticOutcome.FindingDetected(publicIngressFinding(resource));
            case "false" -> new DiagnosticOutcome.NoFinding(
                    RULE_ID, resource.identity(), "public ingress is disabled");
            default -> new DiagnosticOutcome.NotApplicable(
                    RULE_ID, resource.identity(), ATTRIBUTE + " must be 'true' or 'false'");
        };
    }

    private DiagnosticFinding publicIngressFinding(InfrastructureResource resource) {
        var evidence = new Evidence(
                resource.identity(),
                "resource-attribute",
                ATTRIBUTE + "=true",
                ATTRIBUTE + "=false",
                Map.of("attribute", ATTRIBUTE, "value", "true"));
        return new DiagnosticFinding(
                RULE_ID,
                version(),
                Severity.HIGH,
                resource.identity(),
                "Network resource allows public ingress",
                "The resource declares public ingress.",
                evidence,
                "Restrict ingress to explicitly trusted sources.");
    }
}
