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
import graphql.schema.idl.TypeDefinitionRegistry;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@RestController
@RequestMapping(path = "/graphql")
public class GraphqlController {

    private static final String CUSTOMER_ID_TO_PAYMENT_IDS_MAP_KEY = "customerIdsToPaymentIdsMap";

    private static final String RENTAL_HISTORY_DATA_LOADER_KEY = "rentalHistoryDataLoader";

    private final ActorService actorService;

    private final CustomerService customerService;

    private final PaymentService paymentService;

    private final CurrencyConverter currencyConverter;

    private final GraphQL graphQL;

    @Autowired
    GraphqlController(ActorService actorService, CustomerService customerService, PaymentService paymentService)
            throws Exception {
        this.actorService = actorService;
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.currencyConverter = new CurrencyConverter();
        this.graphQL = GraphQL.newGraphQL(createSchema()).build();
    }

    private GraphQLSchema createSchema() throws Exception {
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(loadResourceFileContents("schema.graphqls"));
        return new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, createRuntimeWiring());
    }

    private RuntimeWiring createRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("findActors", environment -> actorService.findAll())
                        .dataFetcher("findCustomers", environment -> {
                            int page = 0;
                            if (environment.containsArgument("page")) {
                                page = environment.getArgument("page");
                            }

                            int size = 20;
                            if (environment.containsArgument("size")) {
                                size = environment.getArgument("size");
                            }

                            Page<Customer> customerPage = customerService.findCustomers(page, size);
                            Map<String, Object> customerConnectionMap = new HashMap<>();
                            customerConnectionMap.put("total", customerPage.getTotalElements());

                            List<Customer> customers = customerPage.getContent();
                            customerConnectionMap.put("content", customers);

                            if (!customers.isEmpty() && environment.getSelectionSet().contains("content/rentalHistory")) {
                                GraphQLContext context = environment.getContext();
                                context.put(CUSTOMER_ID_TO_PAYMENT_IDS_MAP_KEY, paymentService.findCustomerIdToPaymentIdsMap(customers));
                            }

                            return customerConnectionMap;
                        })

                )
                .type(newTypeWiring("Actor")
                        .dataFetcher("name", environment -> {
                            boolean useIndexedFormat = false;
                            if (environment.containsArgument("useIndexedFormat"))  {
                                useIndexedFormat = environment.getArgument("useIndexedFormat");
                            }

                            Actor actor = environment.getSource();
                            if (useIndexedFormat)  {
                                return actor.getLastName() + ", " + actor.getFirstName();
                            } else  {
                                return actor.getFirstName() + " " + actor.getLastName();
                            }
                        })
                )
                .type(newTypeWiring("Customer")
                        .dataFetcher("rentalHistory", environment -> {
                            Customer customer = environment.getSource();
                            GraphQLContext context = environment.getContext();
                            Map<Long, List<Long>> customerIdsToPaymentIdsMap = context.get(CUSTOMER_ID_TO_PAYMENT_IDS_MAP_KEY);
                            List<Long> paymentIds = customerIdsToPaymentIdsMap.get(customer.getId());
                            DataLoader<Long, Object> dataLoader = environment.getDataLoader(RENTAL_HISTORY_DATA_LOADER_KEY);
                            return dataLoader.loadMany(paymentIds);
                        })
                )
                .type(newTypeWiring("RentalHistoryEntry")
                        .dataFetcher("amount", environment -> {
                            Map<String, Object> source = environment.getSource();
                            BigDecimal amount = (BigDecimal)source.get("amount");
                            String currencyLiteral = environment.getArgument("currency");
                            Currency currency = currencyLiteral != null ? Currency.valueOf(currencyLiteral) : Currency.USD;
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

        return new DataLoaderRegistry().register(RENTAL_HISTORY_DATA_LOADER_KEY, rentalHistoryDataLoader);
    }

    private String loadResourceFileContents(String fileName) throws Exception {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> execute(@RequestBody GraphqlRequestBody requestBody)  {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .operationName(requestBody.getOperationName())
                .query(requestBody.getQuery())
                .variables(requestBody.getVariables())
                .dataLoaderRegistry(createDataLoaderRegistry())
                .build();
        return ResponseEntity.ok(graphQL.execute(executionInput).toSpecification());
    }

}