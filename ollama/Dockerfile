FROM ollama/ollama:latest

# Script de inicialização que baixa o modelo
COPY init-ollama.sh /usr/local/bin/init-ollama.sh
RUN chmod +x /usr/local/bin/init-ollama.sh

# Sobrescreve o entrypoint para executar o script de inicialização
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["ollama serve & sleep 10 && /usr/local/bin/init-ollama.sh && wait"]


