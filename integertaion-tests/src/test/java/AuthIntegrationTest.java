import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.path.xml.XmlPath.given;
import static org.hamcrest.Matchers.notNullValue;

class AuthIntegrationTest {


  @BeforeAll
  public static void setup() {
    RestAssured.baseURI = "http://localhost:4004";
  }


  @Test
  void ShouldReturnTokenAfterLogin() {
    String loginBody = """
          {
            "email": "testuser@test.com",
            "password": "password123"
          }
        """;

    Response response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(loginBody)
      .when()
      .post("/auth/login")
      .then()
      .statusCode(200)
      .body("token" , notNullValue())
      .extract().response();


    System.out.println(response.getBody().asString());

  }
  @Test
  public void shouldReturnUnauthorizedOnInvalidLogin() {
    String loginBody = """
          {
            "email": "random_email@test.com",
            "password": "psword"
          }
        """;

    Response response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(loginBody)
      .when()
      .post("/auth/login")
      .then()
      .statusCode(401).extract().response();
  }

}
