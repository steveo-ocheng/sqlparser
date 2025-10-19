package com.sqlparser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.*;

public class SqlAnalyzer {

    private Set<String> tables = new LinkedHashSet<>();
    private Map<String, Set<String>> tableColumns = new LinkedHashMap<>();
    private List<String> selectedColumns = new ArrayList<>();
    private List<String> whereConditions = new ArrayList<>();
    private List<String> joinConditions = new ArrayList<>();
    private List<String> groupByColumns = new ArrayList<>();
    private List<String> orderByColumns = new ArrayList<>();
    private String havingCondition = null;
    private Integer limitValue = null;

    public SqlAnalysisResult analyze(String sql) throws JSQLParserException {
        reset();

        Statement statement = CCJSqlParserUtil.parse(sql);

        if (statement instanceof Select) {
            Select selectStatement = (Select) statement;
            processSelect(selectStatement);
        } else {
            throw new IllegalArgumentException("Only SELECT statements are supported");
        }

        return new SqlAnalysisResult(
            new ArrayList<>(tables),
            tableColumns,
            selectedColumns,
            whereConditions,
            joinConditions,
            groupByColumns,
            orderByColumns,
            havingCondition,
            limitValue,
            generateDescription()
        );
    }

    private void reset() {
        tables.clear();
        tableColumns.clear();
        selectedColumns.clear();
        whereConditions.clear();
        joinConditions.clear();
        groupByColumns.clear();
        orderByColumns.clear();
        havingCondition = null;
        limitValue = null;
    }

    private void processSelect(Select select) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        // Process FROM clause
        if (plainSelect.getFromItem() != null) {
            processFromItem(plainSelect.getFromItem());
        }

