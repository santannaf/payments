package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Value("${fixed.pool.thread}")
    int fixedPoolThread;

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .executor(Executors.newFixedThreadPool(fixedPoolThread, Thread.ofVirtual().factory()))
                .build();
    }

    @Bean
    public Queue<Payment> paymentQueue() {
        return new ConcurrentLinkedQueue<>();
    }
}
