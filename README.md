# REST API Automation – Data Validation Suite

Java test framework that validates a REST API against the database behind it: every response
is compared with the real MySQL rows, and every write call is checked by querying the DB afterwards.

**Stack:** Java 11+, RestAssured, TestNG, Maven, JDBC (MySQL), Extent Reports, Jenkins (optional)

## Layout
```
backend/                       Node/Express + MySQL Users API (system under test)
sql/schema.sql                 Creates apisuite_db, users table, seed data
src/test/java/com/suite/
  config/ConfigReader.java     config.properties + -D overrides
  db/DbUtil.java               JDBC helper (PreparedStatements)
  base/BaseTest.java           RestAssured request spec
  listeners/ExtentListener.java  TestNG listener -> Extent HTML report
  tests/UserReadValidationTest.java   GET checks, @DataProvider loops over IDs read from the DB
  tests/UserWriteValidationTest.java  POST / PUT / DELETE checks + negative cases
testng.xml                     Suite definition
Jenkinsfile                    Optional CI job
```

## Run it
1. **Database** – `mysql -u root -p < sql/schema.sql`
2. **API** – `cd backend && npm install && npm start` (env vars `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_NAME` if not the defaults)
3. **Config** – edit `src/test/resources/config.properties` (DB password etc.) or pass `-D` flags
4. **Tests** – `mvn clean test`
5. **Report** – open `target/extent-report/index.html`

Override on the fly: `mvn test -Dbase.uri=http://localhost:5000/api -Ddb.password=yourpass`

## What is validated
| Area | Checks |
|------|--------|
| GET /users/{id} (data-driven) | status 200; id, name, email, role equal the DB row, for every user in the table |
| GET /users | record count equals `COUNT(*)` |
| GET missing id | 404 and no DB row |
| POST /users | 201; row exists with correct values |
| POST duplicate email | 409; no second row |
| POST missing email | 400; row count unchanged |
| PUT /users/{id} | 200; DB row updated |
| DELETE /users/{id} | 204; row gone; follow-up GET is 404 |
