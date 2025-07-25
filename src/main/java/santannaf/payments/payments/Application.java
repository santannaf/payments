package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Value("${fixed.pool.thread}")
    int fixedPoolThread;

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(fixedPoolThread, Thread.ofVirtual().factory());
    }

    @Bean
    public HttpClient httpClient(ExecutorService executorService) {
        return HttpClient.newBuilder()
                .executor(executorService)
                .build();
    }

    @Bean
    public Queue<Payment> paymentQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public CommandLineRunner commandLineRunner(HttpClient httpClient) {
        return _ -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/payments-summary?from=2000-01-01T00:00Z&to=2900-01-01T00:00Z"))
                    .timeout(Duration.ofMillis(999))
                    .GET()
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        };
    }
}
