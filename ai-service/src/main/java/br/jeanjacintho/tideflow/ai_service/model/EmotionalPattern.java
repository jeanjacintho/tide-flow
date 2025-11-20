package br.jeanjacintho.tideflow.ai_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emotional_patterns", indexes = {
    @Index(name = "idx_pattern_usuario", columnList = "usuario_id"),
    @Index(name = "idx_pattern_tipo", columnList = "usuario_id, tipo")
})
public class EmotionalPattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoPadrao tipo;
    
    @Column(name = "padrao", columnDefinition = "TEXT", nullable = false)
    private String padrao; // Descrição do padrão: "Segunda-feira", "Manhã", "Inverno", etc.
    
    @Column(name = "emocao_principal", length = 50)
    private String emocaoPrincipal; // Emoção mais comum neste padrão
    
    @Column(name = "intensidade_media")
    private Double intensidadeMedia; // Intensidade média da emoção
    
    @Column(name = "confianca", nullable = false)
    private Double confianca; // Confiança no padrão (0-100)
    
    @Column(name = "ocorrencias", nullable = false)
    private Integer ocorrencias; // Quantas vezes o padrão foi observado
    
    @Column(name = "data_inicio")
    private LocalDateTime dataInicio; // Quando o padrão começou a ser observado
    
    @Column(name = "data_fim")
    private LocalDateTime dataFim; // Quando o padrão parou (se aplicável)
    
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true; // Se o padrão ainda está ativo
    
    @Column(name = "ultima_atualizacao", nullable = false)
    private LocalDateTime ultimaAtualizacao;
    
    @PrePersist
    protected void onCreate() {
        if (ultimaAtualizacao == null) {
            ultimaAtualizacao = LocalDateTime.now();
        }
        if (dataInicio == null) {
            dataInicio = LocalDateTime.now();
        }
        if (ativo == null) {
            ativo = true;
        }
        if (ocorrencias == null) {
            ocorrencias = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        ultimaAtualizacao = LocalDateTime.now();
    }
    
    public EmotionalPattern() {
    }
    
    public EmotionalPattern(String usuarioId, TipoPadrao tipo, String padrao, 
                           String emocaoPrincipal, Double intensidadeMedia, 
                           Double confianca, Integer ocorrencias) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.padrao = padrao;
        this.emocaoPrincipal = emocaoPrincipal;
        this.intensidadeMedia = intensidadeMedia;
        this.confianca = confianca;
        this.ocorrencias = ocorrencias;
    }
    
    public void incrementarOcorrencia() {
        this.ocorrencias++;
        this.ultimaAtualizacao = LocalDateTime.now();
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
        this.confianca = confianca != null ? Math.max(0.0, Math.min(100.0, confianca)) : 0.0;
    }
    
    public Integer getOcorrencias() {
        return ocorrencias;
    }
    
    public void setOcorrencias(Integer ocorrencias) {
        this.ocorrencias = ocorrencias != null ? ocorrencias : 0;
    }
    
    public LocalDateTime getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public LocalDateTime getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }
    
    public Boolean getAtivo() {
        return ativo;
    }
    
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo != null ? ativo : true;
    }
    
    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }
    
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}

