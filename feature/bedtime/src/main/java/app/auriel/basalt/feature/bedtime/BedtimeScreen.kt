        package app.auriel.basalt.feature.bedtime

        import androidx.compose.foundation.layout.Arrangement
        import androidx.compose.foundation.layout.Column
        import androidx.compose.foundation.layout.fillMaxSize
        import androidx.compose.foundation.layout.padding
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.dp
        import app.auriel.basalt.core.design.LocalBasaltColors
        import app.auriel.basalt.core.dotmatrix.DotMatrixText

        /**
         * Sleep window. Basalt's own addition, not an AOSP port. Placeholder in
v0.1.0.
         */
        @Composable
        fun BedtimeScreen(modifier: Modifier = Modifier) {
            val colors = LocalBasaltColors.current
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DotMatrixText(
                    text = "BEDTIME",
                    cellSize = 4f,
                    litColor = colors.silver,
                    unlitColor = colors.unlit,
                )
                DotMatrixText(
                    text = "NOT WIRED UP",
                    cellSize = 2f,
                    litColor = colors.pewter,
                    unlitColor = colors.unlit,
                )
            }
        }
