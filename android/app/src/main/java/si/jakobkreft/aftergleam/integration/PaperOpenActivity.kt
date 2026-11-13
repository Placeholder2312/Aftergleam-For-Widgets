package si.jakobkreft.aftergleam.integration
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import si.jakobkreft.aftergleam.MainActivity
import si.jakobkreft.aftergleam.data.Db
import si.jakobkreft.aftergleam.work.Notifications

/** The sole external paper-opening endpoint; its manifest entry requires the signature permission. */
class PaperOpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(ResearchWidgetsContract.EXTRA_PAPER_ID)
        if (intent.action == ResearchWidgetsContract.ACTION_OPEN_PAPER && isValidPaperId(id) &&
            runCatching { Db(applicationContext).papersById(listOf(id!!)).isNotEmpty() }.getOrDefault(false)
        ) {
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra(Notifications.EXTRA_PAPER, id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        finish()
    }
    companion object {
        /** Bound lookup value only: not SQL, a URI, a URL or an arbitrary command. */
        fun isValidPaperId(value: String?): Boolean = value != null && value.isNotBlank() &&
            value == value.trim() && value.length <= 200 && value.none { it.isISOControl() }
    }
}
