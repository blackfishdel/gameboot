/*
 *              ______                        ____              __ 
 *             / ____/___ _____ ___  ___     / __ )____  ____  / /_
 *            / / __/ __ `/ __ `__ \/ _ \   / __  / __ \/ __ \/ __/
 *           / /_/ / /_/ / / / / / /  __/  / /_/ / /_/ / /_/ / /_  
 *           \____/\__,_/_/ /_/ /_/\___/  /_____/\____/\____/\__/  
 *                                                 
 *                                 .-'\
 *                              .-'  `/\
 *                           .-'      `/\
 *                           \         `/\
 *                            \         `/\
 *                             \    _-   `/\       _.--.
 *                              \    _-   `/`-..--\     )
 *                               \    _-   `,','  /    ,')
 *                                `-_   -   ` -- ~   ,','
 *                                 `-              ,','
 *                                  \,--.    ____==-~
 *                                   \   \_-~\
 *                                    `_-~_.-'
 *                                     \-~
 *
 *
 * Copyright (C) 2015 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.gameboot.usersession;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.data.repository.UserRepository;
import com.github.mrstampy.gameboot.data.repository.UserSessionRepository;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;

/**
 * The Class UserSessionAssist provides methods to look up {@link User}s and
 * {@link UserSession}s from the database, failing with
 * {@link GameBootRuntimeException}s should the records not exist.
 */
@Component
public class UserSessionAssist {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The Constant SESSIONS_CACHE. */
  public static final String SESSIONS_CACHE = "sessions";

  /** The Constant SESSIONS_KEY. */
  public static final String SESSIONS_KEY = "ActiveSessions";

  private static final String UNCACHED_SESSION_TIMER = "UncachedSessionTimer";

  @Autowired
  private UserSessionRepository userSessionRepo;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private ActiveSessions activeSessions;

  @Autowired
  private MetricsHelper helper;

  private String sessionsKey = SESSIONS_KEY;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.timer(UNCACHED_SESSION_TIMER, UserSessionAssist.class, "uncached", "session", "timer");
  }

  /**
   * Creates the session.
   *
   * @param user
   *          the user
   * @return the user session
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public UserSession create(User user) throws GameBootRuntimeException {
    userCheck(user);

    String userName = user.getUserName();
    check(activeSessions.hasSession(userName), "Session already exists for " + userName);

    UserSession session = new UserSession();
    session.setUser(user);

    userSessionRepo.save(session);

    activeSessions.addSession(session);

    return session;
  }

  /**
   * Expected user.
   *
   * @param userName
   *          the user name
   * @return the user
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public User expectedUser(String userName) throws GameBootRuntimeException {
    userNameCheck(userName);

    User user = userRepo.findByUserName(userName);

    check(user == null, "No user for " + userName);

    return user;
  }

  /**
   * Expected session.
   *
   * @param userName
   *          the user name
   * @return the user session
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public UserSession expected(String userName) throws GameBootRuntimeException {
    userNameCheck(userName);

    String noSession = "No session for " + userName;

    check(!activeSessions.hasSession(userName), noSession);

    UserSession session = userSessionRepo.findOpenSession(userName);

    check(session == null, noSession);

    return session;
  }

  /**
   * Expected session.
   *
   * @param user
   *          the user
   * @return the user session
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public UserSession expected(User user) throws GameBootRuntimeException {
    userCheck(user);

    String userName = user.getUserName();
    check(!activeSessions.hasSession(userName), "No session for " + userName);

    return userSessionRepo.findByUserAndEndedIsNull(user);
  }

  /**
   * Expected session.
   *
   * @param id
   *          the id
   * @return the user session
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public UserSession expected(long id) throws GameBootRuntimeException {
    if (id <= 0) throw new GameBootRuntimeException("Id must be > 0: " + id);

    check(!activeSessions.hasSession(id), "No session for id " + id);

    return userSessionRepo.findOpenSession(id);
  }

  /**
   * Returns true if the session specified by the userName is active.
   *
   * @param userName
   *          the user name
   * @return true, if successful
   * @see ActiveSessions
   */
  public boolean hasSession(String userName) {
    return activeSessions.hasSession(userName);
  }

  /**
   * Returns true if the session specified by the id is active.
   *
   * @param id
   *          the id
   * @return true, if successful
   * @see ActiveSessions
   */
  public boolean hasSession(long id) {
    return activeSessions.hasSession(id);
  }

  /**
   * Logout, closing the session.
   *
   * @param userName
   *          the user name
   * @return the user
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public User logout(String userName) throws GameBootRuntimeException {
    UserSession session = expected(userName);

    closeSession(session);

    return session.getUser();
  }

  /**
   * Logout, closing the session.
   *
   * @param id
   *          the id
   * @return the user
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  public User logout(Long id) throws GameBootRuntimeException {
    UserSession session = expected(id);

    closeSession(session);

    return session.getUser();
  }

  /**
   * Returns a list of active sessions. This list is backed by cache as defined
   * by the {@link #SESSIONS_CACHE} cache region in ehcache.xml. Use
   * {@link UserSessionLookup} in preference to this method when looking up a
   * specific {@link UserSession}.
   *
   * @return the list
   */
  @Cacheable(cacheNames = SESSIONS_CACHE, key = "target.sessionsKey")
  public List<UserSession> activeSessions() {
    Context ctx = helper.startTimer(UNCACHED_SESSION_TIMER);
    try {
      return Collections.unmodifiableList(userSessionRepo.openSessions());
    } finally {
      ctx.stop();
    }
  }

  /**
   * Gets the sessions key, used as a key in the {@link #SESSIONS_CACHE} cache
   * for the {@link UserSession} list. Exposed as a property for the
   * {@link Cacheable} annotation on {@link #activeSessions()}
   *
   * @return the sessions key
   */
  public String getSessionsKey() {
    return sessionsKey;
  }

  private void closeSession(UserSession session) {
    session.setEnded(new Date());

    userSessionRepo.save(session);

    activeSessions.removeSession(session);

    log.info("User {} logged out", session.getUser().getUserName());
  }

  private void userCheck(User user) throws GameBootRuntimeException {
    check(user == null, "null user");
  }

  private void userNameCheck(String userName) throws GameBootRuntimeException {
    check(isEmpty(userName), "null username");
  }

  private void check(boolean condition, String msg) {
    if (condition) throw new GameBootRuntimeException(msg);
  }
}
