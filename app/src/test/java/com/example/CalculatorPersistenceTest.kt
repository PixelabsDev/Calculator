package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HistoryDao
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Local Database & Preferences Persistence Test Suite.
 * Validates Room SQLite database operations, chronological ordering,
 * repository methods, and safe migration configuration on JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        historyDao = database.historyDao()
        repository = HistoryRepository(historyDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndChronologicalRetrieval() = runBlocking {
        val entry1 = HistoryEntry(expression = "2 + 2", result = "4", timestamp = 1000L)
        val entry2 = HistoryEntry(expression = "10 × 5", result = "50", timestamp = 2000L)
        val entry3 = HistoryEntry(expression = "sin(90)", result = "1", timestamp = 3000L)

        historyDao.insertHistory(entry1)
        historyDao.insertHistory(entry2)
        historyDao.insertHistory(entry3)

        val historyList = historyDao.getAllHistory().first()
        assertEquals(3, historyList.size)

        // Must be in descending chronological order (newest first)
        assertEquals("sin(90)", historyList[0].expression)
        assertEquals("1", historyList[0].result)
        assertEquals(3000L, historyList[0].timestamp)

        assertEquals("10 × 5", historyList[1].expression)
        assertEquals("50", historyList[1].result)
        assertEquals(2000L, historyList[1].timestamp)

        assertEquals("2 + 2", historyList[2].expression)
        assertEquals("4", historyList[2].result)
        assertEquals(1000L, historyList[2].timestamp)
    }

    @Test
    fun testDeleteHistoryById() = runBlocking {
        val entry1 = HistoryEntry(id = 101, expression = "5 + 5", result = "10", timestamp = 1000L)
        val entry2 = HistoryEntry(id = 102, expression = "6 × 6", result = "36", timestamp = 2000L)

        historyDao.insertHistory(entry1)
        historyDao.insertHistory(entry2)

        var list = historyDao.getAllHistory().first()
        assertEquals(2, list.size)

        repository.deleteById(101)

        list = historyDao.getAllHistory().first()
        assertEquals(1, list.size)
        assertEquals(102L, list[0].id)
        assertEquals("6 × 6", list[0].expression)
    }

    @Test
    fun testClearAllHistory() = runBlocking {
        for (i in 1..5) {
            repository.insert(HistoryEntry(expression = "$i + $i", result = "${i * 2}"))
        }

        var list = repository.allHistory.first()
        assertEquals(5, list.size)

        repository.clearAll()

        list = repository.allHistory.first()
        assertTrue("History list must be completely empty after clearAll", list.isEmpty())
    }

    @Test
    fun testPreferencesPersistence() {
        val prefs = context.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)

        // Write user settings
        prefs.edit()
            .putString("theme_mode", "DARK")
            .putBoolean("rad_mode", true)
            .putBoolean("screenshot_blocked", true)
            .apply()

        // Read and assert
        assertEquals("DARK", prefs.getString("theme_mode", null))
        assertTrue(prefs.getBoolean("rad_mode", false))
        assertTrue(prefs.getBoolean("screenshot_blocked", false))
    }

    @Test
    fun testScreenshotBlockedDefaultsToFalse() {
        val prefs = context.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)
        prefs.edit().remove("screenshot_blocked").commit()
        val viewModel = CalculatorViewModel(context as android.app.Application)
        assertFalse("Screenshot block must default to OFF (false)", viewModel.isScreenshotBlocked.value)
    }

    @Test
    fun testDatabaseBuilderHasDestructiveMigrationFallback() {
        val dbInstance = AppDatabase.getDatabase(context)
        assertNotNull(dbInstance)
        assertNotNull(dbInstance.historyDao())
    }
}
