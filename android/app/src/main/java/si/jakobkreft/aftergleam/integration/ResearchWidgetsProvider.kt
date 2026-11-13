package si.jakobkreft.aftergleam.integration
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import si.jakobkreft.aftergleam.data.Db
import si.jakobkreft.aftergleam.data.Paper
import si.jakobkreft.aftergleam.data.Signal
import java.time.LocalDate

/** Read-only widget projection. Android enforces the signature permission from the manifest. */
class ResearchWidgetsProvider : ContentProvider() {
    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(ResearchWidgetsContract.AUTHORITY, ResearchWidgetsContract.PATH_RECOMMENDED, RECOMMENDED)
        addURI(ResearchWidgetsContract.AUTHORITY, ResearchWidgetsContract.PATH_SAVED, SAVED)
    }
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        require(uri.scheme == "content" && uri.query == null && uri.fragment == null) { "Unsupported widget URI" }
        val feed = matcher.match(uri)
        require(feed == RECOMMENDED || feed == SAVED) { "Unknown widget feed" }
        require(selection.isNullOrBlank() && selectionArgs.isNullOrEmpty() && sortOrder.isNullOrBlank()) { "Filtering and custom sorting are unsupported" }
        val columns = (projection?.toList() ?: ResearchWidgetsContract.COLUMNS.toList()).toTypedArray()
        require(columns.isNotEmpty() && columns.distinct().size == columns.size && columns.all { it in ResearchWidgetsContract.COLUMNS }) { "Unsupported widget projection" }
        val app = requireNotNull(context) { "Provider is not attached" }
        val rows = if (feed == RECOMMENDED) recommended(Db(app)) else saved(Db(app))
        val cursor = MatrixCursor(columns, rows.size)
        rows.forEachIndexed { index, paper ->
            val values = mapOf(
                ResearchWidgetsContract.COLUMN_PAPER_ID to paper.id,
                ResearchWidgetsContract.COLUMN_TITLE to paper.title,
                ResearchWidgetsContract.COLUMN_AUTHORS to paper.authors,
                ResearchWidgetsContract.COLUMN_SOURCE to paper.source,
                ResearchWidgetsContract.COLUMN_PUBLISHED to paper.published,
                ResearchWidgetsContract.COLUMN_FEED_POSITION to index,
                ResearchWidgetsContract.COLUMN_FEED_TYPE to if (feed == RECOMMENDED) "recommended" else "saved",
            )
            cursor.addRow(columns.map { values[it] })
        }
        cursor.setNotificationUri(app.contentResolver, uri)
        return cursor
    }

    private data class WidgetPaper(val id: String, val title: String, val authors: String, val source: String, val published: String)
    private fun recommended(db: Db): List<WidgetPaper> {
        // digestFor is the exact saved output order of the existing ranking algorithm.
        val ids = db.digestFor(LocalDate.now().toString()).map { it.paperId }
        val byId = db.papersById(ids).associateBy(Paper::id)
        return ids.distinct().mapNotNull { byId[it]?.toWidgetPaper() }
    }
    private fun saved(db: Db): List<WidgetPaper> {
        val ids = db.allReactions().filterValues { it.saved }.keys
        val times = db.signalTimes(Signal.SAVED)
        val ordered = ids.sortedWith(compareByDescending<String> { times[it] ?: Long.MIN_VALUE }.thenBy { it })
        val byId = db.papersById(ordered).associateBy(Paper::id)
        return ordered.mapNotNull { byId[it]?.toWidgetPaper() }
    }
    private fun Paper.toWidgetPaper() = WidgetPaper(id, displayTitle, authors.joinToString(", "), sourceLabel ?: "arXiv", published)
    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        RECOMMENDED, SAVED -> "vnd.android.cursor.dir/vnd.${ResearchWidgetsContract.AUTHORITY}.paper"
        else -> throw IllegalArgumentException("Unknown widget feed")
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("Read-only provider")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read-only provider")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read-only provider")
    companion object { private const val RECOMMENDED = 1; private const val SAVED = 2 }
}
