package br.jeanjacintho.tideflow.ai_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memorias", indexes = {
    @Index(name = "idx_memoria_usuario", columnList = "usuario_id"),
    @Index(name = "idx_memoria_relevancia", columnList = "usuario_id, relevancia DESC"),
    @Index(name = "idx_memoria_ultima_referencia", columnList = "usuario_id, ultima_referencia")
})
public class Memoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    @Column(name = "conteudo", columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMemoria tipo;

    @Column(name = "contexto", columnDefinition = "TEXT")
    private String contexto;

    @Column(name = "relevancia", nullable = false)
    private Integer relevancia = 50;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "ultima_referencia")
    private LocalDateTime ultimaReferencia;

    @Column(name = "contador_referencias", nullable = false)
    private Integer contadorReferencias = 0;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (ultimaReferencia == null) {
            ultimaReferencia = dataCriacao;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (ultimaReferencia == null) {
            ultimaReferencia = LocalDateTime.now();
        }
    }

    public Memoria() {
    }

    public Memoria(String usuarioId, String conteudo, TipoMemoria tipo, String contexto, Integer relevancia) {
        this.usuarioId = usuarioId;
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.contexto = contexto;
        this.relevancia = relevancia != null ? relevancia : 50;
    }

    public void incrementarReferencia() {
        this.contadorReferencias++;
        this.ultimaReferencia = LocalDateTime.now();
    }

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

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public TipoMemoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoMemoria tipo) {
        this.tipo = tipo;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public Integer getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(Integer relevancia) {
        this.relevancia = relevancia != null ? Math.max(0, Math.min(100, relevancia)) : 50;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getUltimaReferencia() {
        return ultimaReferencia;
    }

    public void setUltimaReferencia(LocalDateTime ultimaReferencia) {
        this.ultimaReferencia = ultimaReferencia;
    }

    public Integer getContadorReferencias() {
        return contadorReferencias;
    }

    public void setContadorReferencias(Integer contadorReferencias) {
        this.contadorReferencias = contadorReferencias != null ? contadorReferencias : 0;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
