# Suggested commands (shopping-cart)

- Run tests:
  - `mvn test`
- Full build:
  - `mvn clean test`
  - `mvn clean package`

Notes:
- Project is a WAR; sources in `src/`.
- If adding tests, ensure Maven Surefire sees them (typically `src/test/java`), and add JUnit/Mockito deps to `pom.xml` if absent.
