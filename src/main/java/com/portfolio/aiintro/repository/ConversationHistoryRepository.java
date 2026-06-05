package com.portfolio.aiintro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistoryRepository, UUID> {
}
