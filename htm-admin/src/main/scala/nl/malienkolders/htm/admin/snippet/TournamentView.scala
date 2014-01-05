package nl.malienkolders.htm.admin.snippet

import net.liftweb._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.util.Helpers._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.mapper._
import nl.malienkolders.htm.lib.rulesets.Ruleset
import nl.malienkolders.htm.lib.model._
import nl.malienkolders.htm.lib.util.Helpers._
import nl.malienkolders.htm.admin.lib._
import nl.malienkolders.htm.admin.lib.exporter._
import nl.malienkolders.htm.admin.lib.Utils.TimeRenderHelper
import net.liftweb.http.SHtml.ElemAttr.pairToBasic
import net.liftweb.sitemap.LocPath.stringToLocPath
import net.liftweb.util.IterableConst.itBindable
import net.liftweb.util.IterableConst.itNodeSeqFunc
import net.liftweb.util.IterableConst.itStringPromotable
import net.liftweb.util.StringPromotable.jsCmdToStrPromo
import scala.xml.Text
import scala.xml.EntityRef
import scala.xml.EntityRef
import scala.collection.mutable.ListBuffer
import scala.util.Random
import java.text.SimpleDateFormat
import java.util.Date

case class ParamInfo(param: String)

object TournamentView {
  val menu = Menu.param[ParamInfo]("View Tournament", "View Tournament", s => Full(ParamInfo(s)),
    pi => pi.param) / "tournaments" / "view"
  lazy val loc = menu.toLoc

  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def render = {
    val t = {
      val param = TournamentView.loc.currentValue.map(_.param).get
      param match {
        case id if id.matches("\\d+") => Tournament.findByKey(id.toLong).get
        case s => Tournament.find(By(Tournament.identifier, TournamentView.loc.currentValue.map(_.param).get)).get
      }
    }

    val tournamentSubscriptions = t.subscriptions.sortBy(_.fighterNumber.is)
    val otherParticipants = Participant.findAll(OrderBy(Participant.name, Ascending)) diff t.subscriptions.map(_.participant.obj.get).toList

    def addParticipant(tournament: Tournament, participantId: Long) {
      val participant = Participant.findByKey(participantId).get
      tournament.subscriptions += TournamentParticipants.create.
        participant(participant).
        experience(0).
        gearChecked(true).
        fighterNumber(tournament.nextFighterNumber)
      tournament.save
      refresh()
    }

    def deleteParticipantFromTournament(tournament: Tournament, participant: Participant) {
      //TODO      tournament.subscriptions -= participant
      tournament.save
      S.notice("%s has been removed from this tournament" format participant.name.is)
    }

    def toggleStar(p: Participant) = {
      p.reload
      if (p.isStarFighter.is)
        p.isStarFighter(false)
      else
        p.isStarFighter(true)
      if (p.save)
        Run("$(\".star" + p.id + "\").attr(\"src\", \"/images/" + (if (p.isStarFighter.get) "star" else "star_gray") + ".png\");")
      else
        Alert("Could not save star status")
    }

    def refresh(anchor: Option[Any] = None) =
      S.redirectTo(t.id.is.toString + anchor.map {
        case p: Participant => "#participant" + p.id.is
        case p: Phase[_] => "#phase" + p.id.is
        case _ => ""
      }.getOrElse(""))

    "#tournamentName" #> t.name &
      "name=tournamentArena" #> SHtml.ajaxSelect(Arena.findAll.map(a => a.id.get.toString -> a.name.get), t.defaultArena.box.map(_.toString), { arena => t.defaultArena(arena.toLong); t.save; S.notice("Default arena changed") }) &
      ".downloadButton" #> Seq(
        "a" #> SHtml.link("/download/pools", () => throw new ResponseShortcutException(downloadPools(t)), Text("Pools")),
        "a" #> SHtml.link("/download/schedule", () => throw new ResponseShortcutException(downloadSchedule(t)), Text("Schedule"))) &
        "#tournamentParticipantsCount *" #> tournamentSubscriptions.size &
        "#tournamentParticipant" #> tournamentSubscriptions.map(sub =>
          "* [class+]" #> (if (sub.participant.obj.get.isPresent.get && sub.gearChecked.get) "present" else if (!sub.participant.obj.get.isPresent.get) "not_present" else "not_checked") &
            "a [href]" #> s"/participants/register/${sub.participant.obj.get.externalId.get}#tournament${t.id.get}" &
            ".badge *" #> sub.fighterNumber.get &
            ".name *" #> (sub.participant.obj.get.name.get + " " + sub.experience.get) &
            "button" #> SHtml.ajaxButton(EntityRef("otimes"), () => { deleteParticipantFromTournament(t, sub.participant.obj.get); refresh() }, "title" -> "Remove from Tournament")) &
        "#phase" #> t.phases.map(p =>
          ".phaseAnchor [name]" #> ("phase" + p.id.get) &
            "#phaseName *" #> <span><a name={ "phase" + p.id.is }></a>{ p.order.is + ": " + p.name.is }</span> &
            "name=ruleset" #> SHtml.ajaxSelect(Ruleset.rulesets.toList.map(r => r._1 -> r._1), Full(p.ruleset.get), { ruleset => p.ruleset(ruleset); p.save; S.notice("Ruleset changed for " + p.name.is) }) &
            "name=timeLimit" #> SHtml.ajaxText((p.timeLimitOfFight.get / 1000).toString, { time => p.timeLimitOfFight(time.toLong seconds); p.save; S.notice("Time limit saved") }, "type" -> "number") &
            "name=fightBreak" #> SHtml.ajaxText((p.breakInFightAt.get / 1000).toString, { time => p.breakInFightAt(time.toLong seconds); p.save; S.notice("Break time saved") }, "type" -> "number") &
            "name=fightBreakDuration" #> SHtml.ajaxText((p.breakDuration.get / 1000).toString, { time => p.breakDuration(time.toLong seconds); p.save; S.notice("Break duration saved") }, "type" -> "number") &
            "name=exchangeLimit" #> SHtml.ajaxText(p.exchangeLimit.toString, { time => p.exchangeLimit(time.toInt); p.save; S.notice("Exchange limit saved") }, "type" -> "number") &
            "name=timeBetweenFights" #> SHtml.ajaxText((p.timeBetweenFights.get / 1000).toString, { time => p.timeBetweenFights(time.toLong seconds); p.save; S.notice("Time between fights saved") }, "type" -> "number") &
            "#addParticipant" #> SHtml.ajaxSelect(("-1", "-- Add Participant --") :: otherParticipants.map(pt => (pt.id.is.toString, pt.name.is)).toList, Full("-1"), id => addParticipant(t, id.toLong), "class" -> "form-control"))

  }

  def downloadSchedule(tournament: Tournament) = {
    OutputStreamResponse(ScheduleExporter.doExport(tournament) _, List("content-disposition" -> ("inline; filename=\"schedule_" + tournament.identifier.get + ".xls\"")))
  }

  def downloadPools(tournament: Tournament) = {
    OutputStreamResponse(PoolsExporter.doExport(tournament) _, List("content-disposition" -> ("inline; filename=\"pools_" + tournament.identifier.get + ".xls\"")))
  }

}