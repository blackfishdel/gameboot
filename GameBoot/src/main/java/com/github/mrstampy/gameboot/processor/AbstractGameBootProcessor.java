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
 *                       http://mrstampy.github.io/gameboot/
 *
 * Copyright (C) 2015, 2016 Burton Alexander
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
package com.github.mrstampy.gameboot.processor;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.locale.processor.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextCodes;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;

/**
 * Abstract superclass for {@link GameBootProcessor}s.
 *
 * @param <M>
 *          the generic type
 */
public abstract class AbstractGameBootProcessor<M extends AbstractGameBootMessage>
    implements GameBootProcessor<M>, ResponseContextCodes {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ResponseContextLookup lookup;

  @Autowired
  private LocaleRegistry localeRegistry;

  /**
   * Sets the error lookup.
   *
   * @param lookup
   *          the new error lookup
   */
  @Autowired
  public void setErrorLookup(ResponseContextLookup lookup) {
    this.lookup = lookup;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.GameBootProcessor#process(com.github
   * .mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  public Response process(M message) throws Exception {
    if (message == null) fail(getResponseContext(NO_MESSAGE), "Null message");

    String type = message.getType();
    Integer id = message.getId();

    log.debug("Processing message type {}, id {}", type, id);

    try {
      validate(message);

      Response response = processImpl(message);
      response.setId(id);
      if (Response.TYPE.equals(response.getType())) response.setType(type);

      log.debug("Created response, code {} for message type {}, id {}", response.getResponseCode(), type, id);

      return response;
    } catch (GameBootRuntimeException | GameBootException e) {
      return gameBootErrorResponse(message, e);
    } catch (Exception e) {
      log.error("Error in processing {}, id {}", type, id, e);

      Response r = failure(getResponseContext(UNEXPECTED_ERROR, message.getSystemId()),
          message,
          "An unexpected error has occurred");
      r.setId(id);

      return r;
    }
  }

  /**
   * Game boot error response.
   *
   * @param message
   *          the message
   * @param e
   *          the e
   * @return the response
   */
  protected Response gameBootErrorResponse(M message, GameBootThrowable e) {
    ResponseContext error = getError(message.getSystemId(), e);

    log.error("Error in processing {} : {}, {}", message.getType(), error, e.getMessage());

    Object[] payload = e.getError() == null ? null : e.getPayload();

    Response r = new Response(message, ResponseCode.FAILURE, error, payload);
    r.setId(message.getId());
    r.setContext(error);

    return r;
  }

  private ResponseContext getError(SystemIdKey systemId, GameBootThrowable e) {
    if (e.getError() != null) return e.getError();
    if (e.getErrorCode() == null) return null;

    return lookup.lookup(e.getErrorCode(), localeRegistry.get(systemId), e.getPayload());
  }

  /**
   * Gets the response context.
   *
   * @param code
   *          the code
   * @param parameters
   *          the parameters
   * @return the response context
   */
  protected ResponseContext getResponseContext(Integer code, Object... parameters) {
    return getResponseContext(code, null, parameters);
  }

  /**
   * Gets the response context.
   *
   * @param code
   *          the code
   * @param systemId
   *          the system id
   * @param parameters
   *          the parameters
   * @return the response context
   */
  protected ResponseContext getResponseContext(Integer code, SystemIdKey systemId, Object... parameters) {
    Locale locale = systemId == null ? Locale.getDefault() : localeRegistry.get(systemId);
    return lookup.lookup(code, locale, parameters);
  }

  /**
   * Fail, throwing a {@link GameBootRuntimeException} with the specified
   * message. The exception is caught by the
   * {@link #process(AbstractGameBootMessage)} implementation which after
   * logging returns a failure response.
   *
   * @param rc
   *          the rc
   * @param message
   *          the message
   * @param payload
   *          the payload
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  protected void fail(ResponseContext rc, String message, Object... payload) throws GameBootRuntimeException {
    throw new GameBootRuntimeException(message, rc, payload);
  }

  /**
   * Returns an initialized success {@link Response}.
   *
   * @param message
   *          the message
   * @param response
   *          the response
   * @return the response
   */
  protected Response success(M message, Object... response) {
    return new Response(message, ResponseCode.SUCCESS, response);
  }

  /**
   * Returns an initialized failure {@link Response}.
   *
   * @param rc
   *          the rc
   * @param message
   *          the message
   * @param response
   *          the response
   * @return the response
   */
  protected Response failure(ResponseContext rc, M message, Object... response) {
    return new Response(message, ResponseCode.FAILURE, rc, response);
  }

  /**
   * Implement to perform any pre-processing validation.
   *
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  protected abstract void validate(M message) throws Exception;

  /**
   * Implement to process the {@link #validate(AbstractGameBootMessage)}'ed
   * message.
   *
   * @param message
   *          the message
   * @return the response
   * @throws Exception
   *           the exception
   */
  protected abstract Response processImpl(M message) throws Exception;

}
