package com.sqlparser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.*;

/**
 * Analyzes SQL SELECT statements and extracts detailed information about their structure.
 * <p>
 * This class uses the JSqlParser library to parse SQL queries and extract various components
 * including tables, columns, conditions, joins, and other SQL clauses. It also generates
 * human-readable descriptions of what the SQL query does.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * SqlAnalyzer analyzer = new SqlAnalyzer();
 * String sql = "SELECT u.name, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id WHERE o.total &gt; 100";
 * SqlAnalysisResult result = analyzer.analyze(sql);
 * System.out.println(result.getDescription());
 * </pre>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Extracts all tables and their aliases</li>
 *   <li>Maps columns to their respective tables</li>
 *   <li>Identifies SELECT items, WHERE conditions, and JOIN conditions</li>
 *   <li>Processes GROUP BY, HAVING, ORDER BY, and LIMIT clauses</li>
 *   <li>Generates natural language descriptions of queries</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b> This class maintains internal state during analysis and automatically resets
 * before each new analysis. The same instance can be safely reused for multiple analyses.
 * </p>
 *
 * @author SQL Parser Team
 * @version 1.0.0
 * @see SqlAnalysisResult
 * @see net.sf.jsqlparser.parser.CCJSqlParserUtil
 */
public class SqlAnalyzer {

    /** Set of table names with their aliases (if any) found in the query. */
    private Set<String> tables = new LinkedHashSet<>();

    /** Map of table names to their columns referenced in the query. */
    private Map<String, Set<String>> tableColumns = new LinkedHashMap<>();

    /** List of columns or expressions selected in the SELECT clause. */
    private List<String> selectedColumns = new ArrayList<>();

    /** List of conditions found in the WHERE clause. */
    private List<String> whereConditions = new ArrayList<>();

    /** List of JOIN conditions as they appear in the query. */
    private List<String> joinConditions = new ArrayList<>();

    /** List of columns or expressions in the GROUP BY clause. */
    private List<String> groupByColumns = new ArrayList<>();

    /** List of columns or expressions in the ORDER BY clause. */
    private List<String> orderByColumns = new ArrayList<>();

    /** Condition from the HAVING clause, if present. */
    private String havingCondition = null;

    /** Limit value from the LIMIT clause, if present. */
    private Integer limitValue = null;

    /**
     * Analyzes a SQL SELECT statement and extracts its components.
     * <p>
     * This method parses the provided SQL string, extracts all relevant components
     * (tables, columns, conditions, etc.), generates a natural language description,
     * and returns an immutable result object containing all the analysis data.
     * </p>
     *
     * <p>
     * The internal state is automatically reset before each analysis, so the same
     * analyzer instance can be reused for multiple queries.
     * </p>
     *
     * @param sql the SQL SELECT statement to analyze (must be a valid SELECT query)
     * @return a {@link SqlAnalysisResult} object containing all extracted information
     * @throws JSQLParserException if the SQL cannot be parsed (invalid syntax)
     * @throws IllegalArgumentException if the statement is not a SELECT statement
     * @see SqlAnalysisResult
     */
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

    /**
     * Resets all internal state to prepare for a new analysis.
     * <p>
     * Clears all collections and nullifies optional fields. This method is
     * automatically called at the beginning of each {@link #analyze(String)} call.
     * </p>
     */
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

    /**
     * Processes a SELECT statement and extracts all its components.
     * <p>
     * This method traverses the SQL Abstract Syntax Tree (AST) and extracts:
     * FROM items, JOINs, SELECT items, WHERE conditions, GROUP BY, HAVING,
     * ORDER BY, and LIMIT clauses.
     * </p>
     *
     * @param select the parsed SELECT statement to process
     */
    private void processSelect(Select select) {
        PlainSelect plainSelect = select.getPlainSelect();

        // Process FROM clause
        if (plainSelect.getFromItem() != null) {
            processFromItem(plainSelect.getFromItem());
        }

        // Process JOINs
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                processFromItem(join.getRightItem());
                if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
                    joinConditions.add(join.toString());
                }
            }
        }

        // Process SELECT items
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> selectItem : plainSelect.getSelectItems()) {
                processSelectItem(selectItem);
            }
        }

        // Process WHERE clause
        if (plainSelect.getWhere() != null) {
            processWhereExpression(plainSelect.getWhere());
        }

        // Process GROUP BY
        if (plainSelect.getGroupBy() != null && plainSelect.getGroupBy().getGroupByExpressionList() != null) {
            ExpressionList<?> groupByList = plainSelect.getGroupBy().getGroupByExpressionList();
            for (int i = 0; i < groupByList.size(); i++) {
                Expression expr = groupByList.get(i);
                groupByColumns.add(expr.toString());
                if (expr instanceof Column) {
                    addTableColumn((Column) expr);
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

    /**
     * Processes a FROM item (table or subquery) and extracts table information.
     *
     * @param fromItem the FROM item to process (can be a Table or subquery)
     */
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

    /**
     * Processes a SELECT item and extracts column information.
     *
     * @param selectItem the SELECT item to process (column, expression, or wildcard)
     */
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

    /**
     * Recursively extracts column references from an expression.
     * <p>
     * This method traverses expression trees (functions, binary expressions, etc.)
     * to find all column references and add them to the table-column mapping.
     * </p>
     *
     * @param expr the expression to analyze
     */
    private void extractColumnsFromExpression(Expression expr) {
        if (expr instanceof Column) {
            addTableColumn((Column) expr);
        } else if (expr instanceof Function) {
            Function func = (Function) expr;
            if (func.getParameters() != null) {
                ExpressionList<?> params = func.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    extractColumnsFromExpression(params.get(i));
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

    /**
     * Processes a WHERE clause expression and extracts condition information.
     *
     * @param expr the WHERE expression to process
     */
    private void processWhereExpression(Expression expr) {
        whereConditions.add(expr.toString());
        extractColumnsFromExpression(expr);
    }

    /**
     * Adds a column to the table-column mapping.
     *
     * @param column the column to add
     */
    private void addTableColumn(Column column) {
        String tableName = column.getTable() != null ? column.getTable().getName() : "unknown";
        String columnName = column.getColumnName();

        tableColumns.putIfAbsent(tableName, new LinkedHashSet<>());
        tableColumns.get(tableName).add(columnName);
    }

    /**
     * Generates a human-readable English description of the SQL query.
     * <p>
     * The description summarizes what the query does, including what columns
     * are selected, from which tables, with what conditions, joins, grouping,
     * ordering, and limits.
     * </p>
     *
     * @return a natural language description of the query
     */
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
