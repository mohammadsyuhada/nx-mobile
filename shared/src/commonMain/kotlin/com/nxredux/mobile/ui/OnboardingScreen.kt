package com.nxredux.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxDetailListBody
import com.nxredux.mobile.ui.nx.NxSetupLandscapeWidth
import com.nxredux.mobile.ui.nx.NxRowKind
import com.nxredux.mobile.ui.nx.NxSignInPrompt

/**
 * An onboarding step ([Screen.Onboarding]) in the tool-page shape: its title, the step count and its lines, its rows.
 * A or a tap on the selected row chooses it ([onRow]); B goes back, except on the steps that have nothing before them.
 * With [Screen.Onboarding.prompt] the RetroAchievements sign-in prompt is over it ([onDraftUser], [onDraftPassword]).
 */
@Composable
fun OnboardingScreen(
    screen: Screen.Onboarding, selected: Int, onSelect: (Int) -> Unit, onRow: (Int) -> Unit, onHint: (NxButton) -> Unit,
    onDraftUser: (String) -> Unit, onDraftPassword: (String) -> Unit, modifier: Modifier = Modifier,
) {
    // The last page: START finishes too, as Press Start does.
    val hints = listOfNotNull(
        if (screen.canGoBack) Hint(NxButton.B, "Back") else null, Hint(NxButton.A, "Choose"),
        if (screen.step == OnboardingStep.DONE) Hint(NxButton.START, "Start") else null,
    )
    if (screen.step == OnboardingStep.WELCOME) {
        OnboardingWelcomePage(screen, onRow, modifier)
        return
    }
    if (screen.step == OnboardingStep.TITLE) {
        val title = screen.rows.getOrNull(0)?.secondary != "Hide"
        val buttonHints = screen.rows.getOrNull(1)?.secondary != "Hide"
        LayoutPreviewPage(screen.title, screen.details, screen.rows, chromePicture(title, buttonHints), CHROME_PICTURE_AR, selected, hints, onSelect, onRow, onHint, modifier)
        return
    }
    // A layout page: the layout row above one picture of it (none for Minimal).
    screen.shot?.let { shot ->
        LayoutPreviewPage(screen.title, screen.details, screen.rows, layoutShotPicture(shot), LAYOUT_PICTURE_AR, selected, hints, onSelect, onRow, onHint, modifier)
        return
    }
    Box(modifier.fillMaxSize()) {
        NxDetailListBody(
            title = screen.title, details = screen.details, rows = screen.rows, selectedIndex = selected, hints = hints,
            onSelect = onSelect, onOpen = onRow, onHint = onHint,
            // The formats, the pad and its theme are value rows ("< value >" while selected), Continue a full row under them.
            rowKind = if (screen.step in VALUE_ROW_STEPS) NxRowKind.OPTIONS else NxRowKind.PILL,
            actionRows = if (screen.step in VALUE_ROW_STEPS) 1 else 0, actionsAtTop = screen.step in VALUE_ROW_STEPS,
            titleActions = if (titleAction(screen)) 1 else 0,   // the last row (Skip) at the title's right
            landscapeWidth = NxSetupLandscapeWidth,
        )
    }
    if (screen.prompt) NxSignInPrompt(
        screen.draftUser, screen.draftPassword, onDraftUser, onDraftPassword,
        onConfirm = { onHint(NxButton.A) }, onCancel = { onHint(NxButton.B) },
    )
}

/** Steps whose rows are value rows (or the accents) above a Continue. */
private val VALUE_ROW_STEPS = setOf(OnboardingStep.SAVE_FORMATS, OnboardingStep.PAD, OnboardingStep.ACCENT, OnboardingStep.TEXT_SIZE)

/** Steps of pill rows whose last row (Skip, or a lone Continue) sits at the title's right. */
private val TITLE_ACTION_STEPS = setOf(OnboardingStep.ARTWORK, OnboardingStep.RA_SIGN_IN, OnboardingStep.RA_DOWNLOAD)

/**
 * [screen]'s last row sits at the title's right: on [TITLE_ACTION_STEPS], and on Bring your saves while it offers the
 * imports (its Skip; an import's summary keeps Continue under the skipped files).
 */
private fun titleAction(screen: Screen.Onboarding): Boolean =
    screen.step in TITLE_ACTION_STEPS || (screen.step == OnboardingStep.IMPORT_SAVES && screen.rows.lastOrNull()?.label == "Skip")
