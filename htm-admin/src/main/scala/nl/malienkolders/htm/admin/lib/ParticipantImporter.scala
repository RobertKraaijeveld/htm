package nl.malienkolders.htm.admin.lib

import scala.xml._
import java.net.URL
import scala.io.Source
import scala.util.matching._
import nl.malienkolders.htm.lib.model._
import nl.malienkolders.htm.admin.model._
import net.liftweb.mapper._
import net.liftweb.util._

object ParticipantImporter {

  val tournamentNames = List(
    "longsword_open" -> "Longsword Open",
    "longsword_ladies" -> "Longsword Ladies",
    "wrestling" -> "Wrestling",
    "sabre" -> "Sabre",
    "rapier_dagger" -> "Rapier & Dagger",
    "sword_buckler" -> "Sword & Buckler")

  def readTuplesFromFile(filename: String) = Source.fromInputStream(ParticipantImporter.getClass().getResourceAsStream(filename), "UTF-8").getLines().toList.map(line => line.split(" -> ") match {
    case Array(code, name) => code -> name
    case _ => "" -> ""
  })

  lazy val clubCode2Name = Map(readTuplesFromFile("clubcodes"): _*)

  lazy val clubName2Code = clubCode2Name.map { case (c, n) => (n, c) }

  lazy val replacements = Map(readTuplesFromFile("clubreplacements").map { case (o, r) => (o.toLowerCase(), r) }: _*)

  def nameReplacements = Map(("9", "F. v. d. Bussche-H.") :: ParticipantNameMapping.findAll.map(pnm => (pnm.externalId.is -> pnm.shortName.is)).toList: _*)

  def normalizeName(nameRaw: String) = {
    def normalizePart(part: String) = {
      val subparts = part.split("-")
      subparts.map(sb => if (sb.length() > 3) sb.take(1).toUpperCase() + sb.drop(1).toLowerCase() else sb).mkString("-")
    }
    val name = nameRaw.replaceAll("\\s+", " ").trim()
    val parts = name.split(" ").toList
    parts.map(normalizePart _).mkString(" ")
  }

  def shortenName(name: String) = {
    val allParts = name.split(" ")
    val uppercasedParts = allParts.takeWhile(_.charAt(0).isUpper)
    val initials = uppercasedParts.take(allParts.length - 1).map(_.charAt(0) + ".")
    (initials.toList.mkString + " " + allParts.drop(initials.length).mkString(" ")).replace(" van ", " v. ").replace(" von dem ", " v.d. ").replace(" von ", " v. ")
  }

  def normalizeClub(clubRaw: String) = {
    def uppercaseWord(word: String) = if (word.length() > 3 && !word.contains(".")) word.take(1).toUpperCase() + word.drop(1) else word
    val club = clubRaw.replaceAll("\\s+", " ").trim()
    println("club: " + club)
    val replaced: String = replacements.getOrElse(club.toLowerCase(), club)
    println("replaced: " + replaced)
    val uppercased = replaced.split(" ").map(uppercaseWord _).mkString(" ")
    println("uppercased: " + uppercased)
    if (uppercased == "" || uppercased == "-")
      ("", "")
    else if (clubCode2Name.contains(uppercased))
      (uppercased, clubCode2Name(uppercased))
    else if (clubName2Code.contains(uppercased))
      (clubName2Code(uppercased), uppercased)
    else
      ("", uppercased)
  }

  def doImport = {
    val noCountry = Country.find(By(Country.code2, "")).get

    replacements.foreach(println _)

    val tournaments = if (Tournament.count == 0) {
      tournamentNames.map { case (identifier, name) => Tournament.create.name(name).identifier(identifier).saveMe }
    } else {
      Tournament.findAll(OrderBy(Tournament.id, Ascending))
    }

    if (Tournament.count > 0) {
      tournaments.zip(tournamentNames).foreach {
        case (t, (identifier, _)) =>
          t.identifier(identifier).save
      }
    }

    val data = Source.fromURL(new URL("http://www.ghfs.se/swordfish-attendee.php"), "UTF-8").getLines.mkString
    val entry = new Regex("""<tr><td bgcolor="[^"]+">(\d+)</td>""" +
      """<td bgcolor="[^"]+">([^<]+)</td>""" +
      """<td bgcolor="[^"]+">([^<]*)</td>""" +
      """<td bgcolor="[^"]+">([^<]*)</td>""" +
      """<td bgcolor="[^"]+" align="center">(X?)</td>""" +
      """<td bgcolor="[^"]+" align="center">(X?)</td>""" +
      """<td bgcolor="[^"]+" align="center">(X?)</td>""" +
      """<td bgcolor="[^"]+" align="center">(X?)</td>""" +
      """<td bgcolor="[^"]+" align="center">(X?)</td>""" +
      """<td bgcolor="[^"]+" align="center">(X?)</td>""" +
      """<td bgcolor="[^"]+" align="center">([^<]*)</td></tr>""")
    val entries = (for (
      entry(id, name, club, _, longsword, longswordF, wrestling, sabre, rapier, swordbuckler, countryName) <- entry findAllIn data
    ) yield {
      val (clubCode, clubName) = normalizeClub(club)
      val country = Country.find(By(Country.name, countryName)) openOr (noCountry)
      (Participant.find(By(Participant.externalId, id)).map(_.country(country)).openOr(Participant.create.
        externalId(id).
        name(normalizeName(name)).
        shortName(nameReplacements.get(id).getOrElse(shortenName(normalizeName(name)))).
        club(clubName).
        clubCode(clubCode).
        country(country)),
        List(longsword, longswordF, wrestling, sabre, rapier, swordbuckler))
    }).toList

    // insert participant if it doesn't exist yet
    entries.foreach {
      case (p, _) =>
        p.save
    }

    // add participants to tournaments
    tournaments.foreach { t =>
      t.participants.clear
      t.save
    }
    entries.foreach {
      case (p, ts) =>
        ts.zipWithIndex.filter(_._1 == "X").foreach { case (_, index) => tournaments(index).participants += p }
    }
    tournaments.foreach(_.save)
  }

}