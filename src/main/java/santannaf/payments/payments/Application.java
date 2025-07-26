package santannaf.payments.payments;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .executor(Runnable::run)
                .build();
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
