package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByIdAndUserId(UUID id, String userId);

    List<Conversation> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Conversation> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId, LocalDateTime after);

    long countByUserId(String userId);

    Optional<Conversation> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT c FROM Conversation c JOIN FETCH c.messages WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findByUserIdWithMessages(@Param("userId") String userId);
}
