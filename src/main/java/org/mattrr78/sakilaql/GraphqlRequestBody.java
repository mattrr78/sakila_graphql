package org.mattrr78.sakilaql;

import java.util.HashMap;
import java.util.Map;

public class GraphqlRequestBody {
    private String query;
    private String operationName;
    private Map<String, Object> variables = new HashMap<>();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        if (variables != null) {
            this.variables = variables;
        }
    }
}
