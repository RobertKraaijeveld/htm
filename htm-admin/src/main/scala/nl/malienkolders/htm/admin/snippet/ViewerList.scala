package nl.malienkolders.htm.admin.snippet

import net.liftweb._
import http._
import mapper._
import util.Helpers._
import scala.xml.NodeSeq
import nl.malienkolders.htm.lib.model._
import java.text.SimpleDateFormat
import java.util.Date

class ViewerList {

  def render = {
    var name = ""
    var url = ""

    def process(): Unit = {
      Viewer.create.alias(name).url(url).save
      S.redirectTo("/viewers/list")
    }

    ".viewer" #> Viewer.findAll.map { v =>
      val status = v.rest.ping
      ".viewer [class+]" #> (if (status) "success" else "danger") &
        ".name *" #> v.alias.get &
        ".url *" #> v.url.get &
        ".status *" #> (if (status) "Up" else "Down") &
        ".actions *" #> SHtml.submit("Delete", () => Viewer.findByKey(v.id.get).foreach(_.delete_!), "class" -> "btn btn-default btn-sm")
    } &
      "name=name" #> SHtml.onSubmit(name = _) &
      "name=url" #> SHtml.onSubmit(url = _) &
      "name=add" #> SHtml.onSubmitUnit(process)
  }

}