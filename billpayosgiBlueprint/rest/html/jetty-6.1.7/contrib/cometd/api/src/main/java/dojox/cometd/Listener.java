// ========================================================================
// Copyright 2007 Mort Bay Consulting Pty. Ltd.
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
/** Cometd Receiver interface.
 * A receive in an object that can receive a message for a Bayeux {@link Client}.
 * 
 * @author gregw
 *
 */
public interface Listener
{
    /**
     * This method is called when the client is removed (explicitly or from a timeout)
     */
    public void removed(String clientId, boolean timeout);
    
    /**
     * Called when a message is delivered to the client
     * @param msg TODO
     */
    public void deliver(Client fromClient, Client toClient, Message msg);
}
