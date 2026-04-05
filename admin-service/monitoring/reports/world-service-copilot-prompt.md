# Service Doctor Copilot Prompt

Service: world-service
Display name: World Service
Phase: P2
Port: 8370
Current service status: STOPPED
Current doctor status: FIXING
Detected error type: RuntimeException
Summary: Caused by: org.hibernate.exception.SQLGrammarException: JDBC exception executing SQL [select we1_0.event_id,we1_0.active,we1_0.create_time,we1_0.cron_schedule,we1_0.description,we1_0.end_time,we1_0.event_data,we1_0.ev...

## Recent logs
```text
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:138) ~[spring-tx-6.2.8.jar!/:6.2.8]
	... 25 common frames omitted
Caused by: java.sql.SQLSyntaxErrorException: Table 'game_world.world_events' doesn't exist
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:112) ~[mysql-connector-j-9.2.0.jar!/:9.2.0]
	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:114) ~[mysql-connector-j-9.2.0.jar!/:9.2.0]
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeInternal(ClientPreparedStatement.java:990) ~[mysql-connector-j-9.2.0.jar!/:9.2.0]
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeQuery(ClientPreparedStatement.java:1058) ~[mysql-connector-j-9.2.0.jar!/:9.2.0]
	at com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52) ~[HikariCP-6.3.0.jar!/:na]
	at com.zaxxer.hikari.pool.HikariProxyPreparedStatement.executeQuery(HikariProxyPreparedStatement.java) ~[HikariCP-6.3.0.jar!/:na]
	at org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess.executeQuery(DeferredResultSetAccess.java:251) ~[hibernate-core-6.6.18.Final.jar!/:6.6.18.Final]
	... 58 common frames omitted

```

## Task
- find the root cause
- propose the smallest safe fix
- do not change unrelated files
- keep current behavior intact
- ensure the relevant Maven build passes after the fix
