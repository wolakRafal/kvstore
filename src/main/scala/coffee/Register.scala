package coffee

import akka.actor._
import scala.concurrent.duration._
import akka.pattern.ask
import akka.pattern.pipe
import coffee.ReceiptPrinter.{PaperJamException, PrintJob}
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.SupervisorStrategy.Directive

object Register {
  sealed trait Article
  case object Espresso extends Article
  case object Cappuccino extends Article
  case class Transaction(article: Article)
}
class Register extends Actor with ActorLogging {

//  override def supervisorStrategy = OneForOneStrategy(10, 2.minutes) {
//    case _ => Restart
//  }

  val decider: PartialFunction[Throwable, Directive] = {
    case _: PaperJamException => SupervisorStrategy.Resume
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy()(decider.orElse(SupervisorStrategy.defaultStrategy.decider))

implicit val timeout = Timeout(1.seconds)
  import Register._
  import Barista._
  var revenue = 0
  val prices = Map[Article, Int](Espresso -> 150, Cappuccino -> 250)
  val printer = context.actorOf(Props[ReceiptPrinter], "Printer")

  def receive = {
    case Transaction(article) =>
      val price = prices(article)
      val requester = sender
      (printer ? PrintJob(price)).map((requester, _)).pipeTo(self)
    case (requester: ActorRef, receipt: Receipt) =>
      revenue += receipt.amount
      log.info(s"Revenue is $revenue cents")
      requester ! receipt
  }

}
