package com.sqlparser;

import net.sf.jsqlparser.JSQLParserException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command-line interface for the SQL Parser application.
 * <p>
 * This class provides the main entry point for running SQL analysis from the command line.
 * It supports reading SQL files, parsing them, and displaying analysis results in various
 * output formats.
 * </p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * java -jar sqlparser-all-1.0.0.jar &lt;file.sql&gt; [options]
 * </pre>
 *
 * <h2>Options:</h2>
 * <ul>
 *   <li><code>--help, -h</code> - Show help message</li>
 *   <li><code>--version, -v</code> - Show version information</li>
 *   <li><code>--examples</code> - Run built-in SQL examples</li>
 *   <li><code>--verbose</code> - Show SQL query and full analysis</li>
 *   <li><code>--quiet, -q</code> - Only show English description</li>
 * </ul>
 *
 * <h2>Examples:</h2>
 * <pre>
 * java -jar sqlparser-all-1.0.0.jar query.sql
 * java -jar sqlparser-all-1.0.0.jar query.sql --verbose
 * java -jar sqlparser-all-1.0.0.jar --examples
 * </pre>
 *
 * @author SQL Parser Team
 * @version 1.0.0
 * @see SqlAnalyzer
 * @see SqlAnalysisResult
 */
public class SqlParserCli {

    /**
     * Main entry point for the CLI application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        if (command.equals("--help") || command.equals("-h")) {
            printUsage();
            System.exit(0);
        }

        if (command.equals("--version") || command.equals("-v")) {
            System.out.println("SQL Parser v1.0.0");
            System.exit(0);
        }

        if (command.equals("--examples")) {
            runExamples();
            System.exit(0);
        }

        // Parse file
        String filePath = args[0];
        boolean verbose = false;
        boolean quiet = false;

        // Check for optional flags
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--verbose")) {
                verbose = true;
            } else if (args[i].equals("--quiet") || args[i].equals("-q")) {
                quiet = true;
            }
        }

        try {
            parseFile(filePath, verbose, quiet);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Parses a SQL file and displays the analysis results.
     *
     * @param filePath path to the SQL file to analyze
     * @param verbose if true, shows the SQL query along with full analysis
     * @param quiet if true, only shows the English description
     * @throws IOException if the file cannot be read
     * @throws JSQLParserException if the SQL cannot be parsed
     */
    private static void parseFile(String filePath, boolean verbose, boolean quiet) throws IOException, JSQLParserException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException("Not a regular file: " + filePath);
        }

        String sql = Files.readString(path).trim();

        if (sql.isEmpty()) {
            throw new IllegalArgumentException("File is empty: " + filePath);
        }

        if (!quiet) {
            System.out.println("Analyzing SQL from file: " + filePath);
            System.out.println("=".repeat(80));
            System.out.println();
        }

        SqlAnalyzer analyzer = new SqlAnalyzer();
        SqlAnalysisResult result = analyzer.analyze(sql);

        if (verbose) {
            System.out.println("SQL QUERY:");
            System.out.println(sql);
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println();
        }

        if (quiet) {
            // Only print description in quiet mode
            System.out.println(result.getDescription());
        } else {
            // Print full formatted output
            System.out.println(result.toFormattedString());
        }
    }

    /**
     * Runs a series of built-in SQL examples to demonstrate the parser's capabilities.
     * <p>
     * Examples include simple SELECT, JOINs, aggregation, multiple JOINs, and subqueries.
     * </p>
     */
    private static void runExamples() {
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
    }

    /**
     * Analyzes a SQL query and prints the results with a title.
     *
     * @param analyzer the SqlAnalyzer instance to use
     * @param title descriptive title for the example
     * @param sql the SQL query to analyze
     */
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

    /**
     * Prints the usage information and help message to the console.
     */
    private static void printUsage() {
        System.out.println("SQL Parser - Analyze SQL SELECT statements");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -jar sqlparser-all-1.0.0.jar <file.sql> [options]");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  --help, -h        Show this help message");
        System.out.println("  --version, -v     Show version information");
        System.out.println("  --examples        Run built-in examples");
        System.out.println("  --verbose         Show SQL query and full analysis");
        System.out.println("  --quiet, -q       Only show English description");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  java -jar sqlparser-all-1.0.0.jar query.sql");
        System.out.println("  java -jar sqlparser-all-1.0.0.jar query.sql --verbose");
        System.out.println("  java -jar sqlparser-all-1.0.0.jar query.sql --quiet");
        System.out.println("  java -jar sqlparser-all-1.0.0.jar --examples");
        System.out.println();
        System.out.println("The SQL file should contain a single SELECT statement.");
    }
}
