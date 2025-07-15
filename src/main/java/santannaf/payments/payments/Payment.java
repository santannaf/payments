package santannaf.payments.payments;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Payment {
    private String correlationId;
    private float amount;
    private Instant requestedAt = Instant.now().atZone(ZoneOffset.UTC).toInstant();

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant parseToInstant(String dateTimeStr) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return localDateTime.toInstant(ZoneOffset.UTC);
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
