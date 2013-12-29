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

import akka.actor.{Terminated, ActorLogging, ActorRef, Actor}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Customer {

  case object CaffeineWithdrawalWarning

}

class Customer(coffeeSource: ActorRef) extends Actor with ActorLogging {

  import Customer._
  import Barista._
  import EspressoCup._

  context.watch(coffeeSource)

  def receive = {
    case CaffeineWithdrawalWarning => coffeeSource ! EspressoRequest
    case (EspressoCup(Filled), Receipt(amount)) =>
      log.info(s"yay, caffeine for ${self}!")
    case ComebackLater => log.info("czekam, czekam sobie")
      context.system.scheduler.scheduleOnce(300.millis) {
        coffeeSource ! EspressoRequest
      }
    case Terminated(bsrista) =>
      log.info("Oh well, let's find another coffeehouse...")
  }
}
