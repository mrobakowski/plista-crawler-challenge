package com.mrobakowski.plistachallenge

import java.net.URL

import scala.collection.concurrent.TrieMap
import scala.collection.concurrent.{Map => ConcurrentMap}
import scala.util.Try

trait PageIndex {
  def add(page: Page)

  def has(url: URL): Boolean

  def search(query: String): Seq[Page]

  def clear() // other ways of eviction could be also implemented, ex. removing the oldest, or least recently used entries
  def size(): Long
}

case class Page(url: URL, title: String, contents: String)

class SimplePageIndex extends PageIndex {
  val storage: ConcurrentMap[URL, Page] = TrieMap()

  override def add(page: Page): Unit = storage += (page.url -> page)

  override def has(url: URL): Boolean = storage.contains(url)

  override def search(query: String): Seq[Page] = {
    val compiledQuery = Try(query.r).getOrElse {
      println("invalid regex")
      return Seq()
    }
    storage.filter { case (_, v) => compiledQuery.findFirstMatchIn(v.contents).isDefined }.values.toSeq
  }

  override def clear(): Unit = storage.clear()

  override def size(): Long = storage.size
}

// Lucene-based PageIndex would also be a good idea, but when running fully in-memory, performance would drop after
// a few hundred megabytes. See https://lucene.apache.org/core/6_5_1/core/org/apache/lucene/store/RAMDirectory.html