package app.auriel.basalt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.auriel.basalt.ui.BasaltApp

class BasaltActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { BasaltApp(startRoute = intent?.getStringExtra(EXTRA_ROUTE)) }
    }

    private companion object {
        /**
         * Which section a widget wants shown.
         *
         * A plain string extra rather than a deep link, because the caller is
         * `:widget` and the two modules must not know each other's types —
         * `:app` already depends on `:widget`, so anything richer would be a
         * cycle. The constant is duplicated there under the same name; an
         * unrecognised value falls back to the start destination rather than
         * failing, which is the behaviour that survives the two drifting.
         */
        const val EXTRA_ROUTE = "app.auriel.basalt.widget.ROUTE"
    }
}
