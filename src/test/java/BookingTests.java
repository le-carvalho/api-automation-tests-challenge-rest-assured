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
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

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


        LocalDate checkin = LocalDate.now();
        LocalDate checkout = checkin.plusDays(faker.number().numberBetween(1,10));

        bookingDates = new BookingDates(checkin.toString(), checkout.toString());
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

    @Test // create token
    @Order(1)
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

    @Test
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
    @Order(5)
    public void getBookingsByDate_BookingExists_returnOk(){
        request
                .when()
                .queryParam("checkin", booking.getBookingdates().getCheckin())
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));

    }

    @Test
    @Order(6)
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
    @Order(7)
    public void updateBooking_ById() throws IOException {
                //update a current booking id
        faker = new Faker();
        LocalDate checkin = LocalDate.now();
        LocalDate checkout = checkin.plusDays(faker.number().numberBetween(1,9));
        bookingDates = new BookingDates(checkin.toString(), checkout.toString());

        Booking bookUpdate = new Booking(user.getFirstName(), user.getLastName(),
                (float)faker.number().randomDouble(2, 50, 100000),
                true,bookingDates, "");
        request
                .header("Cookie", "token=".concat(token))
                //adding put method
                .when()
                        .body(bookUpdate)
                        .put("/booking/" + faker.number().digits(2))
                .then()
                //verify status code as 200
                        .assertThat().statusCode(200)
                        .and().time(lessThan(2000L));
    }

    @Test // Delete booking
    @Order(8)
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
    @Order(9)
    public void healthCheck_returnCreated(){
        request
                .when()
                .get("/ping")
                .then()
                .assertThat()
                .statusCode(201);
    }


}
