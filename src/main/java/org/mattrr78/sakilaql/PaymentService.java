package org.mattrr78.sakilaql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentJpaRepo repo;

    @Autowired
    public PaymentService(PaymentJpaRepo repo)  {
        this.repo = repo;
    }

    Map<Long, List<Long>> findCustomerIdToPaymentIdsMap(List<Customer> customers)  {
        if (customers.isEmpty())  {
            return Collections.emptyMap();
        }
        List<Long> customerIds = customers.stream().map(Customer::getId).collect(Collectors.toList());
        Map<Long, List<Long>> customerIdToPaymentIdsMap = new HashMap<>();
        for (Map<String, Long> entry : repo.findPaymentIdsByCustomerIds(customerIds))  {
            Long customerId = entry.get("customerId");
            if (!customerIdToPaymentIdsMap.containsKey(customerId))  {
                customerIdToPaymentIdsMap.put(customerId, new ArrayList<>());
            }
            customerIdToPaymentIdsMap.get(customerId).add(entry.get("paymentId"));
        }
        return customerIdToPaymentIdsMap;
    }

    List<Map<String, Object>> findRentalHistory(List<Long> paymentIds)  {
        if (paymentIds.isEmpty())  {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rentalHistory = repo.findRentalHistory(paymentIds);
        return rentalHistory;
    }

}
