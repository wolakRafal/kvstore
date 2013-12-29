package kvstore

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._

object Replicator {

  case class Replicate(key: String, valueOption: Option[String], id: Long)

  case class Replicated(key: String, id: Long)

  case class Snapshot(key: String, valueOption: Option[String], seq: Long)

  case class SnapshotAck(key: String, seq: Long)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {

  import Replicator._
  import Replica._
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  // map from sequence number to pair of sender and request
  var acks = Map.empty[Long, (ActorRef, Replicate)]
  // a sequence of not-yet-sent snapshots (you can disregard this if not implementing batching)
  var pending = Vector.empty[Snapshot]

  var _seqCounter = 0L

  def nextSeq = {
    val ret = _seqCounter
    _seqCounter += 1
    ret
  }
  var expectedSeqNo = 0L

  /* TODO Behavior for the Replicator. - retransmisja */
  def receive: Receive = {
    case Replicator.Replicate(key, valueOption, id) =>
      val seqNo = nextSeq
      acks = acks.updated(seqNo, (sender, Replicate(key, valueOption, id)))
      replica ! Replicator.Snapshot(key, valueOption, seqNo)
      expectedSeqNo = seqNo
    case Replicator.SnapshotAck(key, seq)  =>
      if(seq < expectedSeqNo){
        val (receiver, replicateMsg) = acks(seq)
        receiver ! Replicator.Replicated(replicateMsg.key, replicateMsg.id)
      } else {
        expectedSeqNo = seq + 1

      }
  }

}