        // Process JOINs
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                processFromItem(join.getRightItem());
                if (join.getOnExpression() != null) {
                    joinConditions.add(join.toString());
                }
            }
        }

        // Process SELECT items
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem selectItem : plainSelect.getSelectItems()) {
                processSelectItem(selectItem);
            }
        }

        // Process WHERE clause
        if (plainSelect.getWhere() != null) {
            processWhereExpression(plainSelect.getWhere());
        }

        // Process GROUP BY
        if (plainSelect.getGroupBy() != null) {
            List<?> groupByExprs = plainSelect.getGroupBy().getGroupByExpressions();
            if (groupByExprs != null) {
                for (Object obj : groupByExprs) {
                    if (obj instanceof Expression) {
                        Expression expr = (Expression) obj;
                        groupByColumns.add(expr.toString());
                        if (expr instanceof Column) {
                            addTableColumn((Column) expr);
                        }
                    }
                }
            }
        }

        // Process HAVING
        if (plainSelect.getHaving() != null) {
            havingCondition = plainSelect.getHaving().toString();
        }

        // Process ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement orderBy : plainSelect.getOrderByElements()) {
                orderByColumns.add(orderBy.toString());
                if (orderBy.getExpression() instanceof Column) {
                    addTableColumn((Column) orderBy.getExpression());
                }
            }
        }

        // Process LIMIT
        if (plainSelect.getLimit() != null && plainSelect.getLimit().getRowCount() != null) {
            limitValue = Integer.parseInt(plainSelect.getLimit().getRowCount().toString());
        }
    }

    private void processFromItem(FromItem fromItem) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getName();
            if (table.getAlias() != null) {
                tableName = table.getName() + " (alias: " + table.getAlias().getName() + ")";
            }
            tables.add(tableName);
            tableColumns.putIfAbsent(table.getName(), new LinkedHashSet<>());
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect subSelect = (ParenthesedSelect) fromItem;
            processSelect(subSelect.getSelect());
        }
    }

    private void processSelectItem(SelectItem<?> selectItem) {
        if (selectItem instanceof SelectItem) {
            Expression expr = selectItem.getExpression();
            if (expr != null) {
                String columnStr = expr.toString();
                if (selectItem.getAlias() != null) {
                    columnStr += " AS " + selectItem.getAlias().getName();
                }
                selectedColumns.add(columnStr);
                extractColumnsFromExpression(expr);
            } else {
                // Handle special cases like * or table.*
                selectedColumns.add(selectItem.toString());
            }
        }
    }

    private void extractColumnsFromExpression(Expression expr) {
        if (expr instanceof Column) {
            addTableColumn((Column) expr);
        } else if (expr instanceof Function) {
            Function func = (Function) expr;
            if (func.getParameters() != null) {
                List<?> paramList = func.getParameters().getExpressions();
                if (paramList != null) {
                    for (Object obj : paramList) {
                        if (obj instanceof Expression) {
                            extractColumnsFromExpression((Expression) obj);
                        }
                    }
                }
            }
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression binExpr = (BinaryExpression) expr;
            extractColumnsFromExpression(binExpr.getLeftExpression());
            extractColumnsFromExpression(binExpr.getRightExpression());
        } else if (expr instanceof Parenthesis) {
            extractColumnsFromExpression(((Parenthesis) expr).getExpression());
        }
    }

    private void processWhereExpression(Expression expr) {
        whereConditions.add(expr.toString());
        extractColumnsFromExpression(expr);
    }

    private void addTableColumn(Column column) {
        String tableName = column.getTable() != null ? column.getTable().getName() : "unknown";
        String columnName = column.getColumnName();

        tableColumns.putIfAbsent(tableName, new LinkedHashSet<>());
        tableColumns.get(tableName).add(columnName);
    }

    private String generateDescription() {
        StringBuilder desc = new StringBuilder();

        desc.append("This query ");

        // Describe what is being selected
        if (selectedColumns.contains("*")) {
            desc.append("retrieves all columns");
        } else if (selectedColumns.size() == 1) {
            desc.append("retrieves the column ").append(selectedColumns.get(0));
        } else {
            desc.append("retrieves ").append(selectedColumns.size()).append(" columns (");
            desc.append(String.join(", ", selectedColumns.subList(0, Math.min(3, selectedColumns.size()))));
            if (selectedColumns.size() > 3) {
                desc.append(", and ").append(selectedColumns.size() - 3).append(" more");
            }
            desc.append(")");
        }

        // Describe tables
        if (tables.size() == 1) {
            desc.append(" from the table ").append(tables.iterator().next());
        } else if (tables.size() > 1) {
            desc.append(" from ").append(tables.size()).append(" tables (");
            List<String> tableList = new ArrayList<>(tables);
            desc.append(String.join(", ", tableList));
            desc.append(")");
        }

        // Describe joins
        if (!joinConditions.isEmpty()) {
            desc.append(", joining tables");
            if (joinConditions.size() == 1) {
                desc.append(" on condition: ").append(joinConditions.get(0));
            } else {
                desc.append(" using ").append(joinConditions.size()).append(" join conditions");
            }
        }

        // Describe WHERE clause
        if (!whereConditions.isEmpty()) {
            desc.append(". The results are filtered");
            if (whereConditions.size() == 1) {
                desc.append(" where ").append(whereConditions.get(0));
            } else {
                desc.append(" using ").append(whereConditions.size()).append(" conditions");
            }
        }

        // Describe GROUP BY
        if (!groupByColumns.isEmpty()) {
            desc.append(". Results are grouped by ").append(String.join(", ", groupByColumns));
        }

        // Describe HAVING
        if (havingCondition != null) {
            desc.append(", with groups filtered by ").append(havingCondition);
        }

        // Describe ORDER BY
        if (!orderByColumns.isEmpty()) {
            desc.append(". Results are sorted by ").append(String.join(", ", orderByColumns));
        }

        // Describe LIMIT
        if (limitValue != null) {
            desc.append(", limited to ").append(limitValue).append(" rows");
        }

        desc.append(".");

        return desc.toString();
    }
}
