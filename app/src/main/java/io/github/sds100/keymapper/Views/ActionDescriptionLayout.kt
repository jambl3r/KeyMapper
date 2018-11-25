package io.github.sds100.keymapper.Views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import io.github.sds100.keymapper.ActionDescription
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.layout_action_description.view.*

/**
 * Created by sds100 on 15/10/2018.
 */

class ActionDescriptionLayout(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        View.inflate(context, R.layout.layout_action_description, this)

        /* on pre-lollipop devices, vector drawables can't be used with drawableStart,
         * drawableEnd etc. otherwise the app crashes. */
        val errorDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_error_red_24dp)

        textViewError.setCompoundDrawablesWithIntrinsicBounds(
                errorDrawable,
                null,
                null,
                null
        )
    }

    fun setDescription(description: ActionDescription) {
        description.apply {
            textViewTitle.text = title
            imageViewAction.setImageDrawable(iconDrawable)
            textViewError.text = errorDescription

            imageViewAction.setVisible(iconDrawable != null)
            textViewTitle.setVisible(title != null)
            textViewError.setVisible(errorCodeResult != null)
        }
    }

    private fun View.setVisible(visible: Boolean) {
        if (visible) {
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }
}