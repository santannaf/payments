package santannaf.payments.payments;

import java.time.Instant;
import java.time.ZoneOffset;

public class Payment {
    private String correlationId;
    private float amount;
    private Instant requestedAt = Instant.now().atZone(ZoneOffset.UTC).toInstant();

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public float getAmount() {
        return amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
