import Entities.Booking;
import Entities.BookingDates;
import Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class BookingTests {
    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;
    public static String token = "";

    @BeforeAll
    public static void Setup(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8,10),
                faker.phoneNumber().toString());

        bookingDates = new BookingDates("2018-01-02", "2018-01-03");
        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float)faker.number().randomDouble(2, 50, 100000),
                true,bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(),new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }

    @Test
    public void getAllBookingsId_returnOk(){
            Response response = request
                                    .when()
                                        .get("/booking")
                                    .then()
                                        .extract()
                                        .response();


        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }
    @Test // Get Booking id list
    public void getBookingById_returnOk(){
                given()
                .contentType("application/json")
                .log().all()
                .when()
                .get(baseURI + "/booking/" + faker.number().digits(2))
                .then()
                .log().all()
                .statusCode(200);
    }
    @Test
    public void  getBookingsByUserName_BookingExists_returnOk(){
                    request
                            .given()
                            .contentType(ContentType.JSON)
                            .log().all()
                        .when()
                            .get(baseURI + "/booking?" + user.getFirstName() + user.getLastName())
                        .then()
                            .log().all()
                            .assertThat()
                            .statusCode(200)
                        .and()
                        .body("results", hasSize(greaterThan(0)));

    }

    @Test
    public void  CreateBooking_WithValidData_returnOk(){

        Booking test = booking;
        given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                    .contentType(ContentType.JSON)
                        .when()
                        .body(booking)
                        .post("/booking")
                        .then()
                        .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                        .and()
                        .assertThat()
                        .statusCode(200)
                        .contentType(ContentType.JSON).and().time(lessThan(2000L));

    }

    @Test
    public void updatingBooking_ById() throws IOException {
                //update a current booking id
               request
                        .header("Cookie", "token=".concat(token))
                        .log().all()
                //adding put method
                        .body(booking)
                .when()
                        .put("/booking/" + faker.number().digits(2))
                .then()
                        .log().all()

                //verify status code as 200
                        .assertThat().statusCode(403).and().time(lessThan(2000L));
    }
    @Test
    public void getBookingsByDate_BookingExists_returnOk(){
        request
                .when()
                .queryParam("checkin", 2022-11-21)
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));

    }
    @Test // create token
    public void createAuthToken(){
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "password123");

        token = request
                .header("ContentType", "application/json")
                .when()
                .body(body)
                .post("/auth")
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test // Delete booking
    public void deleteBookingById_returnOk(){
        request
                .header("Cookie", "token=".concat(token))
                .when()
                .delete("/booking/" + faker.number().digits(2))
                .then()
                .assertThat()
                .statusCode(201);

    }

    @Test // Health check
    public void healthCheck_returnCreated(){
        request
                .when()
                .get("/ping")
                .then()
                .assertThat()
                .statusCode(201);
    }


}
