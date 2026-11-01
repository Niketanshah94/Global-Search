package com.quickfind.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickfind.core.data.db.FileEntity
import com.quickfind.core.data.db.FileOrigin
import com.quickfind.core.data.db.QueryHistoryEntity
import com.quickfind.core.data.db.QuickFindDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileDaoTest {
    private lateinit var db: QuickFindDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QuickFindDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    private fun file(key: String, name: String, tokens: String, path: String = "Download/", scan: Long = 0) = FileEntity(
        sourceKey = key, displayName = name, nameTokens = tokens, relativePath = path, mimeType = "application/pdf",
        size = 1, dateModified = 0, isFolder = false, contentUri = "content://x/$key", origin = FileOrigin.WALK, scanId = scan,
    )

    @Test
    fun ftsPrefixSearchFindsTokens() = runTest {
        val dao = db.fileDao()
        dao.upsertAll(listOf(file("a", "ResumeFinal_v2.pdf", "resume final v 2 pdf resumefinalv2pdf"), file("b", "notes.txt", "notes txt")))
        val hits = dao.ftsSearch("fin*", 10)
        assertEquals(listOf("ResumeFinal_v2.pdf"), hits.map { it.displayName })
        assertEquals(1, dao.ftsSearch("resume* fin*", 10).size)
        assertTrue(dao.ftsSearch("zzz*", 10).isEmpty())
    }

    @Test
    fun replaceOnSourceKeyKeepsFtsInSync() = runTest {
        val dao = db.fileDao()
        dao.upsertAll(listOf(file("a", "old.pdf", "old pdf")))
        dao.upsertAll(listOf(file("a", "new.pdf", "new pdf")))
        assertTrue(dao.ftsSearch("old*", 10).isEmpty())
        assertEquals(1, dao.ftsSearch("new*", 10).size)
    }

    @Test
    fun staleWalkRowsAreDeleted() = runTest {
        val dao = db.fileDao()
        dao.upsertAll(listOf(file("a", "a.pdf", "a pdf", scan = 1), file("b", "b.pdf", "b pdf", scan = 2)))
        dao.deleteStale(FileOrigin.WALK, 2)
        assertEquals(listOf("b.pdf"), dao.substringSearch("pdf", 10).map { it.displayName })
    }

    @Test
    fun historyRoundTrip() = runTest {
        val dao = db.historyDao()
        dao.insertPick(QueryHistoryEntity(query = "wh", resultId = "app:whatsapp", timestamp = 2))
        dao.insertPick(QueryHistoryEntity(query = "ca", resultId = "app:camera", timestamp = 1))
        assertEquals(listOf("app:whatsapp", "app:camera"), dao.recentPicks(10).map { it.resultId })
        dao.prunePicks(2)
        assertEquals(1, dao.recentPicks(10).size)
    }
}
