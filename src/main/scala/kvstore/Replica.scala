package kvstore

import akka.actor.{OneForOneStrategy, Props, ActorRef, Actor}
import kvstore.Arbiter._
import scala.collection.immutable.Queue
import akka.actor.SupervisorStrategy.Restart
import scala.annotation.tailrec
import akka.pattern.{ask, pipe}
import akka.actor.Terminated
import scala.concurrent.duration._
import akka.actor.PoisonPill
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy
import akka.util.Timeout

object Replica {

  sealed trait Operation {
    def key: String

    def id: Long
  }

  case class Insert(key: String, value: String, id: Long) extends Operation

  case class Remove(key: String, id: Long) extends Operation

  case class Get(key: String, id: Long) extends Operation

  sealed trait OperationReply

  case class OperationAck(id: Long) extends OperationReply

  case class OperationFailed(id: Long) extends OperationReply

  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))

  // nie zatwierdzone wartości
  val NotAckPrefix = "NoAck"
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {

  import Replica._
  import Replicator._
  import Persistence._
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  var kv = Map.empty[String, String]
  // elements waiting for ack
  var waitingAck = Map.empty[Long, (Option[String], ActorRef)]


  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]

  var persistence: ActorRef = _

  override def preStart() = {
    // create replicator
    //replicators = replicators + context.actorOf(Replicator.props(self), "replicator")
    // create persistence
    persistence = context.actorOf(persistenceProps, "Persistence-leader")
    // join to database
    arbiter ! Join
  }

  def receive = {
    case JoinedPrimary => context.become(leader)
    case JoinedSecondary => context.become(replica)
  }

  /* TODO Behavior for  the leader role. */
  val leader: Receive = {
    // KV Protocol
    case Replica.Insert(key, value, id) => insert(key, value, id)

    case Replica.Remove(key, id) => remove(key, id)

    // ACK from Persistence - may by flaky - TODO
    case Persistence.Persisted(key, id) => persistedAck(key, id)

    case Replica.Get(key, id) => get(key, id)

  }

  /* TODO Behavior for the replica role. */
  val replica: Receive = {
    case Replica.Get(key, id) => get(key, id)
//    case
  }

  // Private methods
  private def get(key: String, id: Long) = {
    if (kv.contains(key)) sender ! Replica.GetResult(key, Some(kv(key)), id)
    else sender ! Replica.GetResult(key, None, id)
  }

  private def insert(key: String, value: String, id: Long) = {
    waitingAck = waitingAck.updated(id, (Some(value), sender))
    persistence ! Persistence.Persist(key, Some(value), id)
  }

  private def remove(key: String, id: Long) = {
    waitingAck = waitingAck.updated(id, (None, sender))
    persistence ! Persistence.Persist(key, None, id)
  }

  private def persistedAck(key: String, id: Long) = {
    if (waitingAck.contains(id)) {
      val (valueOption, client) = waitingAck(id)
      if (valueOption.isDefined) {
        kv = kv.updated(key, valueOption.get)
      } else {
        kv = kv - key
      }
      waitingAck = waitingAck - id
      client ! OperationAck(id)
    }
  }
}
