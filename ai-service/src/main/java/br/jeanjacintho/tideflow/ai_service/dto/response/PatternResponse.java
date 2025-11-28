package br.jeanjacintho.tideflow.ai_service.dto.response;

import br.jeanjacintho.tideflow.ai_service.model.EmotionalPattern;
import br.jeanjacintho.tideflow.ai_service.model.TipoPadrao;

import java.time.LocalDateTime;

public class PatternResponse {
    private Long id;
    private TipoPadrao tipo;
    private String padrao;
    private String emocaoPrincipal;
    private Double intensidadeMedia;
    private Double confianca;
    private Integer ocorrencias;
    private LocalDateTime dataInicio;
    private LocalDateTime ultimaAtualizacao;

    public PatternResponse() {
    }

    public PatternResponse(EmotionalPattern pattern) {
        this.id = pattern.getId();
        this.tipo = pattern.getTipo();
        this.padrao = pattern.getPadrao();
        this.emocaoPrincipal = pattern.getEmocaoPrincipal();
        this.intensidadeMedia = pattern.getIntensidadeMedia();
        this.confianca = pattern.getConfianca();
        this.ocorrencias = pattern.getOcorrencias();
        this.dataInicio = pattern.getDataInicio();
        this.ultimaAtualizacao = pattern.getUltimaAtualizacao();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoPadrao getTipo() {
        return tipo;
    }

    public void setTipo(TipoPadrao tipo) {
        this.tipo = tipo;
    }

    public String getPadrao() {
        return padrao;
    }

    public void setPadrao(String padrao) {
        this.padrao = padrao;
    }

    public String getEmocaoPrincipal() {
        return emocaoPrincipal;
    }

    public void setEmocaoPrincipal(String emocaoPrincipal) {
        this.emocaoPrincipal = emocaoPrincipal;
    }

    public Double getIntensidadeMedia() {
        return intensidadeMedia;
    }

    public void setIntensidadeMedia(Double intensidadeMedia) {
        this.intensidadeMedia = intensidadeMedia;
    }

    public Double getConfianca() {
        return confianca;
    }

    public void setConfianca(Double confianca) {
        this.confianca = confianca;
    }

    public Integer getOcorrencias() {
        return ocorrencias;
    }

    public void setOcorrencias(Integer ocorrencias) {
        this.ocorrencias = ocorrencias;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}
