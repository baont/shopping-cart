# shopping-cart project overview

## Purpose
Java (Servlet/JSP) e-commerce shopping cart webapp. Uses JDBC to talk to MySQL, supports user/admin flows, cart, orders, email notifications.

## Tech stack
- Java 8 (source/target 1.8)
- Maven (packaging: war)
- Servlet API 3.1
- JDBC + MySQL connector

## Code structure
- `src/` is Maven `sourceDirectory` (non-standard; not `src/main/java`).
- Packages under `src/com/shashi/...` including beans, service interfaces, service impl, servlets, utilities.
- Resources (e.g., `application.properties`) also live under `src/`.

## Testing
No `src/test/java` or existing unit tests discovered yet. `pom.xml` currently has no test dependencies (no JUnit/Mockito). Will need to add test deps + surefire configuration consistent with Maven.
