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
package com.github.mrstampy.gameboot.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.mrstampy.gameboot.util.GameBootRegistry;

/**
 * The superclass for all GameBoot JSON messages.
 */
@JsonInclude(Include.NON_NULL)
public abstract class AbstractGameBootMessage {

  private Integer id;

  private String type;

  private String systemSessionId;

  /**
   * The Enum Transport.
   */
  public enum Transport {

    /** The web. */
    WEB,
    /** The web socket. */
    WEB_SOCKET,
    /** The netty. */
    NETTY;
  }

  private Transport transport = Transport.WEB;

  /**
   * Instantiates a new abstract game boot message.
   *
   * @param type
   *          the type
   */
  protected AbstractGameBootMessage(String type) {
    setType(type);
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public Integer getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id
   *          the new id
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type
   *          the new type
   */
  public void setType(String type) {
    this.type = type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  /**
   * Gets the system session id, transient data set (or not) by message
   * processing to indicate keys for the various {@link GameBootRegistry}s.
   *
   * @return the system session id
   */
  @JsonIgnore
  public String getSystemSessionId() {
    return systemSessionId;
  }

  /**
   * Sets the system session id.
   *
   * @param systemSessionId
   *          the new system session id
   */
  public void setSystemSessionId(String systemSessionId) {
    this.systemSessionId = systemSessionId;
  }

  /**
   * Gets the transport.
   *
   * @return the transport
   */
  @JsonIgnore
  public Transport getTransport() {
    return transport;
  }

  /**
   * Sets the transport.
   *
   * @param transport
   *          the new transport
   */
  public void setTransport(Transport transport) {
    this.transport = transport;
  }

}
