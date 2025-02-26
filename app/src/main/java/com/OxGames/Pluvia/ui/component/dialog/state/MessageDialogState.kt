package com.OxGames.Pluvia.ui.component.dialog.state

import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.graphics.vector.ImageVector
import com.OxGames.Pluvia.ui.enums.DialogType

data class MessageDialogState(
    val visible: Boolean,
    val type: DialogType = DialogType.NONE,
    val confirmBtnText: String = "Confirm",
    val dismissBtnText: String = "Dismiss",
    val icon: ImageVector? = null,
    val title: String? = null,
    val message: String? = null,
) {
    companion object {
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "visible" to state.visible,
                    "type" to state.type,
                    "confirmBtnText" to state.confirmBtnText,
                    "dismissBtnText" to state.dismissBtnText,
                    // this will probably break once a dialog is used with an icon
                    "icon" to state.icon,
                    "title" to state.title,
                    "message" to state.message,
                )
            },
            restore = { savedMap ->
                MessageDialogState(
                    visible = savedMap["visible"] as Boolean,
                    type = savedMap["type"] as DialogType,
                    confirmBtnText = savedMap["confirmBtnText"] as String,
                    dismissBtnText = savedMap["dismissBtnText"] as String,
                    icon = savedMap["icon"] as ImageVector?,
                    title = savedMap["title"] as String?,
                    message = savedMap["message"] as String?,
                )
            },
        )
    }
}
