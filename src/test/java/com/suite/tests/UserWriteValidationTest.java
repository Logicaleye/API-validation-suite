package com.suite.tests;

import com.suite.base.BaseTest;
import com.suite.db.DbUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Write-side validation: POST / PUT / DELETE calls must actually change the database.
 * Steps run in a fixed order because each builds on the record created by the first.
 */
public class UserWriteValidationTest extends BaseTest {

    private static final String EMAIL = "autotest.user@example.com";
    private int createdId;

    private Map<String, Object> payload(String name, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", EMAIL);
        body.put("role", role);
        return body;
    }

    @Test(priority = 1, description = "POST /users returns 201 and the row exists in the DB")
    public void createUserPersistsToDb() {
        // Guard against leftovers from a previously failed run
        DbUtil.update("DELETE FROM users WHERE email = ?", EMAIL);

        Response response = given().spec(spec).body(payload("Auto Tester", "user"))
                .when().post("/users");

        Assert.assertEquals(response.statusCode(), 201);
        createdId = response.jsonPath().getInt("id");

        Map<String, Object> db = DbUtil.queryForRow("SELECT name, email, role FROM users WHERE id = ?", createdId);
        Assert.assertNotNull(db, "Created user not found in DB");
        Assert.assertEquals(db.get("name"), "Auto Tester");
        Assert.assertEquals(db.get("email"), EMAIL);
        Assert.assertEquals(db.get("role"), "user");
    }

    @Test(priority = 2, dependsOnMethods = "createUserPersistsToDb",
          description = "POST with a duplicate email returns 409 and does not add a second row")
    public void duplicateEmailIsRejected() {
        given().spec(spec).body(payload("Someone Else", "user"))
                .when().post("/users")
                .then().statusCode(409);

        Assert.assertEquals(DbUtil.queryForLong("SELECT COUNT(*) FROM users WHERE email = ?", EMAIL), 1L,
                "Duplicate row was inserted");
    }

    @Test(priority = 3, dependsOnMethods = "createUserPersistsToDb",
          description = "PUT /users/{id} updates the DB row")
    public void updateUserChangesDb() {
        given().spec(spec).body(payload("Auto Tester Updated", "manager"))
                .when().put("/users/" + createdId)
                .then().statusCode(200);

        Map<String, Object> db = DbUtil.queryForRow("SELECT name, role FROM users WHERE id = ?", createdId);
        Assert.assertEquals(db.get("name"), "Auto Tester Updated");
        Assert.assertEquals(db.get("role"), "manager");
    }

    @Test(priority = 4, dependsOnMethods = "createUserPersistsToDb",
          description = "POST without an email returns 400 and adds nothing")
    public void missingEmailReturns400() {
        long before = DbUtil.queryForLong("SELECT COUNT(*) FROM users");

        Map<String, Object> bad = new HashMap<>();
        bad.put("name", "No Email");
        given().spec(spec).body(bad).when().post("/users").then().statusCode(400);

        Assert.assertEquals(DbUtil.queryForLong("SELECT COUNT(*) FROM users"), before, "Row count changed");
    }

    @Test(priority = 5, dependsOnMethods = "createUserPersistsToDb",
          description = "DELETE /users/{id} removes the row from the DB")
    public void deleteUserRemovesFromDb() {
        given().spec(spec).when().delete("/users/" + createdId).then().statusCode(204);

        Assert.assertNull(DbUtil.queryForRow("SELECT id FROM users WHERE id = ?", createdId),
                "Row still present after DELETE");

        given().spec(spec).when().get("/users/" + createdId).then().statusCode(404);
    }

    /** Safety net so a failure mid-way never leaves test data behind. */
    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        DbUtil.update("DELETE FROM users WHERE email = ?", EMAIL);
    }
}
