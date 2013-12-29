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

import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import kvstore.{Persistence, Replica, Tools}
import kvstore.Arbiter.Join
import coffee.Barista._
import coffee.Barista.EspressoCup.Filled
import scala.tools.cmd.gen.AnyVals
import coffee.Barista.Receipt

class CoffeeTestSpec extends TestKit(ActorSystem("Coffeehouse")) with FunSuite
with BeforeAndAfterAll
with ShouldMatchers
with ImplicitSender {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  test("case1: Caffein for all addicted") {
    import Customer._
    val testProbe = TestProbe()
    val barista = system.actorOf(Props[Barista], "Barista")
    val customerJohnny = system.actorOf(Props(classOf[Customer], barista), "Johnny")
    val customerAlina = system.actorOf(Props(classOf[Customer], barista), "Alina")

    customerJohnny ! CaffeineWithdrawalWarning
    customerAlina ! CaffeineWithdrawalWarning
    Thread.sleep(1000L)
    barista ! ClosingTime
    customerJohnny ! CaffeineWithdrawalWarning
    customerAlina ! CaffeineWithdrawalWarning

//    testProbe.send(barista, EspressoRequest)
//    testProbe.expectMsg((EspressoCup(Filled), Receipt(150)))
  }
}

