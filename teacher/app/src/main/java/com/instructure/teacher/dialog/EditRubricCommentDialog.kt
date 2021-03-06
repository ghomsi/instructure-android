/*
* Copyright (C) 2017 - present Instructure, Inc.
*
*     This program is free software: you can redistribute it and/or modify
*     it under the terms of the GNU General Public License as published by
*     the Free Software Foundation, version 3 of the License.
*
*     This program is distributed in the hope that it will be useful,
*     but WITHOUT ANY WARRANTY; without even the implied warranty of
*     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*     GNU General Public License for more details.
*
*     You should have received a copy of the GNU General Public License
*     along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.instructure.teacher.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import android.view.WindowManager
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.onClick
import com.instructure.teacher.R
import com.instructure.pandautils.utils.BooleanArg
import com.instructure.pandautils.utils.LongArg
import com.instructure.pandautils.utils.StringArg
import com.instructure.pandautils.utils.dismissExisting
import com.instructure.teacher.view.edit_rubric.RubricCommentEditedEvent
import kotlinx.android.synthetic.main.view_edit_grade_comment.*
import org.greenrobot.eventbus.EventBus


class EditRubricCommentDialog : AppCompatDialogFragment() {

    var mCriterionId by StringArg()
    var mStudentId by LongArg(-1L)
    var mAssigneeName by StringArg()
    var mDefaultString by StringArg()
    var mGradeAnonymously by BooleanArg()

    init {
        retainInstance = true
    }

    companion object {
        @JvmStatic
        fun show(
                manager: FragmentManager,
                criterionId: String,
                studentId: Long,
                assigneeName: String,
                gradeAnonymously: Boolean,
                defaultString: String = ""
        ) = EditRubricCommentDialog().apply {
            manager.dismissExisting<EditRubricCommentDialog>()
            mCriterionId = criterionId
            mStudentId = studentId
            mAssigneeName = assigneeName
            mDefaultString = defaultString
            mGradeAnonymously = gradeAnonymously
            show(manager, javaClass.simpleName)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = AppCompatDialog(context, R.style.Theme_AppCompat_Light_Translucent).apply {
        setContentView(R.layout.view_edit_grade_comment)

        // Send event bus on save, dismiss dialog. Send null if text is blank (i.e. delete comment)
        saveCommentButton.onClick {
            val text = commentEditText.text.toString()
            EventBus.getDefault().post(RubricCommentEditedEvent(mCriterionId, if (text.isBlank()) null else text, mStudentId))
            dismiss()
        }

        ViewStyler.themeButton(dismissEditCommentButton)
        // Dismiss on outside click
        dismissEditCommentButton.onClick { dismiss() }

        // Set up EditText
        if (!mGradeAnonymously) commentEditText.hint = getString(R.string.sendMessageToHint, mAssigneeName)
        commentEditText.highlightColor = ThemePrefs.increaseAlpha(ThemePrefs.brandColor)
        commentEditText.setText(mDefaultString)

        // Theme save button
        DrawableCompat.setTint(saveCommentButton.drawable, ThemePrefs.buttonColor)

        // Style the dialog
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(activity, com.instructure.pandautils.R.color.dim_lighter_gray)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    override fun onDestroyView() {
        // Fix for rotation bug
        dialog?.let { if (retainInstance) it.setDismissMessage(null) }
        super.onDestroyView()
    }
}
