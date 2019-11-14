package com.breadwallet.ui.settings.nodeselector

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.tools.util.TrustedNode
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_nodes.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.erased.instance

private const val DIALOG_TITLE_PADDING = 16
private const val DIALOG_TITLE_TEXT_SIZE = 18f
private const val DIALOG_INPUT_PADDING = 24

class NodeSelectorController :
    BaseMobiusController<NodeSelectorModel, NodeSelectorEvent, NodeSelectorEffect>() {

    override val layoutId = R.layout.activity_nodes
    override val defaultModel = NodeSelectorModel.createDefault()
    override val update = NodeSelectorUpdate
    override val init = NodeSelectorInit

    override val effectHandler = CompositeEffectHandler.from<NodeSelectorEffect, NodeSelectorEvent>(
        Connectable { output ->
            NodeSelectorEffectHandler(output, direct.instance(), ::showNodeDialog)
        })

    override fun bindView(output: Consumer<NodeSelectorEvent>) = output.view {
        button_switch.onClick(NodeSelectorEvent.OnSwitchButtonClicked)
    }

    override fun NodeSelectorModel.render() {
        val res = checkNotNull(resources)
        ifChanged(NodeSelectorModel::mode) {
            button_switch.text = when (mode) {
                NodeSelectorModel.Mode.AUTOMATIC -> res.getString(R.string.NodeSelector_manualButton)
                NodeSelectorModel.Mode.MANUAL -> res.getString(R.string.NodeSelector_automaticButton)
                else -> ""
            }
        }

        ifChanged(NodeSelectorModel::currentNode) {
            node_text.text = if (currentNode.isNotBlank()) {
                currentNode
            } else {
                res.getString(R.string.NodeSelector_automatic)
            }
        }

        ifChanged(NodeSelectorModel::connected) {
            node_status.text = if (connected) {
                res.getString(R.string.NodeSelector_connected)
            } else {
                res.getString(R.string.NodeSelector_notConnected)
            }
        }
    }

    private fun showNodeDialog() {
        val res = checkNotNull(resources)
        val alertDialog = AlertDialog.Builder(activity)
        val customTitle = TextView(activity)

        customTitle.gravity = Gravity.CENTER
        customTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val pad16 = Utils.getPixelsFromDps(activity, DIALOG_TITLE_PADDING)
        customTitle.setPadding(pad16, pad16, pad16, pad16)
        customTitle.text = res.getString(R.string.NodeSelector_enterTitle)
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, DIALOG_TITLE_TEXT_SIZE)
        customTitle.setTypeface(null, Typeface.BOLD)
        alertDialog.setCustomTitle(customTitle)
        alertDialog.setMessage(res.getString(R.string.NodeSelector_enterBody))

        val input = EditText(activity)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val padding = Utils.getPixelsFromDps(activity, DIALOG_INPUT_PADDING)

        input.setPadding(padding, 0, padding, padding)
        input.layoutParams = lp
        alertDialog.setView(input)

        alertDialog.setNegativeButton(
            res.getString(R.string.Button_cancel)
        ) { dialog, _ -> dialog.cancel() }

        alertDialog.setPositiveButton(
            res.getString(R.string.Button_ok)
        ) { _, _ ->
            // this implementation will be overridden
        }

        val dialog = alertDialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val node = input.text.toString()
            if (TrustedNode.isValid(node)) {
                eventConsumer.accept(NodeSelectorEvent.SetCustomNode(node))
            } else {
                viewAttachScope.launch(Dispatchers.Main) {
                    customTitle.setText(R.string.NodeSelector_invalid)
                    customTitle.setTextColor(res.getColor(R.color.warning_color))
                    delay(1_000L)
                    customTitle.setText(R.string.NodeSelector_enterTitle)
                    customTitle.setTextColor(res.getColor(R.color.almost_black))
                }
            }
        }
        viewAttachScope.launch(Dispatchers.Main) {
            delay(200L)
            input.requestFocus()
            val keyboard =
                activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.showSoftInput(input, 0)
        }
    }
}