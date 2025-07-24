package santannaf.payments.payments;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public class Payment {
    private String correlationId;
    private float amount;
    private final Instant requestedAt = Instant.now().atZone(ZoneOffset.UTC).toInstant();

    public String getCorrelationId() {
        return correlationId;
    }

    public float getAmount() {
        return amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public final Long getEpochMilli() {
        return requestedAt.toEpochMilli();
    }

    public final String json() {
        return String.format(Locale.US, """
                {
                    "correlationId": "%s",
                    "amount": %.2f,
                    "requestedAt": "%s"
                }
                """, correlationId, amount, requestedAt);
    }

    public final String getEntryJson() {
        return amount + "|1|" + UUID.randomUUID();
    }

    public final String getKey() {
        return "summary:default";
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
