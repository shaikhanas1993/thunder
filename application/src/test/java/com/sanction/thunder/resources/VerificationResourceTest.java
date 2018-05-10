package com.sanction.thunder.resources;

import com.codahale.metrics.MetricRegistry;

import com.sanction.thunder.authentication.Key;
import com.sanction.thunder.dao.DatabaseError;
import com.sanction.thunder.dao.DatabaseException;
import com.sanction.thunder.dao.UsersDao;
import com.sanction.thunder.email.EmailService;
import com.sanction.thunder.models.Email;
import com.sanction.thunder.models.ResponseType;
import com.sanction.thunder.models.User;

import java.net.URI;
import java.util.Collections;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationResourceTest {
  private static final String URL = "http://www.test.com/";
  private static final String SUCCESS_HTML = "<html>success!</html>";
  private static final String VERIFICATION_HTML = "<html>Verify</html>";
  private static final String VERIFICATION_TEXT = "Verify";

  private final EmailService emailService = mock(EmailService.class);
  private final MetricRegistry metrics = new MetricRegistry();
  private final UsersDao usersDao = mock(UsersDao.class);
  private final Key key = mock(Key.class);

  private static final UriInfo uriInfo = mock(UriInfo.class);
  private static final UriBuilder uriBuilder = mock(UriBuilder.class);

  private final User unverifiedMockUser =
      new User(new Email("test@test.com", false, "verificationToken"),
          "password", Collections.emptyMap());
  private final User verifiedMockUser =
      new User(new Email("test@test.com", true, "verificationToken"),
          "password", Collections.emptyMap());
  private final User nullDatabaseTokenMockUser =
      new User(new Email("test@test.com", false, null),
          "password", Collections.emptyMap());
  private final User mismatchedTokenMockUser =
      new User(new Email("test@test.com", false, "mismatchedToken"),
          "password", Collections.emptyMap());

  private final VerificationResource resource =
      new VerificationResource(usersDao, metrics, emailService, SUCCESS_HTML, VERIFICATION_HTML,
          VERIFICATION_TEXT);

  @BeforeAll
  static void setup() throws Exception {
    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.queryParam(anyString(), any())).thenReturn(uriBuilder);
    when(uriBuilder.build()).thenReturn(new URI(URL));

    when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
  }

  /* Verify User Tests */
  @Test
  void testCreateVerificationEmailWithNullEmail() {
    Response response = resource.createVerificationEmail(uriInfo, key, null, "password");

    assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
  }

  @Test
  void testCreateVerificationEmailWithNullPassword() {
    Response response = resource.createVerificationEmail(uriInfo, key, "test@test.com", null);

    assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
  }

  @Test
  void testCreateVerificationEmailFindUserException() {
    when(usersDao.findByEmail(anyString()))
        .thenThrow(new DatabaseException(DatabaseError.DATABASE_DOWN));

    Response response = resource.createVerificationEmail(uriInfo, key, "test@test.com", "password");

    assertEquals(response.getStatusInfo(), Response.Status.SERVICE_UNAVAILABLE);
  }

  @Test
  void testCreateVerificationEmailUpdateUserException() {
    when(usersDao.findByEmail(anyString())).thenReturn(unverifiedMockUser);
    when(usersDao.update(anyString(), any(User.class)))
        .thenThrow(new DatabaseException(DatabaseError.DATABASE_DOWN));

    Response response = resource.createVerificationEmail(uriInfo, key, "test@test.com", "password");

    assertEquals(response.getStatusInfo(), Response.Status.SERVICE_UNAVAILABLE);
  }

  @Test
  void testCreateVerificationEmailSendEmailFailure() {
    when(usersDao.findByEmail(anyString())).thenReturn(unverifiedMockUser);
    when(usersDao.update(anyString(), any(User.class))).thenReturn(unverifiedMockUser);
    when(emailService.sendEmail(any(Email.class), anyString(), anyString(), anyString()))
        .thenReturn(false);

    Response response = resource.createVerificationEmail(uriInfo, key, "test@test.com", "password");

    assertEquals(response.getStatusInfo(), Response.Status.INTERNAL_SERVER_ERROR);
  }

  @Test
  void testCreateVerificationEmailSuccess() {
    when(usersDao.findByEmail(anyString())).thenReturn(unverifiedMockUser);
    when(usersDao.update(anyString(), any(User.class))).thenReturn(unverifiedMockUser);
    when(emailService.sendEmail(any(Email.class), anyString(), anyString(), anyString()))
        .thenReturn(true);

    Response response = resource.createVerificationEmail(uriInfo, key, "test@test.com", "password");
    User result = (User) response.getEntity();

    assertAll("Assert successful send email",
        () -> assertEquals(response.getStatusInfo(), Response.Status.OK),
        () -> assertEquals(unverifiedMockUser, result));

    // Verify that the correct HTML and Text were used to send the email
    verify(emailService).sendEmail(
        any(Email.class), eq("Account Verification"), eq(VERIFICATION_HTML), eq(VERIFICATION_TEXT));
  }

  @Test
  void testCreateVerificationEmailCorrectUrl() {
    when(usersDao.findByEmail(anyString())).thenReturn(unverifiedMockUser);
    when(usersDao.update(anyString(), any(User.class))).thenReturn(unverifiedMockUser);
    when(emailService.sendEmail(any(Email.class), anyString(), anyString(), anyString()))
        .thenReturn(true);

    String verificationHtml = "<html>Verify CODEGEN-URL</html>";
    String verificationText = "Verify CODEGEN-URL";
    VerificationResource resource = new VerificationResource(
        usersDao, metrics, emailService, SUCCESS_HTML, verificationHtml, verificationText);

    Response response = resource.createVerificationEmail(uriInfo, key, "test@test.com", "password");
    User result = (User) response.getEntity();

    assertAll("Assert successful send email with inserted URL",
        () -> assertEquals(response.getStatusInfo(), Response.Status.OK),
        () -> assertEquals(unverifiedMockUser, result));

    // Verify that the correct HTML and Text were used to send the email
    String expectedVerificationHtml = "<html>Verify " + URL + "</html>";
    String expectedVerificationText = "Verify " + URL;
    verify(emailService).sendEmail(
        any(Email.class), eq("Account Verification"),
        eq(expectedVerificationHtml), eq(expectedVerificationText));
  }

  /* Verify Email Tests */
  @Test
  void testVerifyEmailWithNullEmail() {
    Response response = resource.verifyEmail(null, "verificationToken", ResponseType.JSON);

    assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
  }

  @Test
  void testVerifyEmailWithNullToken() {
    Response response = resource.verifyEmail("test@test.com", null, ResponseType.JSON);

    assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
  }

  @Test
  void testVerifyEmailFindUserException() {
    when(usersDao.findByEmail(anyString()))
        .thenThrow(new DatabaseException(DatabaseError.DATABASE_DOWN));

    Response response = resource.verifyEmail("test@test.com", "verificationToken",
        ResponseType.JSON);

    assertEquals(response.getStatusInfo(), Response.Status.SERVICE_UNAVAILABLE);
  }

  @Test
  void testVerifyEmailWithNullDatabaseToken() {
    when(usersDao.findByEmail(anyString())).thenReturn(nullDatabaseTokenMockUser);

    Response response = resource.verifyEmail("test@test.com", "verificationToken",
        ResponseType.JSON);

    assertEquals(response.getStatusInfo(), Response.Status.INTERNAL_SERVER_ERROR);
  }

  @Test
  void testVerifyEmailWithMismatchedToken() {
    when(usersDao.findByEmail(anyString())).thenReturn(mismatchedTokenMockUser);

    Response response = resource.verifyEmail("test@test.com", "verificationToken",
        ResponseType.JSON);

    assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
  }

  @Test
  void testVerifyEmailUpdateUserException() {
    when(usersDao.findByEmail("test@test.com")).thenReturn(unverifiedMockUser);
    when(usersDao.update(unverifiedMockUser.getEmail().getAddress(), verifiedMockUser))
        .thenThrow(new DatabaseException(DatabaseError.DATABASE_DOWN));

    Response response = resource.verifyEmail("test@test.com", "verificationToken",
        ResponseType.JSON);

    assertEquals(response.getStatusInfo(), Response.Status.SERVICE_UNAVAILABLE);
  }

  @Test
  void testVerifyEmailSuccess() {
    when(usersDao.findByEmail("test@test.com")).thenReturn(unverifiedMockUser);
    when(usersDao.update(unverifiedMockUser.getEmail().getAddress(), verifiedMockUser))
        .thenReturn(verifiedMockUser);

    Response response = resource.verifyEmail("test@test.com", "verificationToken",
        ResponseType.JSON);
    User result = (User) response.getEntity();

    assertAll("Assert successful verify email with JSON response",
        () -> assertEquals(response.getStatusInfo(), Response.Status.OK),
        () -> assertEquals(verifiedMockUser, result));
  }

  @Test
  void testVerifyEmailWithHtmlResponse() {
    when(usersDao.findByEmail("test@test.com")).thenReturn(unverifiedMockUser);
    when(usersDao.update(unverifiedMockUser.getEmail().getAddress(), verifiedMockUser))
        .thenReturn(verifiedMockUser);

    Response response = resource.verifyEmail("test@test.com", "verificationToken",
        ResponseType.HTML);
    URI result = response.getLocation();

    assertAll("Assert successful verify email with HTML response",
        () -> assertEquals(response.getStatusInfo(), Response.Status.SEE_OTHER),
        () -> assertEquals(UriBuilder.fromUri("/verify/success").build(), result));
  }

  /* HTML Success Tests */
  @Test
  void testGetSuccessHtml() {
    Response response = resource.getSuccessHtml();
    String result = (String) response.getEntity();

    assertAll("Assert successful HTML response",
        () -> assertEquals(Response.Status.OK, response.getStatusInfo()),
        () -> assertEquals(SUCCESS_HTML, result));
  }
}
