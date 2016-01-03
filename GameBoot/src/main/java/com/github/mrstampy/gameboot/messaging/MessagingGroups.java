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
package com.github.mrstampy.gameboot.messaging;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.websocket.WebSocketSessionRegistry;

import io.netty.channel.Channel;

/**
 * MessagingGroups facilitates sending messages to a group of connections,
 * either web sockets, Netty connections or a mix of the two.
 */
@Component
public class MessagingGroups {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Group key for ALL connections. */
  public static final String ALL = "ALL";

  @Autowired
  private NettyConnectionRegistry nettyRegistry;

  @Autowired
  private WebSocketSessionRegistry webSocketRegistry;

  /**
   * Finds either the {@link Channel} or {@link WebSocketSession} associated
   * with the key and sends the message.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   */
  public void send(AbstractRegistryKey<?> key, String message) {
    if (nettyRegistry.contains(key)) {
      nettyRegistry.send(key, message);
    } else if (webSocketRegistry.contains(key)) {
      webSocketRegistry.send(key, message);
    } else {
      log.warn("No connection for {}, cannot send message", key);
    }
  }

  /**
   * Finds either the {@link Channel} or {@link WebSocketSession} associated
   * with the key and sends the message.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   */
  public void send(AbstractRegistryKey<?> key, byte[] message) {
    if (nettyRegistry.contains(key)) {
      nettyRegistry.send(key, message);
    } else if (webSocketRegistry.contains(key)) {
      webSocketRegistry.send(key, message);
    } else {
      log.warn("No connection for {}, cannot send message", key);
    }
  }

  /**
   * Adds the to group.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void addToGroup(String groupName, Channel channel) {
    nettyRegistry.putInGroup(groupName, channel);
  }

  /**
   * Adds the to group.
   *
   * @param groupName
   *          the group name
   * @param session
   *          the session
   */
  public void addToGroup(String groupName, WebSocketSession session) {
    webSocketRegistry.putInGroup(groupName, session);
  }

  /**
   * Removes the from group.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void removeFromGroup(String groupName, Channel channel) {
    nettyRegistry.removeFromGroup(groupName, channel);
  }

  /**
   * Removes the from group.
   *
   * @param groupName
   *          the group name
   * @param session
   *          the session
   */
  public void removeFromGroup(String groupName, WebSocketSession session) {
    webSocketRegistry.removeFromGroup(groupName, session);
  }

  /**
   * Send to all.
   *
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToAll(String message, SystemIdKey... except) {
    sendMessage(ALL, message, except);
  }

  /**
   * Send to all.
   *
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToAll(byte[] message, SystemIdKey... except) {
    sendMessage(ALL, message, except);
  }

  /**
   * Send message.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendMessage(String groupName, String message, SystemIdKey... except) {
    groupNameCheck(groupName);
    if (isEmpty(message)) throw new IllegalArgumentException("No message");

    webSocketRegistry.sendToGroup(groupName, message, except);
    nettyRegistry.sendToGroup(groupName, message, except);
  }

  /**
   * Send message.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendMessage(String groupName, byte[] message, SystemIdKey... except) {
    groupNameCheck(groupName);
    if (message == null || message.length == 0) throw new IllegalArgumentException("No message");

    webSocketRegistry.sendToGroup(groupName, message, except);
    nettyRegistry.sendToGroup(groupName, message, except);
  }

  private void groupNameCheck(String groupName) {
    if (isEmpty(groupName)) throw new NullPointerException("No groupName");
  }
}
