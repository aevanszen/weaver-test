package weaver

import LogFormatter.{ formatTimestamp }
import weaver.Log.{ debug, error, info, warn }
import cats.data.Chain
import cats.syntax.show._

object Formatter {

  val EOL        = java.lang.System.lineSeparator()
  val DOUBLE_EOL = EOL * 2

  sealed abstract class Tabulation(val prefix: String) {
    override def toString = prefix
  }
  case object TAB2 extends Tabulation("  ")
  case object TAB4 extends Tabulation("    ")

  def formatResultStatus(name: String, result: Result): String = {
    val tabulatedTestLines = name.split("\\r?\\n").map(TAB2 -> _).toList

    def withPrefix(newPrefix: String): String = {
      tabulatedTestLines match {
        case (_, firstLine) :: Nil => newPrefix + firstLine
        case (_, firstLine) :: extraLines =>
          newPrefix + firstLine + EOL + extraLines
            .map(l => l._1.prefix + l._2)
            .mkString(EOL)
        case Nil => newPrefix + ""
      }
    }

    import Result._

    result match {
      case Success                                 => withPrefix(green("+ "))
      case _: Failure | _: Failures | _: Exception => withPrefix(red("- "))
      case _: Cancelled =>
        withPrefix(yellow("- ")) + yellow(" !!! CANCELLED !!!")
      case _: Ignored => withPrefix(yellow("- ")) + yellow(" !!! IGNORED !!!")
    }
  }

  def outcomeWithResult(outcome: TestOutcome, result: Result): String = {

    import outcome._

    val builder = new StringBuilder()
    val newLine = '\n'
    builder.append(formatResultStatus(name, result))
    result.formatted.foreach { resultInfo =>
      builder.append(EOL)
      builder.append(resultInfo)
    }
    if (status.isFailed) {
      val hasDebugOrError =
        log.exists(e => List(debug, error).contains(e.level))
      val shortLevelPadder = if (hasDebugOrError) "  " else " "
      val levelPadder: Log.Level => String = {
        case `info` | `warn`   => shortLevelPadder
        case `debug` | `error` => " "
      }

      val eff = log.map { entry =>
        builder.append(TAB4)
        val loc = entry.location.fileName
          .map(fn => s"[$fn:${entry.location.line}]")
          .getOrElse("")

        builder.append(s"${entry.level.show}${levelPadder(entry.level)}")
        builder.append(s"${formatTimestamp(entry.timestamp)} ")
        builder.append(s"$loc ")
        builder.append(entry.msg)
        val keyLengthMax =
          entry.ctx.map(_._1.length).foldLeft[Int](0)(math.max)

        entry.ctx.foreach {
          case (k, v) =>
            builder.append(newLine)
            builder.append(TAB4.prefix * 2)
            builder.append(k)
            (0 to (keyLengthMax - k.length)).foreach(_ => builder.append(" "))
            builder.append("-> ")
            builder.append(v)
        }
        builder.append(newLine)

        ()
      }

      discard[Chain[Unit]](eff)
      if (log.nonEmpty) {
        builder.append(newLine)
      }
    }
    builder.mkString
  }
}