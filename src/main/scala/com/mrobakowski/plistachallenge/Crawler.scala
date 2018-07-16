package com.mrobakowski.plistachallenge

import java.net.URL
import java.util.concurrent.LinkedBlockingDeque

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import org.scalactic.Snapshots

import scala.annotation.tailrec
import scala.util.Try

class Crawler(val pageIndex: PageIndex, private val browser: Browser = JsoupBrowser()) extends AutoCloseable with StrictLogging {
  // deque is used instead of a queue because we want to be able to stop the crawler before it processes rest of the
  // queued crawl requests
  private val commandQueue = new LinkedBlockingDeque[CrawlerCommand]()

  private val crawlerThread = new Thread {
    override def run(): Unit = {
      logger.info("started crawler thread")
      crawlerLoop()
    }
  }

  crawlerThread.start()

  @tailrec private def crawlerLoop(): Unit = {
    commandQueue.take() match {
      case CloseCrawler => logger.info("CloseCrawler command received; stopping crawler thread..."); return
      case StopCrawler => logger.info("Clearing crawler queue"); commandQueue.clear()
      case CrawlUrl(url, allowSubdomains, forceReCrawl) => try {
        crawlUrl(url, allowSubdomains, forceReCrawl)
      } catch {
        case e: Exception =>
          logger.error(s"error when crawling $url: ${e.getMessage}")
          logger.debug("exception: ", e)
      }
    }

    crawlerLoop()
  }

  private def linksToHost(contextUrl: URL, allowSubdomains: Boolean) = {
    import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
    import net.ruippeixotog.scalascraper.dsl.DSL._

    val hostname = contextUrl.getHost

    val urlFilter: URL => Boolean = if (allowSubdomains) { url: URL =>
      val host = url.getHost
      host == hostname || host.endsWith(s".$hostname")
    } else {
      _.getHost == hostname
    }

    elementList("[href]").map(elemList => elemList.flatMap { element =>
      val href = element >> attr("href")
      Try(new URL(contextUrl, href)).toOption
    }.filter(urlFilter))
  }

  private def normalizeUrl(url: URL): URL = {
    // for now we just strip the anchor part of the url
    new URL(s"${url.getProtocol}://${url.getAuthority}${url.getFile}")
  }

  private def crawlUrl(url: URL, allowSubdomains: Boolean, forceReCrawl: Boolean = false): Unit = {
    import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
    import net.ruippeixotog.scalascraper.dsl.DSL._

    val normalizedUrl = normalizeUrl(url)

    // don't crawl if we already have the page indexed - this prevents infinite loops when pages link to each other
    if (!forceReCrawl && pageIndex.has(normalizedUrl)) {
      logger.debug(s"$normalizedUrl is already in the index, not crawling")
      return
    }

    logger.debug(s"crawling $normalizedUrl")
    val doc = browser.get(normalizedUrl.toString)

    val documentText = doc >> allText
    val documentTitle = doc >> text("title")
    logger.debug(Snapshots.snap(normalizedUrl, documentTitle).lines)

    pageIndex.add(Page(normalizedUrl, documentTitle, documentText))

    val outgoingLinks = doc >> linksToHost(normalizedUrl, allowSubdomains)
    logger.debug(Snapshots.snap(outgoingLinks).lines)

    // We don't simply recurse to crawlUrl here, but add to the queue, so we don't cause stack overflow.
    // This has an additional benefit of making crawling breadth-first
    outgoingLinks.foreach(link => queueCrawl(link, allowSubdomains))

    notifyIfQueueEmpty()
  }

  def queueCrawl(url: URL, allowSubdomains: Boolean = false, forceReCrawl: Boolean = false): Unit = {
    commandQueue.offer(CrawlUrl(url, allowSubdomains, forceReCrawl))
  }

  override def close(): Unit = {
    commandQueue.offerFirst(CloseCrawler)
    crawlerThread.join()
  }

  def stop(): Unit = {
    commandQueue.offerFirst(StopCrawler)
  }

  private def notifyIfQueueEmpty(): Unit = commandQueue.synchronized {
    if (commandQueue.isEmpty) commandQueue.notifyAll()
  }

  def waitUntilFinishedCrawling(): Unit = commandQueue.synchronized {
    while (!commandQueue.isEmpty) commandQueue.wait()
  }
}


private sealed trait CrawlerCommand

private final case class CrawlUrl(url: URL, allowSubdomains: Boolean, forceReCrawl: Boolean) extends CrawlerCommand

private case object CloseCrawler extends CrawlerCommand // closes crawler thread
private case object StopCrawler extends CrawlerCommand  // clears crawler queue

