package si.jakobkreft.aftergleam.integration
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Public constants shared by the independently versioned provider and companion. */
object ResearchWidgetsContract {
    const val AUTHORITY = "si.jakobkreft.aftergleam.researchwidgets"
    const val READ_PERMISSION = "si.jakobkreft.aftergleam.permission.READ_RESEARCH_WIDGETS"
    const val COMPANION_PACKAGE = "com.placeholder2312.researchwidgets"
    const val PATH_RECOMMENDED = "v1/recommended"
    const val PATH_SAVED = "v1/saved"
    const val PATH_DOWNLOADED = "v1/downloaded"
    const val COLUMN_PAPER_ID = "paper_id"
    const val COLUMN_TITLE = "title"
    const val COLUMN_AUTHORS = "authors"
    const val COLUMN_SOURCE = "source"
    const val COLUMN_PUBLISHED = "published"
    const val COLUMN_FEED_POSITION = "feed_position"
    const val COLUMN_FEED_TYPE = "feed_type"
    val COLUMNS = arrayOf(COLUMN_PAPER_ID, COLUMN_TITLE, COLUMN_AUTHORS, COLUMN_SOURCE, COLUMN_PUBLISHED, COLUMN_FEED_POSITION, COLUMN_FEED_TYPE)
    val RECOMMENDED_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_RECOMMENDED")
    val SAVED_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_SAVED")
    val DOWNLOADED_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_DOWNLOADED")
    const val ACTION_OPEN_PAPER = "si.jakobkreft.aftergleam.action.OPEN_WIDGET_PAPER"
    const val EXTRA_PAPER_ID = "si.jakobkreft.aftergleam.extra.WIDGET_PAPER_ID"
    const val ACTION_DATA_CHANGED = "si.jakobkreft.aftergleam.action.RESEARCH_WIDGETS_DATA_CHANGED"
    const val EXTRA_REVISION = "si.jakobkreft.aftergleam.extra.WIDGET_DATA_REVISION"
}

/** Notify observers and the one intended companion after a successful data write. */
object ResearchWidgetsNotifier {
    @Volatile private var revision = 0L
    fun notifyDataChanged(context: Context) {
        val app = context.applicationContext
        app.contentResolver.notifyChange(ResearchWidgetsContract.RECOMMENDED_URI, null)
        app.contentResolver.notifyChange(ResearchWidgetsContract.SAVED_URI, null)
        app.contentResolver.notifyChange(ResearchWidgetsContract.DOWNLOADED_URI, null)
        val next = synchronized(this) { revision = maxOf(System.currentTimeMillis(), revision + 1L); revision }
        val intent = Intent(ResearchWidgetsContract.ACTION_DATA_CHANGED)
            .setPackage(ResearchWidgetsContract.COMPANION_PACKAGE)
            .putExtra(ResearchWidgetsContract.EXTRA_REVISION, next)
        // Receiver permission limits delivery to an installed app holding the same signature permission.
        app.sendBroadcast(intent, ResearchWidgetsContract.READ_PERMISSION)
    }
}
