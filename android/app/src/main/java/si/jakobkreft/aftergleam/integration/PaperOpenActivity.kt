package si.jakobkreft.aftergleam.integration
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import si.jakobkreft.aftergleam.MainActivity
import si.jakobkreft.aftergleam.data.Db
import si.jakobkreft.aftergleam.ui.LibraryShelf
import si.jakobkreft.aftergleam.work.Notifications

/**
 * The sole external entry point for the companion widgets: it opens one paper, or the Library on one
 * shelf. Its manifest entry requires the signature permission.
 */
class PaperOpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.action) {
            ResearchWidgetsContract.ACTION_OPEN_PAPER -> openPaper()
            ResearchWidgetsContract.ACTION_OPEN_LIBRARY -> openLibrary()
        }
        finish()
    }

    private fun openPaper() {
        val id = intent.getStringExtra(ResearchWidgetsContract.EXTRA_PAPER_ID)
        if (isValidPaperId(id) &&
            runCatching { Db(applicationContext).papersById(listOf(id!!)).isNotEmpty() }.getOrDefault(false)
        ) {
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra(Notifications.EXTRA_PAPER, id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }

    private fun openLibrary() {
        val shelf = intent.getStringExtra(ResearchWidgetsContract.EXTRA_LIBRARY_SHELF)
        if (isValidShelf(shelf)) {
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra(Notifications.EXTRA_LIBRARY_SHELF, shelf)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }

    companion object {
        /** Bound lookup value only: not SQL, a URI, a URL or an arbitrary command. */
        fun isValidPaperId(value: String?): Boolean = value != null && value.isNotBlank() &&
            value == value.trim() && value.length <= 200 && value.none { it.isISOControl() }

        /** Only the three known shelf names are accepted; anything else is ignored. */
        fun isValidShelf(value: String?): Boolean = LibraryShelf.entries.any { it.action == value }
    }
}
