package dojox.cometd;

import java.util.Map;


/* ------------------------------------------------------------ */
/** A Bayeux Message
 * A Map of String to Object that has been optimized for conversion to JSON messages.
 * 
 * @author gregw
 *
 */
public interface Message extends Map<String,Object>
{
    public String getClientId();
    public String getChannel();
    public Object getId();
    public Message getAssociated();
    public void setAssociated(Message message);
}


