package santannaf.payments.payments;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

@Component
public class PaymentWorker {
    private final Queue<Payment> queue;
    private final PaymentProcess paymentProcess;

    public PaymentWorker(Queue<Payment> queue, PaymentProcess paymentProcess) {
        this.queue = queue;
        this.paymentProcess = paymentProcess;
    }

    @Value("${fixed.pool.thread}") int fixedPoolThread;

    @PostConstruct
    void start() {
        for (int i = 0; i < fixedPoolThread; i++) {
            Thread.startVirtualThread(this::consumer);
        }
    }

    private void consumer() {
        while (true) {
            Payment payment = queue.poll();
            if (payment != null) paymentProcess.sendPayment(payment);
            else LockSupport.parkNanos(1_000_000L);
        }
    }
}
