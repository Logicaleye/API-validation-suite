package com.suite.tests;

import com.suite.base.BaseTest;
import com.suite.db.DbUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Read-side validation: every GET response is compared with what is really stored in MySQL.
 */
public class UserReadValidationTest extends BaseTest {

    /** Data-driven: one test run per user id currently in the database. */
    @DataProvider(name = "userIds")
    public Object[][] userIds() {
        List<Map<String, Object>> rows = DbUtil.queryForList("SELECT id FROM users ORDER BY id");
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = ((Number) rows.get(i).get("id")).intValue();
        }
        return data;
    }

    @Test(dataProvider = "userIds", description = "GET /users/{id} fields match the DB row")
    public void apiUserMatchesDbRecord(int id) {
        Response response = given().spec(spec).when().get("/users/" + id);

        Assert.assertEquals(response.statusCode(), 200, "Unexpected status code");

        Map<String, Object> db = DbUtil.queryForRow(
                "SELECT id, name, email, role FROM users WHERE id = ?", id);
        Assert.assertNotNull(db, "No DB row for id " + id);

        Assert.assertEquals(response.jsonPath().getInt("id"), ((Number) db.get("id")).intValue(), "id mismatch");
        Assert.assertEquals(response.jsonPath().getString("name"), db.get("name"), "name mismatch");
        Assert.assertEquals(response.jsonPath().getString("email"), db.get("email"), "email mismatch");
        Assert.assertEquals(response.jsonPath().getString("role"), db.get("role"), "role mismatch");
    }

    @Test(description = "GET /users returns exactly as many records as the users table")
    public void apiUserCountMatchesDbCount() {
        Response response = given().spec(spec).when().get("/users");

        Assert.assertEquals(response.statusCode(), 200);
        long dbCount = DbUtil.queryForLong("SELECT COUNT(*) FROM users");
        Assert.assertEquals((long) response.jsonPath().getList("$").size(), dbCount, "Record count mismatch");
    }

    @Test(description = "GET /users/{id} for a missing id returns 404 and the DB has no such row")
    public void unknownUserReturns404() {
        int missingId = 999999;
        Assert.assertNull(DbUtil.queryForRow("SELECT id FROM users WHERE id = ?", missingId),
                "Test precondition failed: id exists in DB");

        given().spec(spec).when().get("/users/" + missingId)
                .then().statusCode(404);
    }
}
