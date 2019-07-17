package mozilla.lockbox.adapter

import android.content.Context
import android.opengl.Visibility
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.log

class ItemDetailAdapter(
    context: Context,
    textViewResourceId: Int,
    val values: ArrayList<ItemDetailAction.EditItemMenu>
) : ArrayAdapter<ItemDetailAction.EditItemMenu>(context, textViewResourceId, values) {

    private var selectedIndex = -1

    fun setSelection(position: Int) {
        selectedIndex = position
        setNotifyOnChange(true)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView

        label.setTextAppearance(R.style.TextAppearanceWidgetEventToolbarTitle)
        label.setTextColor(label.resources.getColor(R.color.text_white, null))
        label.setBackgroundColor(label.resources.getColor(R.color.color_primary, null))
        label.text = context.resources.getString(values[position].titleId)

        label.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_menu_kebab, 0)

        return label
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (position == 0) {
            val textView = TextView(context)
            return textView
        } else {
            val label = super.getDropDownView(position, convertView, parent) as TextView

            label.setTextAppearance(R.style.TextAppearanceSortMenuItem)
            val title = context.resources.getString(values[position].titleId)
            label.text = title
            label.background = context.resources.getDrawable(R.drawable.button_pressed_white, null)
            val padding = label.resources.getDimensionPixelSize(R.dimen.sort_item_padding)
            label.setPadding(padding, padding, padding, padding)

            return label
        }

    }
}