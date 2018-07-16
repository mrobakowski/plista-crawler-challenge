package com.mrobakowski.plistachallenge

import java.net.URL

import com.typesafe.scalalogging.StrictLogging

import scala.collection.mutable

class CrawlerTest extends CrawlerSpec with StrictLogging {
  "Sanity" should "be preserved" in {
    assert(true)
  }

  "Crawler" should "add given page to the index" in {
    val pages = mutable.Map[URL, Page]()

    val c = new Crawler(new PageIndex {
      override def add(page: Page): Unit = pages += (page.url -> page)

      override def has(url: URL): Boolean = pages.contains(url)

      override def search(query: String): Seq[Page] = ???

      override def clear(): Unit = ???

      override def size(): Long = ???
    })

    val url = new URL("https://en.wikipedia.org/wiki/Scala_(programming_language)")

    c.queueCrawl(url)
    Thread.sleep(100)
    c.close()

    pages.keys should contain(url)

    pages(url).title should equal("Scala (programming language) - Wikipedia")
  }

  // in the real world network IO should be abstracted away and mocked, since this test takes a looooong time
  "Crawler" should "crawl all the pages recursively" ignore {
    val pages = mutable.Map[URL, Page]()

    val c = new Crawler(new PageIndex {
      override def add(page: Page): Unit = pages += (page.url -> page)

      override def has(url: URL): Boolean = pages.contains(url)

      override def search(query: String): Seq[Page] = ???

      override def clear(): Unit = ???

      override def size(): Long = ???
    })

    val url = new URL("https://www.plista.com/")

    c.queueCrawl(url)
    c.waitUntilFinishedCrawling()
    Unit
  }

  "SimplePageIndex" should "be able to search pages it contains by regex" in {
    val index = new SimplePageIndex()
    val urls = List(
      new URL("http://p0"),
      new URL("http://p1"),
      new URL("http://p2"),
      new URL("http://p3"),
      new URL("http://p4")
    )

    index.add(Page(urls(0), "bar", "bar"))
    index.add(Page(urls(1), "baz", "baz"))
    index.add(Page(urls(2), "foo", "foo"))
    index.add(Page(urls(3), "qux", "qux"))
    index.add(Page(urls(4), "quux", "quux"))

    index.search("ba.").toSet should equal(Set(urls(0), urls(1)))
    index.search("q.*x").toSet should equal(Set(urls(3), urls(4)))
    index.search("foo").toSet should equal(Set(urls(2)))
  }
}
