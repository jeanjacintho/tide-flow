package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.EmotionalPattern;
import br.jeanjacintho.tideflow.ai_service.model.TipoPadrao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionalPatternRepository extends JpaRepository<EmotionalPattern, Long> {

    List<EmotionalPattern> findByUsuarioIdAndAtivoTrueOrderByConfiancaDesc(String usuarioId);

    List<EmotionalPattern> findByUsuarioIdAndTipoAndAtivoTrue(String usuarioId, TipoPadrao tipo);

    @Query("SELECT p FROM EmotionalPattern p WHERE p.usuarioId = :usuarioId " +
           "AND p.padrao = :padrao AND p.tipo = :tipo AND p.ativo = true")
    EmotionalPattern findByUsuarioIdAndPadraoAndTipo(
        @Param("usuarioId") String usuarioId,
        @Param("padrao") String padrao,
        @Param("tipo") TipoPadrao tipo
    );

    @Query("SELECT p FROM EmotionalPattern p WHERE p.usuarioId = :usuarioId " +
           "AND p.ativo = true ORDER BY p.confianca DESC, p.ocorrencias DESC")
    List<EmotionalPattern> findPadroesMaisConfiaveis(@Param("usuarioId") String usuarioId);
}
