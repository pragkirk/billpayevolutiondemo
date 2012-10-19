// ========================================================================
// Copyright 2007 Dojo Foundation
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//========================================================================

package dojox.cometd;




/* ------------------------------------------------------------ */
/** Bayeux Interface.
 * This interface represents the server side API for the  Bayeux messaging protocol.
 *
 * Bayeux implementations must be thread safe and multiple threads may simultaneously
 * call Bayeux methods.
 * 
 */
public interface Bayeux
{
    public static final String META="/meta";
    public static final String META_SLASH="/meta/";
    public static final String META_CONNECT="/meta/connect";
    public static final String META_CLIENT="/meta/client";
    public static final String META_DISCONNECT="/meta/disconnect";
    public static final String META_HANDSHAKE="/meta/handshake";
    public static final String META_PING="/meta/ping";
    public static final String META_RECONNECT="/meta/reconnect";  // deprecated
    public static final String META_STATUS="/meta/status";
    public static final String META_SUBSCRIBE="/meta/subscribe";
    public static final String META_UNSUBSCRIBE="/meta/unsubscribe";
    public static final String CLIENT_FIELD="clientId";
    public static final String DATA_FIELD="data";
    public static final String CHANNEL_FIELD="channel";
    public static final String ID_FIELD="id";
    public static final String ERROR_FIELD="error";
    public static final String TIMESTAMP_FIELD="timestamp";
    public static final String TRANSPORT_FIELD="transport";
    public static final String ADVICE_FIELD="advice";
    public static final String SUCCESSFUL_FIELD="successful";
    public static final String SUBSCRIPTION_FIELD="subscription";
    public static final String EXT_FIELD="ext";
    
    public static final String SERVICE="/service";
    public static final String SERVICE_SLASH="/service/";
    
    /** ServletContext attribute name used to obtain the Bayeux object */
    public static final String DOJOX_COMETD_BAYEUX="dojox.cometd.bayeux";
    
    /* ------------------------------------------------------------ */
    /**
     * @param idprefix
     * @param listener
     * @return
     */
    public Client newClient(String idprefix, Listener listener);

    /* ------------------------------------------------------------ */
    /**
     * @param client_id
     * @return
     */
    public Client getClient(String client_id);
    
    /* ------------------------------------------------------------ */
    public Channel getChannel(String channelId,boolean create);

    /* ------------------------------------------------------------ */
    /** Deliver data to a channel.
     * @param fromClient The client sending the data
     * @param data The data itself which must be an Object that can be encoded with {@link JSON}.
     * @param toChannel The Channel ID to which the data is targetted
     * @param msgId optional message ID or null for automatic generation of a message ID.
     */
    public void publish(Client fromClient, String toChannel, Object data, String msgId);

    /* ------------------------------------------------------------ */
    /** Deliver a message to a client.
     */
    public void deliver(Client fromClient, Client toClient, String toChannel, Message message);

    /* ------------------------------------------------------------ */
    /** Subscribe to a channel.
     * Equivalent to getChannel(toChannel).subscribe(subscriber).
     * @param toChannel
     * @param subscriber
     * @param createChannel. Create the channel if it does not exist
     */
    public void subscribe(String toChannel, Client subscriber);

    /* ------------------------------------------------------------ */
    /** Unsubscribe to a channel
     * @param toChannel
     * @param subscriber
     */
    public void unsubscribe(String toChannel, Client subscriber);
    
    /* ------------------------------------------------------------ */
    public boolean hasChannel(String channel);

    /* ------------------------------------------------------------ */
    public void addFilter(String channels, DataFilter filter);
    
    /* ------------------------------------------------------------ */
    public void removeFilter(String channels, DataFilter filter);
    
    /* ------------------------------------------------------------ */
   public SecurityPolicy getSecurityPolicy();
   
    /* ------------------------------------------------------------ */
   public void setSecurityPolicy(SecurityPolicy securityPolicy);
   
   /* ------------------------------------------------------------ */
   public Message newMessage();
   
    
}