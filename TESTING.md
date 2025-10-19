# Testing Guide

## Running Tests

### Unit Tests

The project includes comprehensive unit tests for all core functionality.

```bash
# Run all tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# View test report
open build/reports/tests/test/index.html
```

### Test Coverage

**Test Suite Summary:**
- **Total Tests:** 46
- **SqlAnalyzerTest:** 29 tests
- **SqlAnalysisResultTest:** 17 tests
- **Success Rate:** 100%

### Test Categories

#### SqlAnalyzerTest (`src/test/java/com/sqlparser/SqlAnalyzerTest.java`)

**Basic Query Tests:**
- Simple SELECT queries (single/multiple columns, wildcard)
- WHERE clauses (single and multiple conditions)
- ORDER BY (ASC/DESC)
- LIMIT clauses

**Advanced Query Tests:**
- JOINs (INNER, LEFT, RIGHT, multiple joins)
- GROUP BY (single/multiple columns)
- HAVING clauses
- Aggregate functions (COUNT, SUM, AVG, MIN, MAX)
- Subqueries in WHERE clause
- Column and table aliases
- Qualified column names
- Complex expressions in SELECT

**Integration Tests:**
- Complex queries with all clauses combined
- Table-column mapping extraction
- Natural language description generation

**Error Handling Tests:**
- Invalid SQL syntax
- Non-SELECT statements
- Empty result handling

**State Management Tests:**
- Analyzer state reset between analyses
- Multiple sequential analyses

#### SqlAnalysisResultTest (`src/test/java/com/sqlparser/SqlAnalysisResultTest.java`)

**Result Object Tests:**
- Object creation and initialization
- Getter methods for all fields
- Empty list/null handling
- Data structure integrity

**Formatting Tests:**
- Complete formatted output
- Minimal output (without optional clauses)
- Individual section formatting
- Empty section handling

**Data Integrity Tests:**
- Order preservation (columns, tables)
- Multiple condition handling
- Complex data structure mapping

## Example SQL Files Testing

### Running Individual Examples

```bash
# Basic examples
java -jar build/libs/sqlparser-all-1.0.0.jar examples/simple-query.sql
java -jar build/libs/sqlparser-all-1.0.0.jar examples/join-query.sql
java -jar build/libs/sqlparser-all-1.0.0.jar examples/aggregation-query.sql
java -jar build/libs/sqlparser-all-1.0.0.jar examples/multi-join-query.sql
java -jar build/libs/sqlparser-all-1.0.0.jar examples/subquery.sql

# Advanced CTE examples
java -jar build/libs/sqlparser-all-1.0.0.jar examples/complex-cte-query.sql
java -jar build/libs/sqlparser-all-1.0.0.jar examples/recursive-cte-query.sql
java -jar build/libs/sqlparser-all-1.0.0.jar examples/multiple-cte-analytics.sql
```

### Testing All Examples at Once

```bash
# Test all SQL files in quiet mode
for file in examples/*.sql; do
    echo "Testing: $file"
    java -jar build/libs/sqlparser-all-1.0.0.jar "$file" --quiet
    echo ""
done
```

## Manual Testing Checklist

When making changes to the parser, verify:

- [ ] Simple SELECT queries parse correctly
- [ ] WHERE conditions are extracted properly
- [ ] JOIN conditions are identified
- [ ] Aggregate functions are recognized
- [ ] Window functions are handled
- [ ] CTEs are processed correctly
- [ ] Subqueries are detected
- [ ] Column-to-table mapping is accurate
- [ ] Natural language descriptions are generated
- [ ] Error messages are helpful
- [ ] All example SQL files execute without errors

## Continuous Testing

### Pre-commit Checks

Before committing changes:

```bash
# 1. Run all unit tests
./gradlew test

# 2. Build the JAR
./gradlew build

# 3. Test with example files
java -jar build/libs/sqlparser-all-1.0.0.jar examples/complex-cte-query.sql --quiet
```

### Adding New Tests

When adding new functionality:

1. **Write unit tests first** in `src/test/java/com/sqlparser/`
2. **Add example SQL files** in `examples/` directory
3. **Update documentation** in README.md and relevant docs
4. **Run full test suite** to ensure no regressions

Example test structure:

```java
@Test
@DisplayName("Should parse [feature description]")
void testFeatureName() throws JSQLParserException {
    String sql = "SELECT ... your test query ...";
    SqlAnalysisResult result = analyzer.analyze(sql);

    // Assertions
    assertEquals(expected, result.getSomething());
    assertTrue(result.getSomethingElse().contains("value"));
}
```

## Performance Testing

For large/complex queries:

```bash
# Time the analysis
time java -jar build/libs/sqlparser-all-1.0.0.jar examples/complex-cte-query.sql --quiet
```

Expected performance:
- Simple queries: < 100ms
- Complex queries: < 500ms
- CTE queries: < 1s

## Debugging Failed Tests

### View detailed test output:

```bash
./gradlew test --info
```

### View HTML test report:

```bash
# After running tests
open build/reports/tests/test/index.html
```

### Run specific test class:

```bash
./gradlew test --tests SqlAnalyzerTest
./gradlew test --tests SqlAnalysisResultTest
```

### Run specific test method:

```bash
./gradlew test --tests SqlAnalyzerTest.testComplexQuery
```

## Known Limitations

The parser currently has these known limitations:

1. **JOIN column extraction**: Columns referenced only in JOIN ON clauses are not added to the table-column mapping (only SELECT, WHERE, GROUP BY, ORDER BY columns are tracked)

2. **CTE analysis**: CTEs are recognized and treated as tables in the final query, but the internal CTE definitions are not separately analyzed

3. **Subquery depth**: Very deeply nested subqueries may impact performance

4. **Non-SELECT statements**: Only SELECT statements are supported (INSERT, UPDATE, DELETE will throw exceptions)

These are documented in tests with explanatory comments where applicable.
