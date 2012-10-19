package dojox.cometd;


public interface Extension
{
    Message rcv(Message message);
    Message rcvMeta(Message message);
    Message send(Message message);
    Message sendMeta(Message message);
}
