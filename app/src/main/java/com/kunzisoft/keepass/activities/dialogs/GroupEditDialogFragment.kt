/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.ImageView
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.CREATION
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.UPDATE
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.GroupVersioned
import com.kunzisoft.keepass.database.element.PwIcon

class GroupEditDialogFragment : DialogFragment(), IconPickerDialogFragment.IconPickerListener {

    private var mDatabase: Database? = null

    private var editGroupListener: EditGroupListener? = null

    private var editGroupDialogAction: EditGroupDialogAction? = null
    private var nameGroup: String? = null
    private var iconGroup: PwIcon? = null

    private var iconButton: ImageView? = null
    private var iconColor: Int = 0

    enum class EditGroupDialogAction {
        CREATION, UPDATE, NONE;

        companion object {
            fun getActionFromOrdinal(ordinal: Int): EditGroupDialogAction {
                return values()[ordinal]
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            editGroupListener = context as EditGroupListener?
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context?.toString()
                    + " must implement " + GroupEditDialogFragment::class.java.name)
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.group_edit, null)
            val nameField = root?.findViewById<TextView>(R.id.group_edit_name)
            iconButton = root?.findViewById(R.id.group_edit_icon_button)

            // Retrieve the textColor to tint the icon
            val ta = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
            iconColor = ta.getColor(0, Color.WHITE)
            ta.recycle()

            // Init elements
            mDatabase = App.currentDatabase
            editGroupDialogAction = EditGroupDialogAction.NONE
            nameGroup = ""
            iconGroup = mDatabase?.iconFactory?.folderIcon

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(KEY_ACTION_ID)
                    && savedInstanceState.containsKey(KEY_NAME)
                    && savedInstanceState.containsKey(KEY_ICON)) {
                editGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(savedInstanceState.getInt(KEY_ACTION_ID))
                nameGroup = savedInstanceState.getString(KEY_NAME)
                iconGroup = savedInstanceState.getParcelable(KEY_ICON)

            } else {
                arguments?.apply {
                    if (containsKey(KEY_ACTION_ID))
                        editGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(getInt(KEY_ACTION_ID))

                    if (containsKey(KEY_NAME) && containsKey(KEY_ICON)) {
                        nameGroup = getString(KEY_NAME)
                        iconGroup = getParcelable(KEY_ICON)
                    }
                }
            }

            // populate the name
            nameField?.text = nameGroup
            // populate the icon
            assignIconView()

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        editGroupListener?.approveEditGroup(
                                editGroupDialogAction,
                                nameField?.text.toString(),
                                iconGroup)

                        this@GroupEditDialogFragment.dialog.cancel()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        editGroupListener?.cancelEditGroup(
                                editGroupDialogAction,
                                nameField?.text.toString(),
                                iconGroup)

                        this@GroupEditDialogFragment.dialog.cancel()
                    }

            iconButton?.setOnClickListener { _ ->
                fragmentManager?.let {
                    IconPickerDialogFragment().show(it, "IconPickerDialogFragment")
                }
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun assignIconView() {
        mDatabase?.drawFactory
                ?.assignDatabaseIconTo(
                        context,
                        iconButton,
                        iconGroup,
                        iconColor)
    }

    override fun iconPicked(bundle: Bundle) {
        iconGroup = bundle.getParcelable(IconPickerDialogFragment.KEY_ICON_STANDARD)
        assignIconView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_ACTION_ID, editGroupDialogAction!!.ordinal)
        outState.putString(KEY_NAME, nameGroup)
        outState.putParcelable(KEY_ICON, iconGroup)
        super.onSaveInstanceState(outState)
    }

    interface EditGroupListener {
        fun approveEditGroup(action: EditGroupDialogAction?, name: String?, icon: PwIcon?)
        fun cancelEditGroup(action: EditGroupDialogAction?, name: String?, icon: PwIcon?)
    }

    companion object {

        const val TAG_CREATE_GROUP = "TAG_CREATE_GROUP"

        const val KEY_NAME = "KEY_NAME"
        const val KEY_ICON = "KEY_ICON"
        const val KEY_ACTION_ID = "KEY_ACTION_ID"

        fun build(): GroupEditDialogFragment {
            val bundle = Bundle()
            bundle.putInt(KEY_ACTION_ID, CREATION.ordinal)
            val fragment = GroupEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }

        fun build(group: GroupVersioned): GroupEditDialogFragment {
            val bundle = Bundle()
            bundle.putString(KEY_NAME, group.title)
            bundle.putParcelable(KEY_ICON, group.icon)
            bundle.putInt(KEY_ACTION_ID, UPDATE.ordinal)
            val fragment = GroupEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
