/** ***************************************************************************
  * Copyright (c) 2013
  * ADVA Optical Networking
  *
  * All rights reserved. Any unauthorized disclosure or publication of the
  * confidential and proprietary information to any other party will constitute
  * an infringement of copyright laws.
  *
  * $Id$
  * Author  : Rafal Wolak, RWolak@advaoptical.com
  * Created : 28 December 2013
  * Purpose :
  *
  * $Rev$
  * $URL$
  *
  * Notes:
  *
  * ****************************************************************************
  */

package coffee

import akka.actor.{OneForOneStrategy, SupervisorStrategy, Props, Actor}
import akka.actor.SupervisorStrategy.{Resume,Directive}
import coffee.ReceiptPrinter.PaperJamException
import akka.pattern.AskTimeoutException

object Barista {
  case object EspressoRequest
  case object ClosingTime
  case class EspressoCup(state: EspressoCup.State)
  object EspressoCup {
    sealed trait State
    case object Clean extends State
    case object Filled extends State
    case object Dirty extends State
  }
  case class Receipt(amount: Int)
  case object ComebackLater
}
class Barista extends Actor {
  import Barista._
  import Register._
  import EspressoCup._
  import context.dispatcher
  import akka.util.Timeout
  import akka.pattern.ask
  import akka.pattern.pipe
  import concurrent.duration._

  implicit val timeout = Timeout(1.seconds)
  val register = context.actorOf(Props[Register], "Register")
  def receive = {
    case EspressoRequest =>
      val receipt = register ? Transaction(Espresso)
      receipt.map((EspressoCup(Filled), _)).recover{
        case _: AskTimeoutException => ComebackLater
      } pipeTo(sender)
    case ClosingTime => context.system.shutdown()
  }
}
