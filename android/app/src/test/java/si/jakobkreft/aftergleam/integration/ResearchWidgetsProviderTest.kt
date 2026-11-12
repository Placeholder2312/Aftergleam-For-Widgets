package si.jakobkreft.aftergleam.integration
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.pm.ProviderInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import si.jakobkreft.aftergleam.data.Db
import si.jakobkreft.aftergleam.data.Paper
import si.jakobkreft.aftergleam.data.Reaction
import si.jakobkreft.aftergleam.data.ShownItem
import si.jakobkreft.aftergleam.data.Signal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResearchWidgetsProviderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val today = LocalDate.now().toString()
    private fun paper(id: String) = Paper(id, "Title $id", "Private abstract must not be exposed", listOf("Ada Lovelace", "Grace Hopper"), listOf("cs.AI"), today, today)
    private fun provider(): ResearchWidgetsProvider {
        context.deleteDatabase("aftergleam.db")
        val value = ResearchWidgetsProvider()
        value.attachInfo(context, ProviderInfo().apply {
            authority = ResearchWidgetsContract.AUTHORITY
            exported = true
            readPermission = ResearchWidgetsContract.READ_PERMISSION
            writePermission = ResearchWidgetsContract.READ_PERMISSION
        })
        return value
    }
    @Test fun recommendedUsesStoredRankOrderAndOnlyWidgetFields() {
        val p = provider(); val db = Db(context)
        db.upsertPapers(listOf(paper("rank-1"), paper("rank-2")))
        db.markShown(listOf(ShownItem("rank-2", "RELEVANCE", "reason", 0.9f), ShownItem("rank-1", "RELEVANCE", "reason", 0.8f)), today)
        p.query(ResearchWidgetsContract.RECOMMENDED_URI, null, null, null, null).use {
            assertEquals(2, it.count); assertTrue(it.moveToFirst())
            assertEquals("rank-2", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_PAPER_ID)))
            assertEquals("Title rank-2", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_TITLE)))
            assertEquals("Ada Lovelace, Grace Hopper", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_AUTHORS)))
            assertEquals("recommended", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_FEED_TYPE)))
            assertEquals(0, it.getInt(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_FEED_POSITION)))
            assertFalse(it.columnNames.contains("abstract")); assertFalse(it.columnNames.contains("signals"))
        }
    }
    @Test fun savedUsesCurrentSavedFlagAndMostRecentSaveTimestamp() {
        val p = provider(); val db = Db(context); db.upsertPapers(listOf(paper("old"), paper("new"), paper("not-saved")))
        listOf("old", "new").forEach { db.setReaction(it, Reaction(saved = true)); db.addSignal(it, Signal.SAVED) }
        db.writableDatabase.execSQL("UPDATE signals SET ts = 100 WHERE paper_id = 'old' AND signal = 'SAVED'")
        db.writableDatabase.execSQL("UPDATE signals SET ts = 200 WHERE paper_id = 'new' AND signal = 'SAVED'")
        p.query(ResearchWidgetsContract.SAVED_URI, null, null, null, null).use {
            assertEquals(2, it.count); assertTrue(it.moveToFirst())
            assertEquals("new", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_PAPER_ID)))
            assertTrue(it.moveToNext()); assertEquals("old", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_PAPER_ID)))
            assertEquals("saved", it.getString(it.getColumnIndexOrThrow(ResearchWidgetsContract.COLUMN_FEED_TYPE)))
        }
    }
    @Test fun emptyFeedsReturnEmptyCursors() {
        val p = provider()
        assertEquals(0, p.query(ResearchWidgetsContract.RECOMMENDED_URI, null, null, null, null).use { it.count })
        assertEquals(0, p.query(ResearchWidgetsContract.SAVED_URI, null, null, null, null).use { it.count })
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownFeed() {
        provider().query(Uri.parse("content://${ResearchWidgetsContract.AUTHORITY}/v1/private"), null, null, null, null)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsCallerSelection() {
        provider().query(ResearchWidgetsContract.SAVED_URI, null, "1=1", null, null)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownProjection() {
        provider().query(ResearchWidgetsContract.SAVED_URI, arrayOf("abstract"), null, null, null)
    }
    @Test(expected = UnsupportedOperationException::class) fun rejectsWrites() {
        provider().insert(ResearchWidgetsContract.SAVED_URI, ContentValues())
    }
    @Test fun paperOpenIdIsBoundedAndRejectsControlCharacters() {
        assertTrue(PaperOpenActivity.isValidPaperId("10.1101/2026.11.12.123456"))
        assertTrue(PaperOpenActivity.isValidPaperId("osf:4vpd7_v1"))
        assertFalse(PaperOpenActivity.isValidPaperId(null)); assertFalse(PaperOpenActivity.isValidPaperId(" paper-id "))
        assertFalse(PaperOpenActivity.isValidPaperId("paper\n-id")); assertFalse(PaperOpenActivity.isValidPaperId("x".repeat(201)))
    }
    @Test fun providerDeclaresSignatureReadPermission() {
        val info = context.packageManager.resolveContentProvider(ResearchWidgetsContract.AUTHORITY, PackageManager.GET_META_DATA)
        assertTrue(info != null); assertEquals(ResearchWidgetsContract.READ_PERMISSION, info!!.readPermission)
        assertEquals(ResearchWidgetsContract.READ_PERMISSION, info.writePermission)
        val permission = context.packageManager.getPermissionInfo(ResearchWidgetsContract.READ_PERMISSION, 0)
        assertTrue(permission.protectionLevel and PermissionInfo.PROTECTION_SIGNATURE != 0)
    }
}
