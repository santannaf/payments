package santannaf.payments.payments;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonFormat.*;

public class Payment {
    private String correlationId;
    private float amount;

//    private final long epochMillis;
//    private final String requestedAtFormatted;

    @JsonFormat(shape = Shape.STRING, timezone = "UTC")
    private final Instant requestedAt = Instant.now();

//    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
//
//    public Payment(String correlationId, float amount) {
//        Instant requestedAt = Instant.now();
//        this.epochMillis = requestedAt.toEpochMilli();
//        this.requestedAtFormatted = FORMATTER.format(requestedAt);
//        this.correlationId = correlationId;
//        this.amount = amount;
//    }

    public String getCorrelationId() {
        return correlationId;
    }

    public float getAmount() {
        return amount;
    }

//    public long getEpochMilli() {
//        return epochMillis;
//    }
//
//    public String getRequestedAt() {
//        return requestedAtFormatted;
//    }

    public String json() {
        return String.format(Locale.US, "{\"correlationId\": \"%s\",\"amount\": %.1f,\"requestedAt\": \"%s\"}",
                getCorrelationId(), getAmount(), getRequestedAt());
    }

    public final String getEntryJson() {
        return amount + "|1|" + UUID.randomUUID();
    }

    public final String getKeyDefault() {
        return "summary:default";
    }

    public final String getKeyFallback() {
        return "summary:fallback";
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
