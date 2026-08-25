        package app.auriel.basalt.feature.settings

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
         * Settings. Every control here is bespoke — there is no Material to
borrow switches or sliders from — so this stays a placeholder until
the design system has them.
         */
        @Composable
        fun SettingsScreen(modifier: Modifier = Modifier) {
            val colors = LocalBasaltColors.current
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DotMatrixText(
                    text = "SETTINGS",
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
