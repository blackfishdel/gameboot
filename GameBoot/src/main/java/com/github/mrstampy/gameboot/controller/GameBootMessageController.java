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
package com.github.mrstampy.gameboot.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.locale.processor.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextCodes;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;
import com.github.mrstampy.gameboot.messages.finder.MessageClassFinder;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.AbstractNettyMessageHandler;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler;

/**
 * For browser based games specific JSON messages can easily be paired with
 * their corresponding {@link GameBootProcessor}. This class takes advantage of
 * the {@link MessageClassFinder} implementation (
 * {@link GameBootMessageConverter}) to convert messages to their correct
 * {@link AbstractGameBootMessage} subclass and are submitted to the correct
 * {@link GameBootProcessor} implementation for processing. This functionality
 * facilitates message processing for all messages without the need to determine
 * The Type ahead of time.<br>
 * <br>
 * 
 * GameBoot enforces one {@link GameBootProcessor} per
 * {@link AbstractGameBootMessage}. Implement a different
 * {@link MessageClassFinder} to process alternative messages.
 * 
 * @see AbstractNettyMessageHandler
 * @see AbstractGameBootWebSocketHandler
 */
@Component
public class GameBootMessageController implements ResponseContextCodes {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MESSAGE_COUNTER = "Message Controller Counter";

  @Autowired
  private List<GameBootProcessor<? extends AbstractGameBootMessage>> processors;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private LocaleRegistry localeRegistry;

  private ResponseContextLookup lookup;

  /** The map. */
  protected Map<String, GameBootProcessor<?>> map = new ConcurrentHashMap<>();

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

  /**
   * Post construct, invoke directly from subclass {@link PostConstruct}.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    processors.forEach(p -> map.put(p.getType(), p));

    helper.counter(MESSAGE_COUNTER, GameBootMessageController.class, "message", "counter");
  }

  /**
   * Process the given JSON message using the {@link GameBootProcessor}
   * specified for its {@link AbstractGameBootMessage#getType()}.
   *
   * @param <AGBM>
   *          the generic type
   * @param request
   *          the message
   * @return the response
   * @throws Exception
   *           the exception
   */
  public <AGBM extends AbstractGameBootMessage> String process(String request) throws Exception {
    helper.incr(MESSAGE_COUNTER);

    if (isEmpty(request)) fail(getResponseContext(NO_MESSAGE), "Empty message");

    AGBM msg = converter.fromJson(request);

    Response r = process(msg);

    return r == null ? null : converter.toJson(r);
  }

  /**
   * Exposed for {@link AbstractGameBootMessage}-encapsulating protocols
   * (JSON-RPC, Stomp etc).
   *
   * @param <AGBM>
   *          the generic type
   * @param msg
   *          the msg
   * @return the response
   * @throws Exception
   *           the exception
   */
  @SuppressWarnings("unchecked")
  public <AGBM extends AbstractGameBootMessage> Response process(AGBM msg) throws Exception {
    GameBootProcessor<AGBM> processor = (GameBootProcessor<AGBM>) map.get(msg.getType());

    if (processor == null) {
      log.error("No processor for {}", msg.getType());

      fail(getResponseContext(UNKNOWN_MESSAGE, msg.getSystemId()), "Unrecognized message");
    }

    return processor.process(msg);
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
   * message.
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
}
