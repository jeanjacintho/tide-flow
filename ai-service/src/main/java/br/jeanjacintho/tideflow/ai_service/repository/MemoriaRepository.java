package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.Memoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemoriaRepository extends JpaRepository<Memoria, Long> {
    
    List<Memoria> findByUsuarioIdOrderByRelevanciaDesc(String usuarioId);
    
    @Query("SELECT m FROM Memoria m WHERE m.usuarioId = :usuarioId ORDER BY m.relevancia DESC, m.contadorReferencias DESC")
    List<Memoria> findMemoriasRelevantes(@Param("usuarioId") String usuarioId);
    
    @Query("SELECT m FROM Memoria m WHERE m.usuarioId = :usuarioId ORDER BY m.relevancia DESC, m.contadorReferencias DESC")
    Page<Memoria> findMemoriasRelevantes(@Param("usuarioId") String usuarioId, Pageable pageable);
    
    @Query("SELECT m FROM Memoria m WHERE m.usuarioId = :usuarioId " +
           "AND (m.ultimaReferencia IS NULL OR m.ultimaReferencia < :dataLimite) " +
           "ORDER BY m.relevancia DESC, m.dataCriacao DESC")
    List<Memoria> findMemoriasNaoReferenciadasRecentemente(
        @Param("usuarioId") String usuarioId,
        @Param("dataLimite") LocalDateTime dataLimite
    );
    
    @Query("SELECT m FROM Memoria m WHERE m.usuarioId = :usuarioId " +
           "AND (m.ultimaReferencia IS NULL OR m.ultimaReferencia < :dataLimite) " +
           "ORDER BY m.relevancia DESC, m.dataCriacao DESC")
    Page<Memoria> findMemoriasNaoReferenciadasRecentemente(
        @Param("usuarioId") String usuarioId,
        @Param("dataLimite") LocalDateTime dataLimite,
        Pageable pageable
    );
    
    @Query("SELECT m FROM Memoria m WHERE m.usuarioId = :usuarioId " +
           "AND m.tipo = :tipo " +
           "ORDER BY m.relevancia DESC")
    List<Memoria> findByUsuarioIdAndTipo(@Param("usuarioId") String usuarioId, 
                                         @Param("tipo") br.jeanjacintho.tideflow.ai_service.model.TipoMemoria tipo);
    
    Page<Memoria> findByUsuarioIdOrderByRelevanciaDesc(String usuarioId, Pageable pageable);
}

