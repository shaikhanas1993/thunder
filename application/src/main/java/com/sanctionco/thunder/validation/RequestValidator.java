package com.sanctionco.thunder.validation;

import com.sanctionco.thunder.models.User;

import javax.inject.Inject;
import javax.validation.ValidationException;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods to validate incoming HTTP requests.
 *
 * @see com.sanctionco.thunder.resources.UserResource
 * @see com.sanctionco.thunder.resources.VerificationResource
 */
public class RequestValidator {
  private static final Logger LOG = LoggerFactory.getLogger(RequestValidator.class);

  private final PropertyValidator propertyValidator;
  private final boolean passwordHeaderCheckEnabled;

  /**
   * Constructs a new {@code RequestValidator} with the given property validator.
   *
   * @param propertyValidator the validator that can validate user property maps
   * @param passwordHeaderCheckEnabled {@code true} if the validator should check that the passwords
   *                                   match; {@code false} otherwise
   */
  @Inject
  public RequestValidator(PropertyValidator propertyValidator, boolean passwordHeaderCheckEnabled) {
    this.propertyValidator = propertyValidator;
    this.passwordHeaderCheckEnabled = passwordHeaderCheckEnabled;
  }

  /**
   * Determines if the user object is valid. Checks to ensure that the user is not null,
   * the user's email is not null, the email address is valid, and that the user has a valid
   * property map.
   *
   * @param user the user to validate
   * @throws ValidationException if validation fails
   */
  public void validate(User user) {
    if (user == null) {
      LOG.warn("Attempted to post a null user.");
      throw new ValidationException("Cannot post a null user.");
    }

    if (user.getEmail() == null) {
      LOG.warn("Attempted to post a user with a null Email object.");
      throw new ValidationException("Cannot post a user without an email address.");
    }

    if (!isValidEmail(user.getEmail().getAddress())) {
      LOG.error("The new user has an invalid email address: {}", user.getEmail());
      throw new ValidationException("Invalid email address format. Please try again.");
    }

    if (!propertyValidator.isValidPropertiesMap(user.getProperties())) {
      LOG.warn("Attempted to post a user with invalid properties.");
      throw new ValidationException("Cannot post a user with invalid properties.");
    }
  }

  /**
   * Determines if the given password and email are valid. Checks to ensure both are not null
   * or empty.
   *
   * @param password the password to validate
   * @param email the email to validate
   * @param tokenAsPassword {@code true} if the password parameter should be considered as a
   *                        verification token; {@code false} otherwise
   * @throws ValidationException if validation fails
   */
  public void validate(String password, String email, boolean tokenAsPassword) {
    if (email == null || email.isEmpty()) {
      LOG.warn("Attempted to operate on a null user.");
      throw new ValidationException("Incorrect or missing email query parameter.");
    }

    if (password == null || password.isEmpty()) {
      if (tokenAsPassword) {
        LOG.warn("Attempted to verify user {} without a token.", email);
        throw new ValidationException("Incorrect or missing verification token query parameter.");
      } else if (passwordHeaderCheckEnabled) {
        LOG.warn("Attempted to operate on user {} without a password.", email);
        throw new ValidationException("Credentials are required to access this resource.");
      }
    }
  }

  /**
   * Determines if the given password, email, and User object are valid. In this method,
   * the email is not checked.
   *
   * @param password the password to validate
   * @param email the email to validate
   * @param user the user to validate
   * @throws ValidationException if validation fails
   *
   * @see RequestValidator#validate(User)
   */
  public void validate(String password, String email, User user) {
    validate(user);

    // Existing email can be null or empty, so just validate password
    if (passwordHeaderCheckEnabled && (password == null || password.isEmpty())) {
      LOG.warn("Attempted to operate on user {} without a password.", email);
      throw new ValidationException("Credentials are required to access this resource.");
    }
  }

  /**
   * Returns {@code true} if the validator is checking for the password header;
   * {@code false} otherwise.
   *
   * @return {@code true} if the password header check is enabled; {@code false} otherwise
   */
  public boolean isPasswordHeaderCheckEnabled() {
    return passwordHeaderCheckEnabled;
  }

  /**
   * Determines if the given email is valid.
   *
   * @param email the email address to validate
   * @return {@code true} if the email is valid; {@code false} otherwise
   */
  private boolean isValidEmail(String email) {
    return email != null && !email.isEmpty() && EmailValidator.getInstance().isValid(email);
  }
}
