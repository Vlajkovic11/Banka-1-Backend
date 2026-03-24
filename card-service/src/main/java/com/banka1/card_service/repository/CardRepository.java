package com.banka1.card_service.repository;

import com.banka1.card_service.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted cards.
 */
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Checks whether a card number is already in use.
     *
     * @param cardNumber card number to check
     * @return {@code true} when a card with the number already exists
     */
    boolean existsByCardNumber(String cardNumber);
}
