package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmotionalAnalysisRepository extends JpaRepository<EmotionalAnalysis, UUID> {
    
    List<EmotionalAnalysis> findByUsuarioIdOrderBySequenceNumberDesc(String usuarioId);
    
    Optional<EmotionalAnalysis> findByMessageId(UUID messageId);
    
    @Query("SELECT e FROM EmotionalAnalysis e WHERE e.usuarioId = :usuarioId " +
           "AND e.conversationId = :conversationId " +
           "AND e.sequenceNumber = :sequenceNumber")
    Optional<EmotionalAnalysis> findByConversationAndSequence(
        @Param("usuarioId") String usuarioId,
        @Param("conversationId") UUID conversationId,
        @Param("sequenceNumber") Integer sequenceNumber
    );
}


