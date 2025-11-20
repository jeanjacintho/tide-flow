package br.jeanjacintho.tideflow.ai_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "triggers", indexes = {
    @Index(name = "idx_trigger_usuario", columnList = "usuario_id"),
    @Index(name = "idx_trigger_tipo", columnList = "usuario_id, tipo"),
    @Index(name = "idx_trigger_impacto", columnList = "usuario_id, impacto DESC"),
    @Index(name = "idx_trigger_ultima_observacao", columnList = "data_ultima_observacao DESC"),
    @Index(name = "idx_trigger_frequencia", columnList = "usuario_id, frequencia DESC"),
    @Index(name = "idx_trigger_emocao", columnList = "usuario_id, emocao_associada")
})
public class Trigger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoGatilho tipo;
    
    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao; // Descrição do gatilho
    
    @Column(name = "impacto", nullable = false)
    private Integer impacto; // Impacto emocional (1-10, onde 10 é muito negativo, 1 é muito positivo)
    
    @Column(name = "frequencia", nullable = false)
    private Integer frequencia; // Quantas vezes o gatilho foi observado
    
    @Column(name = "emocao_associada", length = 50)
    private String emocaoAssociada; // Emoção mais comum quando o gatilho ocorre
    
    @Column(name = "contexto", columnDefinition = "TEXT")
    private String contexto; // Contexto onde o gatilho ocorre
    
    @Column(name = "positivo", nullable = false)
    private Boolean positivo; // Se é um gatilho positivo (melhora humor) ou negativo
    
    @Column(name = "data_primeira_observacao", nullable = false)
    private LocalDateTime dataPrimeiraObservacao;
    
    @Column(name = "data_ultima_observacao", nullable = false)
    private LocalDateTime dataUltimaObservacao;
    
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
    
    @PrePersist
    protected void onCreate() {
        if (dataPrimeiraObservacao == null) {
            dataPrimeiraObservacao = LocalDateTime.now();
        }
        if (dataUltimaObservacao == null) {
            dataUltimaObservacao = LocalDateTime.now();
        }
        if (ativo == null) {
            ativo = true;
        }
        if (frequencia == null) {
            frequencia = 1;
        }
        if (positivo == null) {
            positivo = false; // Default negativo, será ajustado na análise
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        dataUltimaObservacao = LocalDateTime.now();
    }
    
    public Trigger() {
    }
    
    public Trigger(String usuarioId, TipoGatilho tipo, String descricao, 
                   Integer impacto, Integer frequencia, String emocaoAssociada,
                   String contexto, Boolean positivo) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.impacto = impacto != null ? Math.max(1, Math.min(10, impacto)) : 5;
        this.frequencia = frequencia != null ? frequencia : 1;
        this.emocaoAssociada = emocaoAssociada;
        this.contexto = contexto;
        this.positivo = positivo != null ? positivo : false;
    }
    
    public void incrementarFrequencia() {
        this.frequencia++;
        this.dataUltimaObservacao = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
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
        this.impacto = impacto != null ? Math.max(1, Math.min(10, impacto)) : 5;
    }
    
    public Integer getFrequencia() {
        return frequencia;
    }
    
    public void setFrequencia(Integer frequencia) {
        this.frequencia = frequencia != null ? frequencia : 0;
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
        this.positivo = positivo != null ? positivo : false;
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
    
    public Boolean getAtivo() {
        return ativo;
    }
    
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo != null ? ativo : true;
    }
}

