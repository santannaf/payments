package santannaf.payments.payments;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(path = {"/payments-summary"})
public class PaymentsSummaryController {
    private final RedisTemplate<String, Object> redisTemplate;

    public PaymentsSummaryController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private final String[] processors = {"default", "fallback"};
    private static final long TIME_MAX = Long.MAX_VALUE;

    @GetMapping
    Map<String, Map<String, Object>> getPaymentSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        long fromMillis = (from != null) ? from.toInstant(ZoneOffset.UTC).toEpochMilli() : 0;
        long toMillis = (to != null) ? to.toInstant(ZoneOffset.UTC).toEpochMilli() : TIME_MAX;
        Map<String, Map<String, Object>> summary = new HashMap<>();

        for (String processor : processors) {
            Set<Object> entries = fetchPaymentsByTimeFrame(processor, fromMillis, toMillis);
            extracted(processor, entries, summary);
        }

        return summary;
    }

    private static void extracted(String processor, Set<Object> entries, Map<String, Map<String, Object>> summary) {
        double totalAmount = 0;
        int totalRequests = 0;

        if (entries == null) return;

        for (Object entry : entries) {
            String[] parts = ((String) entry).split("\\|");
            try {
                double amount = Double.parseDouble(parts[0]);
                int requests = Integer.parseInt(parts[1]);
                totalAmount += amount;
                totalRequests += requests;
            } catch (NumberFormatException ignored) {
            }
        }

        Map<String, Object> processorSummary = new HashMap<>();
        processorSummary.put("totalAmount", Math.round(totalAmount * 10.0) / 10.0);
        processorSummary.put("totalRequests", totalRequests);
        summary.put(processor, processorSummary);
    }

    private Set<Object> fetchPaymentsByTimeFrame(String processor, long fromMillis, long toMillis) {
        String key = "summary:" + processor;
        return redisTemplate.opsForZSet().rangeByScore(key, fromMillis, toMillis);
    }
}
