package com.sqlparser;

import java.util.*;

public class SqlAnalysisResult {
    private final List<String> tables;
    private final Map<String, Set<String>> tableColumns;
    private final List<String> selectedColumns;
    private final List<String> whereConditions;
    private final List<String> joinConditions;
    private final List<String> groupByColumns;
    private final List<String> orderByColumns;
    private final String havingCondition;
    private final Integer limitValue;
    private final String description;

    public SqlAnalysisResult(
            List<String> tables,
            Map<String, Set<String>> tableColumns,
            List<String> selectedColumns,
            List<String> whereConditions,
            List<String> joinConditions,
            List<String> groupByColumns,
            List<String> orderByColumns,
            String havingCondition,
            Integer limitValue,
            String description) {
        this.tables = tables;
        this.tableColumns = tableColumns;
        this.selectedColumns = selectedColumns;
        this.whereConditions = whereConditions;
        this.joinConditions = joinConditions;
        this.groupByColumns = groupByColumns;
        this.orderByColumns = orderByColumns;
        this.havingCondition = havingCondition;
        this.limitValue = limitValue;
        this.description = description;
    }

    public List<String> getTables() {
        return tables;
    }

    public Map<String, Set<String>> getTableColumns() {
        return tableColumns;
    }

    public List<String> getSelectedColumns() {
        return selectedColumns;
    }

    public List<String> getWhereConditions() {
        return whereConditions;
    }

    public List<String> getJoinConditions() {
        return joinConditions;
    }

    public List<String> getGroupByColumns() {
        return groupByColumns;
    }

    public List<String> getOrderByColumns() {
        return orderByColumns;
    }

    public String getHavingCondition() {
        return havingCondition;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public String getDescription() {
        return description;
    }

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== SQL ANALYSIS ===\n\n");

        sb.append("DESCRIPTION:\n");
        sb.append("  ").append(description).append("\n\n");

        sb.append("TABLES:\n");
        for (String table : tables) {
            sb.append("  - ").append(table).append("\n");
        }
        sb.append("\n");

        sb.append("TABLE-COLUMNS:\n");
        for (Map.Entry<String, Set<String>> entry : tableColumns.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String column : entry.getValue()) {
                    sb.append("    - ").append(column).append("\n");
                }
            }
        }
        sb.append("\n");

        sb.append("SELECTED COLUMNS:\n");
        for (String col : selectedColumns) {
            sb.append("  - ").append(col).append("\n");
        }
        sb.append("\n");

        if (!whereConditions.isEmpty()) {
            sb.append("WHERE CONDITIONS:\n");
            for (String condition : whereConditions) {
                sb.append("  - ").append(condition).append("\n");
            }
            sb.append("\n");
        }

        if (!joinConditions.isEmpty()) {
            sb.append("JOIN CONDITIONS:\n");
            for (String join : joinConditions) {
                sb.append("  - ").append(join).append("\n");
            }
            sb.append("\n");
        }

        if (!groupByColumns.isEmpty()) {
            sb.append("GROUP BY:\n");
            for (String col : groupByColumns) {
                sb.append("  - ").append(col).append("\n");
            }
            sb.append("\n");
        }

        if (havingCondition != null) {
            sb.append("HAVING:\n");
            sb.append("  - ").append(havingCondition).append("\n\n");
        }

        if (!orderByColumns.isEmpty()) {
            sb.append("ORDER BY:\n");
            for (String col : orderByColumns) {
                sb.append("  - ").append(col).append("\n");
            }
            sb.append("\n");
        }

        if (limitValue != null) {
            sb.append("LIMIT:\n");
            sb.append("  - ").append(limitValue).append("\n\n");
        }

        return sb.toString();
    }
}
