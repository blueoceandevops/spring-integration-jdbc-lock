# Spring Integration JDBC lock - transaction serialization error

**UPDATE**:
This issue was present in Spring Integration release `4.3.5` and was fixed in `4.3.6`/`5.0.0`.
See [INT-4178](https://jira.spring.io/browse/INT-4178) and [this PR](https://github.com/spring-projects/spring-integration/pull/1991).

---

This is a minimal project to demonstrate transaction serialization exception that occurs when using `JdbcLockRegistry` based `LockRegistryLeaderInitiator` on PostgreSQL. 

The project assumes there is a PostgreSQL database named `test` available on `localhost:5432` and database user `user` with password `user` according to `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql:///test
spring.datasource.username=test
spring.datasource.password=test
```

Spring Integration schema script will be executed automatically on application startup.

To demonstrate the issue, example flow from [Spring Integration Java DSL reference](https://github.com/spring-projects/spring-integration-java-dsl/wiki/spring-integration-java-dsl-reference#example-configurations) is used.
Start two instances of the application and focus on this instance that didn't acquire the lock. After some time, usually within a minute from startup, the following exception is logged to console:

```
2016-12-01 08:49:43.581 DEBUG 8916 --- [ck-leadership-0] o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [org.springframework.integration.jdbc.lock.DefaultLockRepository.acquire]: PROPAGATION_REQUIRED,ISOLATION_SERIALIZABLE,timeout_1; ''
2016-12-01 08:49:43.582 DEBUG 8916 --- [ck-leadership-0] o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@2066448911 wrapping org.postgresql.jdbc.PgConnection@7177fa6b] for JDBC transaction
2016-12-01 08:49:43.582 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.datasource.DataSourceUtils      : Changing isolation level of JDBC Connection [HikariProxyConnection@2066448911 wrapping org.postgresql.jdbc.PgConnection@7177fa6b] to 8
2016-12-01 08:49:43.582 DEBUG 8916 --- [ck-leadership-0] o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@2066448911 wrapping org.postgresql.jdbc.PgConnection@7177fa6b] to manual commit
2016-12-01 08:49:43.583 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL update
2016-12-01 08:49:43.583 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [DELETE FROM INT_LOCK WHERE REGION=? AND LOCK_KEY=? AND CREATED_DATE<?]
2016-12-01 08:49:43.583 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : SQL update affected 0 rows
2016-12-01 08:49:43.584 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL update
2016-12-01 08:49:43.594 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [UPDATE INT_LOCK SET CREATED_DATE=? WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?]
2016-12-01 08:49:43.595 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : SQL update affected 0 rows
2016-12-01 08:49:43.595 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL update
2016-12-01 08:49:43.595 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [INSERT INTO INT_LOCK (REGION, LOCK_KEY, CLIENT_ID, CREATED_DATE) VALUES (?, ?, ?, ?)]
2016-12-01 08:49:43.596 DEBUG 8916 --- [ck-leadership-0] s.j.s.SQLErrorCodeSQLExceptionTranslator : Translating SQLException with SQL state '40001', error code '0', message [ERROR: could not serialize access due to read/write dependencies among transactions
  Detail: Reason code: Canceled on identification as a pivot, during write.
  Hint: The transaction might succeed if retried.]; SQL was [INSERT INTO INT_LOCK (REGION, LOCK_KEY, CLIENT_ID, CREATED_DATE) VALUES (?, ?, ?, ?)] for task [PreparedStatementCallback]
2016-12-01 08:49:43.600 DEBUG 8916 --- [ck-leadership-0] o.s.j.d.DataSourceTransactionManager     : Initiating transaction rollback
2016-12-01 08:49:43.601 DEBUG 8916 --- [ck-leadership-0] o.s.j.d.DataSourceTransactionManager     : Rolling back JDBC transaction on Connection [HikariProxyConnection@2066448911 wrapping org.postgresql.jdbc.PgConnection@7177fa6b]
2016-12-01 08:49:43.601 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.datasource.DataSourceUtils      : Resetting isolation level of JDBC Connection [HikariProxyConnection@2066448911 wrapping org.postgresql.jdbc.PgConnection@7177fa6b] to 2
2016-12-01 08:49:43.601 DEBUG 8916 --- [ck-leadership-0] o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@2066448911 wrapping org.postgresql.jdbc.PgConnection@7177fa6b] after transaction
2016-12-01 08:49:43.602 DEBUG 8916 --- [ck-leadership-0] o.s.jdbc.datasource.DataSourceUtils      : Returning JDBC Connection to DataSource
```

The project also includes MariaDB JDBC driver and MySQL/MariaDB configuration in `application.propeties` (commented out), however the issue wasn't observed on MariaDB/MySQL.
