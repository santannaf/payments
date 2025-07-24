


# shellcheck disable=SC2046
docker rm -f $(docker ps -a -q) && docker system prune -f && \
docker compose -f ./payment-processor/docker-compose.yml up -d && \
docker compose -f ./docker-compose.yml up -d && \
k6 run ./rinha-test/rinha.js
