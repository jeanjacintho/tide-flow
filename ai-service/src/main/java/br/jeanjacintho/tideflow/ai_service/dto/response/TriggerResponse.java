package br.jeanjacintho.tideflow.ai_service.dto.response;

import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.model.TipoGatilho;

import java.time.LocalDateTime;

public class TriggerResponse {
    private Long id;
    private TipoGatilho tipo;
    private String descricao;
    private Integer impacto;
    private Integer frequencia;
    private String emocaoAssociada;
    private String contexto;
    private Boolean positivo;
    private LocalDateTime dataPrimeiraObservacao;
    private LocalDateTime dataUltimaObservacao;

    public TriggerResponse() {
    }

    public TriggerResponse(Trigger trigger) {
        this.id = trigger.getId();
        this.tipo = trigger.getTipo();
        this.descricao = trigger.getDescricao();
        this.impacto = trigger.getImpacto();
        this.frequencia = trigger.getFrequencia();
        this.emocaoAssociada = trigger.getEmocaoAssociada();
        this.contexto = trigger.getContexto();
        this.positivo = trigger.getPositivo();
        this.dataPrimeiraObservacao = trigger.getDataPrimeiraObservacao();
        this.dataUltimaObservacao = trigger.getDataUltimaObservacao();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoGatilho getTipo() {
        return tipo;
    }

    public void setTipo(TipoGatilho tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getImpacto() {
        return impacto;
    }

    public void setImpacto(Integer impacto) {
        this.impacto = impacto;
    }

    public Integer getFrequencia() {
        return frequencia;
    }

    public void setFrequencia(Integer frequencia) {
        this.frequencia = frequencia;
    }

    public String getEmocaoAssociada() {
        return emocaoAssociada;
    }

    public void setEmocaoAssociada(String emocaoAssociada) {
        this.emocaoAssociada = emocaoAssociada;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public Boolean getPositivo() {
        return positivo;
    }

    public void setPositivo(Boolean positivo) {
        this.positivo = positivo;
    }

    public LocalDateTime getDataPrimeiraObservacao() {
        return dataPrimeiraObservacao;
    }

    public void setDataPrimeiraObservacao(LocalDateTime dataPrimeiraObservacao) {
        this.dataPrimeiraObservacao = dataPrimeiraObservacao;
    }

    public LocalDateTime getDataUltimaObservacao() {
        return dataUltimaObservacao;
    }

    public void setDataUltimaObservacao(LocalDateTime dataUltimaObservacao) {
        this.dataUltimaObservacao = dataUltimaObservacao;
    }
}
