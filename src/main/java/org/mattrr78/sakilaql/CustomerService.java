package org.mattrr78.sakilaql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerJpaRepo repo;

    @Autowired
    public CustomerService(CustomerJpaRepo repo)  {
        this.repo = repo;
    }

    public Page<Customer> findCustomers(int page)  {
        return repo.findCustomers(PageRequest.of(page, 100, Sort.by("name")));
    }

}