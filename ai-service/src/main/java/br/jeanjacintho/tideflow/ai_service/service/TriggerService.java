package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.model.TipoGatilho;
import br.jeanjacintho.tideflow.ai_service.repository.TriggerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class TriggerService {

    private static final Logger logger = LoggerFactory.getLogger(TriggerService.class);

    private final TriggerRepository triggerRepository;

    public TriggerService(TriggerRepository triggerRepository) {
        this.triggerRepository = triggerRepository;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> processarGatilhos(String usuarioId, Map<String, Object> memoriaData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> gatilhosData = (List<Map<String, Object>>) memoriaData.get("gatilhos");

                if (gatilhosData == null || gatilhosData.isEmpty()) {
                    return null;
                }

                for (Map<String, Object> gatilhoData : gatilhosData) {
                    try {
                        Trigger trigger = criarTriggerFromData(usuarioId, gatilhoData);

                        Optional<Trigger> existingTrigger = triggerRepository
                                .findByUsuarioIdAndDescricaoSimilar(usuarioId, trigger.getDescricao());

                        if (existingTrigger.isPresent()) {

                            Trigger existing = existingTrigger.get();
                            existing.incrementarFrequencia();

                            if (trigger.getImpacto() > existing.getImpacto()) {
                                existing.setImpacto(trigger.getImpacto());
                            }

                            if (trigger.getEmocaoAssociada() != null &&
                                !trigger.getEmocaoAssociada().equals(existing.getEmocaoAssociada())) {
                                existing.setEmocaoAssociada(trigger.getEmocaoAssociada());
                            }
                            triggerRepository.save(existing);
                            logger.info("Gatilho atualizado: {} - {}", existing.getTipo(), existing.getDescricao());
                        } else {

                            triggerRepository.save(trigger);
                            logger.info("Gatilho salvo: {} - {}", trigger.getTipo(), trigger.getDescricao());
                        }
                    } catch (Exception e) {
                        logger.error("Erro ao salvar gatilho individual: {}", e.getMessage(), e);
                    }
                }

                return null;
            } catch (Exception e) {
                logger.error("Erro ao processar gatilhos: {}", e.getMessage(), e);
                return null;
            }
        });
    }

    private Trigger criarTriggerFromData(String usuarioId, Map<String, Object> data) {
        String tipoStr = (String) data.get("tipo");
        TipoGatilho tipo;
        try {
            tipo = TipoGatilho.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            tipo = TipoGatilho.SITUACAO;
        }

        String descricao = (String) data.get("descricao");
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do gatilho não pode ser vazia");
        }

        Integer impacto = 5;
        Object impactoObj = data.get("impacto");
        if (impactoObj instanceof Number) {
            impacto = ((Number) impactoObj).intValue();
        }

        String emocaoAssociada = (String) data.get("emocaoAssociada");
        String contexto = (String) data.get("contexto");

        Boolean positivo = false;
        Object positivoObj = data.get("positivo");
        if (positivoObj instanceof Boolean) {
            positivo = (Boolean) positivoObj;
        } else if (positivoObj instanceof String) {
            positivo = Boolean.parseBoolean((String) positivoObj);
        }

        return new Trigger(usuarioId, tipo, descricao, impacto, 1, emocaoAssociada, contexto, positivo);
    }

    @Transactional(readOnly = true)
    public List<Trigger> getGatilhos(String usuarioId) {
        return triggerRepository.findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Trigger> getGatilhosPositivos(String usuarioId) {
        return triggerRepository.findByUsuarioIdAndPositivoAndAtivoTrue(usuarioId, true);
    }

    @Transactional(readOnly = true)
    public List<Trigger> getGatilhosNegativos(String usuarioId) {
        return triggerRepository.findByUsuarioIdAndPositivoAndAtivoTrue(usuarioId, false);
    }
}
