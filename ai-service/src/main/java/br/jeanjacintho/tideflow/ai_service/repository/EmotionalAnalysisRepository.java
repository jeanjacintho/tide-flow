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
    
    @Query("SELECT e FROM EmotionalAnalysis e WHERE e.departmentId = :departmentId " +
           "AND e.createdAt >= :startDateTime AND e.createdAt < :endDateTime")
    List<EmotionalAnalysis> findByDepartmentIdAndDateRange(
        @Param("departmentId") UUID departmentId,
        @Param("startDateTime") java.time.LocalDateTime startDateTime,
        @Param("endDateTime") java.time.LocalDateTime endDateTime
    );
    
    @Query("SELECT e FROM EmotionalAnalysis e WHERE e.companyId = :companyId " +
           "AND e.createdAt >= :startDateTime AND e.createdAt < :endDateTime")
    List<EmotionalAnalysis> findByCompanyIdAndDateRange(
        @Param("companyId") UUID companyId,
        @Param("startDateTime") java.time.LocalDateTime startDateTime,
        @Param("endDateTime") java.time.LocalDateTime endDateTime
    );
    
    @Query(value = "SELECT COUNT(DISTINCT e.usuario_id) FROM emotional_analysis e WHERE e.department_id = :departmentId " +
           "AND DATE(e.created_at) = :date", nativeQuery = true)
    Long countUniqueUsersByDepartmentAndDate(
        @Param("departmentId") UUID departmentId,
        @Param("date") java.time.LocalDate date
    );
    
    @Query(value = "SELECT COUNT(DISTINCT e.conversation_id) FROM emotional_analysis e WHERE e.department_id = :departmentId " +
           "AND DATE(e.created_at) = :date", nativeQuery = true)
    Long countConversationsByDepartmentAndDate(
        @Param("departmentId") UUID departmentId,
        @Param("date") java.time.LocalDate date
    );
    
    @Query(value = "SELECT COUNT(*) FROM emotional_analysis e WHERE e.department_id = :departmentId " +
           "AND DATE(e.created_at) = :date", nativeQuery = true)
    Long countMessagesByDepartmentAndDate(
        @Param("departmentId") UUID departmentId,
        @Param("date") java.time.LocalDate date
    );
    
    @Query(value = "SELECT DISTINCT e.department_id FROM emotional_analysis e " +
           "WHERE e.department_id IS NOT NULL AND DATE(e.created_at) = :date", nativeQuery = true)
    List<UUID> findDistinctDepartmentIdsByDate(@Param("date") java.time.LocalDate date);
    
    @Query(value = "SELECT DISTINCT e.company_id FROM emotional_analysis e " +
           "WHERE e.company_id IS NOT NULL AND DATE(e.created_at) = :date", nativeQuery = true)
    List<UUID> findDistinctCompanyIdsByDate(@Param("date") java.time.LocalDate date);
}


