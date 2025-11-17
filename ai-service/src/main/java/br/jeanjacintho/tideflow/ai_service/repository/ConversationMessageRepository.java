package br.jeanjacintho.tideflow.ai_service.repository;

import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

    long countByConversationId(UUID conversationId);

    @Query("SELECT m FROM ConversationMessage m WHERE m.conversation.id = :conversationId ORDER BY m.sequenceNumber DESC")
    List<ConversationMessage> findLastMessageByConversationId(@Param("conversationId") UUID conversationId);
}
