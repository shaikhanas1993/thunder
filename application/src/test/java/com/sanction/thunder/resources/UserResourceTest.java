package com.sanction.thunder.resources;

import com.sanction.thunder.dao.StormUsersDao;
import com.sanction.thunder.models.StormUser;
import javax.ws.rs.core.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserResourceTest {

  private final StormUsersDao usersDao = mock(StormUsersDao.class);

  private final UserResource resource = new UserResource(usersDao);

  @Test
  public void testPostNullUser() {
    Response response = resource.postUser(null);

    assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());
  }

  @Test
  public void testPostUserFailure() {
    StormUser user = mock(StormUser.class);
    when(usersDao.insert(user)).thenReturn(null);

    Response response = resource.postUser(user);

    assertEquals(Response.Status.CONFLICT, response.getStatusInfo());
  }

  @Test
  public void testPostUser() {
    StormUser user = mock(StormUser.class);
    when(usersDao.insert(user)).thenReturn(user);

    Response response = resource.postUser(user);
    StormUser result = (StormUser) response.getEntity();

    assertEquals(Response.Status.CREATED, response.getStatusInfo());
    assertEquals(user, result);
  }

  @Test
  public void testUpdateNullUser() {
    Response response = resource.updateUser(null);

    assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());
  }

  @Test
  public void testUpdateUserFailure() {
    StormUser user = mock(StormUser.class);
    when(usersDao.update(user)).thenReturn(null);

    Response response = resource.updateUser(user);

    assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());
  }

  @Test
  public void testUpdateUser() {
    StormUser user = mock(StormUser.class);
    when(usersDao.update(user)).thenReturn(user);

    Response response = resource.updateUser(user);
    StormUser result = (StormUser) response.getEntity();

    assertEquals(Response.Status.OK, response.getStatusInfo());
    assertEquals(user, result);
  }

  @Test
  public void testGetUserWithNullUsername() {
    Response response = resource.getUser(null);

    assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());
  }

  @Test
  public void testGetUserFailure() {
    when(usersDao.findByUsername("test")).thenReturn(null);

    Response response = resource.getUser("test");

    assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());
  }

  @Test
  public void testGetUser() {
    StormUser user = mock(StormUser.class);
    when(usersDao.findByUsername("test")).thenReturn(user);

    Response response = resource.getUser("test");
    StormUser result = (StormUser) response.getEntity();

    assertEquals(Response.Status.OK, response.getStatusInfo());
    assertEquals(user, result);
  }

  @Test
  public void testDeleteNullUser() {
    Response response = resource.deleteUser(null);

    assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());
  }

  @Test
  public void testDeleteUserFailure() {
    when(usersDao.delete("test")).thenReturn(null);

    Response response = resource.deleteUser("test");

    assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());
  }

  @Test
  public void testDeleteUser() {
    StormUser user = mock(StormUser.class);
    when(usersDao.delete("test")).thenReturn(user);

    Response response = resource.deleteUser("test");
    StormUser result = (StormUser) response.getEntity();

    assertEquals(Response.Status.OK, response.getStatusInfo());
    assertEquals(user, result);
  }
}