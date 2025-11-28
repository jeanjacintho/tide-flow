#!/bin/bash

set -e

echo "🌱 Iniciando população do banco de dados Tide Flow..."
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "📦 1/3 - Populando User Service (usuários, empresa, departamentos)..."
cd user-service
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run -Dspring-boot.run.arguments=seed &
    USER_SERVICE_PID=$!
    sleep 20
    kill $USER_SERVICE_PID 2>/dev/null || true
    wait $USER_SERVICE_PID 2>/dev/null || true
else
    echo "⚠️  mvnw não encontrado. Execute manualmente: cd user-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments=seed"
fi
cd ..

echo ""
echo "💬 2/3 - Populando AI Service (conversas)..."
echo "   Aguardando user-service estar disponível..."
sleep 5

cd ai-service
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run -Dspring-boot.run.arguments=seed &
    AI_SERVICE_PID=$!
    sleep 20
    kill $AI_SERVICE_PID 2>/dev/null || true
    wait $AI_SERVICE_PID 2>/dev/null || true
else
    echo "⚠️  mvnw não encontrado. Execute manualmente: cd ai-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments=seed"
fi
cd ..

echo ""
echo "📊 3/3 - Populando AI Service (relatórios)..."
sleep 5

cd ai-service
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run -Dspring-boot.run.arguments=seed &
    AI_SERVICE_PID=$!
    sleep 20
    kill $AI_SERVICE_PID 2>/dev/null || true
    wait $AI_SERVICE_PID 2>/dev/null || true
else
    echo "⚠️  mvnw não encontrado. Execute manualmente: cd ai-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments=seed"
fi
cd ..

echo ""
echo "✅ População do banco de dados concluída!"
echo ""
echo "📋 Resumo:"
echo "   ✅ Usuário root: root / root123"
echo "   ✅ Empresa: moredevs"
echo "   ✅ Funcionários: 15"
echo "   ✅ Departamentos: 6"
echo "   ✅ Conversas: ~45-60"
echo "   ✅ Relatórios: 5"
echo ""
echo "🔑 Credenciais de acesso:"
echo "   Root: root@tideflow.com / root123"
echo "   Funcionários: [nome]@moredevs.com / senha123"
echo ""
echo "💡 Exemplo de login de funcionário:"
echo "   Email: joao.silva@moredevs.com"
echo "   Senha: senha123"
