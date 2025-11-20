package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.model.TipoGatilho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriggerRepository extends JpaRepository<Trigger, Long> {
    
    List<Trigger> findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(String usuarioId);
    
    List<Trigger> findByUsuarioIdAndTipoAndAtivoTrue(String usuarioId, TipoGatilho tipo);
    
    List<Trigger> findByUsuarioIdAndPositivoAndAtivoTrue(String usuarioId, Boolean positivo);
    
    @Query("SELECT t FROM Trigger t WHERE t.usuarioId = :usuarioId " +
           "AND LOWER(t.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')) " +
           "AND t.ativo = true")
    Optional<Trigger> findByUsuarioIdAndDescricaoSimilar(
        @Param("usuarioId") String usuarioId,
        @Param("descricao") String descricao
    );
    
    @Query("SELECT t FROM Trigger t WHERE t.usuarioId = :usuarioId " +
           "AND t.ativo = true ORDER BY t.impacto DESC, t.frequencia DESC")
    List<Trigger> findGatilhosMaisImpactantes(@Param("usuarioId") String usuarioId);
}

