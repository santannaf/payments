#!/bin/bash

set -e  # Sai em erro de comando
set -o pipefail

APP_JAR="./build/libs/app.jar"
PROCESSOR_COMPOSE="./payment-processor/docker-compose-arm64.yml"
BACKEND_COMPOSE="./docker-compose-arm64.yml"

for i in {1..10}; do
    echo "=================================="
    echo "🔁 Execução $i de 10"
    echo "=================================="

    echo "🛑 Encerrando app Java anterior (se existir)..."
    JAVA_PID=$(pgrep -f "$APP_JAR" || true)
    if [[ -n "$JAVA_PID" ]]; then
        kill "$JAVA_PID" && echo "✅ Java encerrado (PID $JAVA_PID)"
        sleep 2
    fi

    echo "🧹 Limpando Docker..."
    docker rm -f $(docker ps -aq) 2>/dev/null || true
    docker system prune -f

    echo "🚀 Subindo serviços Docker..."
    docker compose -f "$PROCESSOR_COMPOSE" up -d
    docker compose -f "$BACKEND_COMPOSE" up -d

    echo "⏳ Aguardando containers subirem (5s)..."
    sleep 5

#    echo "☕ Iniciando aplicação Java..."
#    java -jar "$APP_JAR" &
#    APP_PID=$!
#    echo
#    echo "✅ Java iniciado (PID $APP_PID)"
    sleep 3

    echo "📈 Executando K6..."
    k6 run ./rinha-test/rinha.js

#    echo "🛑 Encerrando aplicação Java..."
#    kill $APP_PID
#    wait $APP_PID 2>/dev/null || true

    echo "✅ Execução $i finalizada com sucesso"
    echo
done

echo "🏁 Todas as execuções concluídas."
