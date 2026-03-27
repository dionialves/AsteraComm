#!/bin/bash
set -e

TRIGGER_FILE=/app/src/main/resources/.reload-trigger

echo "[DEV] Compilando projeto..."
mvn compile -q

echo "[DEV] Iniciando Spring Boot com DevTools..."
mvn spring-boot:run &

echo "[DEV] Monitorando alteracoes em src/main..."
touch /tmp/last_compile
touch /tmp/last_resources

while true; do
    sleep 2

    # Alteracoes em Java/SQL/properties → recompila e dispara restart
    CHANGED_JAVA=$(find /app/src/main \( -name "*.java" -o -name "*.sql" -o -name "*.properties" \) \
        ! -name ".reload-trigger" -newer /tmp/last_compile 2>/dev/null | wc -l)
    if [ "$CHANGED_JAVA" -gt 0 ]; then
        echo "[DEV] Alteracao em Java/config detectada. Recompilando..."
        touch /tmp/last_compile
        touch /tmp/last_resources
        mvn process-resources compile -q 2>&1 || echo "[DEV] Erro na compilacao — verifique o codigo"
        touch "$TRIGGER_FILE"
        continue
    fi

    # Alteracoes em HTML/CSS → copia para target/classes (sem restart; cache=false resolve na proxima request)
    CHANGED_RES=$(find /app/src/main/resources \( -name "*.html" -o -name "*.css" \) \
        -newer /tmp/last_resources 2>/dev/null | wc -l)
    if [ "$CHANGED_RES" -gt 0 ]; then
        echo "[DEV] Alteracao em template/CSS detectada. Sincronizando resources..."
        touch /tmp/last_resources
        mvn process-resources -q 2>&1 || echo "[DEV] Erro ao sincronizar resources"
    fi
done
