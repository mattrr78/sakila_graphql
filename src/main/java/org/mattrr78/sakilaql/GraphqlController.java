package org.mattrr78.sakilaql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@RestController
@RequestMapping(path = "/graphql")
public class GraphqlController {

    private final CustomerService customerService;

    private final PaymentService paymentService;

    private final CurrencyConverter currencyConverter;

    private final GraphQL graphQL;

    @Autowired
    GraphqlController(CustomerService customerService, PaymentService paymentService) throws Exception {
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.currencyConverter = new CurrencyConverter();
        this.graphQL = GraphQL.newGraphQL(createSchema()).build();
    }

    @PostConstruct
    public void init() throws Exception {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(loadResourceFileContents("request.graphql"))
                .dataLoaderRegistry(createDataLoaderRegistry())
                .build();

        Map<String, Object> result = graphQL.execute(executionInput).toSpecification();
        System.out.println("Customers requested");
    }

    private GraphQLSchema createSchema() throws Exception {
        String schema = loadResourceFileContents("schema.graphqls");

        return new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(schema), createRuntimeWiring());
    }

    private RuntimeWiring createRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("findCustomers", environment -> {
                            int page = 0;
                            if (environment.containsArgument("page"))  {
                                page = environment.getArgument("page");
                            }
                            List<Customer> customers = customerService.findCustomers(page).getContent();
                            if (!customers.isEmpty() && environment.getSelectionSet().contains("rentalHistory")) {
                                List<Long> customerIds = customers.stream().map(Customer::getId).collect(Collectors.toList());
                                Map<Long, List<Long>> customerIdsToPaymentIdsMap = paymentService.findCustomerIdToPaymentIdsMap(customerIds);
                                GraphQLContext context = environment.getContext();
                                context.put("customerIdsToPaymentIdsMap", customerIdsToPaymentIdsMap);
                            }
                            return customers;
                        })
                )
                .type(newTypeWiring("Customer")
                        .dataFetcher("rentalHistory", environment -> {
                            Customer customer = environment.getSource();
                            GraphQLContext context = environment.getContext();
                            Map<Long, List<Long>> customerIdsToPaymentIdsMap = context.get("customerIdsToPaymentIdsMap");
                            List<Long> paymentIds = customerIdsToPaymentIdsMap.get(customer.getId());
                            DataLoader<Long, Object> dataLoader = environment.getDataLoader("rentalHistory");
                            return dataLoader.loadMany(paymentIds);
                        })
                )
                .type(newTypeWiring("RentalHistoryEntry")
                        .dataFetcher("amount", environment -> {
                            Map<String, Object> source = environment.getSource();
                            BigDecimal amount = (BigDecimal)source.get("amount");
                            Currency currency = Currency.valueOf(environment.getArgument("currency"));
                            return currencyConverter.convertFromUSD(amount, currency);
                        }))
                .build();
    }

    private DataLoaderRegistry createDataLoaderRegistry()  {
        BatchLoader<Long, Map<String, Object>> rentalHistoryBatchLoader =
                paymentIds -> CompletableFuture.supplyAsync(() -> {
                    // Return list size must match input list size
                    return paymentService.findRentalHistory(paymentIds);
                });
        DataLoader<Long, Map<String, Object>> rentalHistoryDataLoader = DataLoader.newDataLoader(rentalHistoryBatchLoader);

        return new DataLoaderRegistry().register("rentalHistory", rentalHistoryDataLoader);
    }

    private String loadResourceFileContents(String fileName) throws Exception {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> execute(@RequestBody GraphqlRequestBody requestBody)  {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .operationName(requestBody.getOperationName())
                .query(requestBody.getQuery())
                .variables(requestBody.getVariables() != null ? requestBody.getVariables() : Collections.emptyMap())
                .dataLoaderRegistry(createDataLoaderRegistry())
                .build();
        return ResponseEntity.ok(graphQL.execute(executionInput).toSpecification());
    }

}