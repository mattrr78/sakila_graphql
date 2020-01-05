package org.mattrr78.sakilaql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerJpaRepo extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c")
    Page<Customer> findCustomers(Pageable pageable);

}