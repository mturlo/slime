Slime
============

Slack Bot Akka Actor

Usage:

```scala
import com.cyberdolphins.slime._
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.outgoing._
import com.cyberdolphins.slime.common._

class Slime extends SlackBotActor {
  override def eventReceive: EventReceive = {

    case SimpleInboundMessage("speak!", channel, user) =>

      val fields = (1 to 10).map(i => Field(s"Field no. *$i*"))

      publish(ComplexOutboundMessage("Spoken!", channel, user,
        Attachment("A Attachment", "http://www.imdb.com/title/tt3230854/", "Woohoo!"),
        Attachment("Another Attachment")
        .withColor(Color.good)
        .withFields(fields: _*)
        .withMarkdownIn(MarkdownInValues.fields)))
  }
}


object Slime extends App {

  val actorSystem = ActorSystem()

  val slimeBotActor = actorSystem.actorOf(Props[Slime], "slime-bot")

  slimeBotActor ! Connect(System.getenv("SLACK_API_KEY"))

  sys.addShutdownHook {
    slimeBotActor ! Close
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

  actorSystem.awaitTermination()
}
```
