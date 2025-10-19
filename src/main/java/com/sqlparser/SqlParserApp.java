package com.sqlparser;

import net.sf.jsqlparser.JSQLParserException;

public class SqlParserApp {

    public static void main(String[] args) {
        SqlAnalyzer analyzer = new SqlAnalyzer();

        // Example 1: Simple SELECT
        String sql1 = "SELECT id, name, email FROM users WHERE age > 18 ORDER BY name";
        analyzeSql(analyzer, "Example 1: Simple SELECT", sql1);

        // Example 2: JOIN query
        String sql2 = "SELECT u.name, o.order_date, o.total FROM users u " +
                      "INNER JOIN orders o ON u.id = o.user_id " +
                      "WHERE o.total > 100 ORDER BY o.order_date DESC";
        analyzeSql(analyzer, "Example 2: JOIN Query", sql2);

        // Example 3: Complex query with GROUP BY and HAVING
        String sql3 = "SELECT department, COUNT(*) as employee_count, AVG(salary) as avg_salary " +
                      "FROM employees " +
                      "WHERE hire_date > '2020-01-01' " +
                      "GROUP BY department " +
                      "HAVING COUNT(*) > 5 " +
                      "ORDER BY avg_salary DESC " +
                      "LIMIT 10";
        analyzeSql(analyzer, "Example 3: Aggregation Query", sql3);

        // Example 4: Multiple JOINs
        String sql4 = "SELECT c.customer_name, p.product_name, oi.quantity, oi.price " +
                      "FROM customers c " +
                      "INNER JOIN orders o ON c.customer_id = o.customer_id " +
                      "INNER JOIN order_items oi ON o.order_id = oi.order_id " +
                      "INNER JOIN products p ON oi.product_id = p.product_id " +
                      "WHERE o.order_date >= '2024-01-01' AND oi.quantity > 1";
        analyzeSql(analyzer, "Example 4: Multiple JOINs", sql4);

        // Example 5: Subquery
        String sql5 = "SELECT name, salary FROM employees " +
                      "WHERE salary > (SELECT AVG(salary) FROM employees) " +
                      "ORDER BY salary DESC";
        analyzeSql(analyzer, "Example 5: Subquery", sql5);

        // Example 6: SELECT ALL columns
        String sql6 = "SELECT * FROM products WHERE category = 'Electronics' AND price < 500";
        analyzeSql(analyzer, "Example 6: SELECT ALL", sql6);

        // Example 7: Complex expression
        String sql7 = "SELECT first_name || ' ' || last_name AS full_name, " +
                      "ROUND(salary * 1.1, 2) AS new_salary, " +
                      "EXTRACT(YEAR FROM hire_date) AS hire_year " +
                      "FROM employees " +
                      "WHERE department_id IN (10, 20, 30)";
        analyzeSql(analyzer, "Example 7: Complex Expressions", sql7);
    }

    private static void analyzeSql(SqlAnalyzer analyzer, String title, String sql) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(title);
        System.out.println("=".repeat(80));
        System.out.println("SQL: " + sql);
        System.out.println();

        try {
            SqlAnalysisResult result = analyzer.analyze(sql);
            System.out.println(result.toFormattedString());
        } catch (JSQLParserException e) {
            System.err.println("Error parsing SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error analyzing SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
