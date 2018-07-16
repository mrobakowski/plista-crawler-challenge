package com.mrobakowski.plistachallenge

import java.net.URL

import com.typesafe.scalalogging.StrictLogging

import scala.io.StdIn
import scala.util.Try

object Main extends StrictLogging {
  val index = new SimplePageIndex()
  val crawler = new Crawler(index)

  def main(args: Array[String]): Unit = {
    while (true) {
      val line = StdIn.readLine(s"(pages: ${index.size()})> ")
      if (line == null) return

      line.trim match {
        case Search(query) => search(query)
        case Crawl(url) => crawl(url)
        case "clear" => clear()
        case "stop" => stop()
        case "" => // do nothing
        case _ => println("I do not understand that command. Available commands:\n" +
          "search <regex>\ncrawl <url>\nclear")
      }
    }
  }

  def search(query: String): Unit = {
    val pages = index.search(query)
    if (pages.isEmpty) println("I found nothing")
    else println(s"Here's what i found:\n${pages.map { p => s"  ${p.title}\n    ${p.url}" }.mkString("\n")}")
  }

  def crawl(urlStr: String): Unit = {
    val url = Try(new URL(urlStr)).getOrElse {
      println("Invalid url")
      return
    }

    crawler.queueCrawl(url)
  }

  def stop(): Unit = {
    crawler.stop()
  }

  def clear(): Unit = {
    index.clear()
  }
}

object Search {
  def unapply(line: String): Option[String] = {
    if (line.startsWith("search ")) Some(line.stripPrefix("search ").trim)
    else None
  }
}

object Crawl {
  def unapply(line: String): Option[String] = {
    if (line.startsWith("crawl ")) Some(line.stripPrefix("crawl ").trim)
    else None
  }
}