package org.mattrr78.sakilaql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface PaymentJpaRepo extends JpaRepository<Payment, Long>  {

    @Query("SELECT payment.customerId AS customerId, payment.id AS id FROM Payment payment WHERE payment.customerId IN (?1)")
    List<Map<String, Long>> findPaymentIdsByCustomerIds(List<Long> customerIds);

    @Query(value =
            "SELECT payment.amount AS amount, film.title AS film " +
            "FROM payment " +
            "LEFT JOIN rental USING(rental_id) " +
            "LEFT JOIN inventory USING(inventory_id) " +
            "LEFT JOIN film USING(film_id) " +
            "WHERE payment_id IN (?1)",
            nativeQuery = true)
    List<Map<String, Object>> findRentalHistory(List<Long> paymentIds);

}
