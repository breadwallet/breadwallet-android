package com.breadwallet.ui.addwallets

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView

import com.breadwallet.R
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import com.breadwallet.tools.util.TokenUtil
import com.squareup.picasso.Picasso

import java.io.File

class AddTokenListAdapter(
    private val context: Context,
    private val addWalletListener: (token: Token) -> Unit
) : RecyclerView.Adapter<AddTokenListAdapter.TokenItemViewHolder>() {

    var tokens: List<Token> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: TokenItemViewHolder, position: Int) {
        val token = tokens[position]
        val currencyCode = token.currencyCode.toLowerCase()
        val tokenIconPath = TokenUtil.getTokenIconPath(context, currencyCode, true)

        val iconDrawable = holder.iconParent.background as GradientDrawable

        when {
            tokenIconPath.isNullOrEmpty() -> {
                // If no icon is present, then use the capital first letter of the token currency code instead.
                holder.iconLetter.visibility = View.VISIBLE
                iconDrawable.setColor(Color.parseColor(token.startColor))
                holder.iconLetter.text = currencyCode.substring(0, 1).toUpperCase()
                holder.logo.visibility = View.GONE
            }
            else -> {
                val iconFile = File(tokenIconPath)
                Picasso.get().load(iconFile).into(holder.logo)
                holder.iconLetter.visibility = View.GONE
                holder.logo.visibility = View.VISIBLE
                iconDrawable.setColor(Color.TRANSPARENT)
            }
        }

        holder.name.text = token.name
        holder.symbol.text = currencyCode

        val addWalletTypedValue = TypedValue()
        val removeWalletTypedValue = TypedValue()

        context.theme.apply {
            resolveAttribute(
                R.attr.add_wallet_button_background,
                addWalletTypedValue,
                true
            )
            resolveAttribute(
                R.attr.remove_wallet_button_background,
                removeWalletTypedValue,
                true
            )
        }

        holder.addRemoveButton.apply {
            text = context.getString(R.string.TokenList_add)
            background = context.getDrawable(addWalletTypedValue.resourceId)
            setTextColor(context.getColor(R.color.button_add_wallet_text))

            setOnClickListener {
                addWalletListener(tokens[holder.adapterPosition])
            }
        }
    }

    override fun getItemCount(): Int {
        return tokens.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TokenItemViewHolder {

        val inflater = (context as Activity).layoutInflater
        val convertView = inflater.inflate(R.layout.token_list_item, parent, false)

        val holder = TokenItemViewHolder(convertView)
        holder.setIsRecyclable(false)

        return holder
    }

    inner class TokenItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.token_icon)
        val symbol: BaseTextView = view.findViewById(R.id.token_symbol)
        val name: BaseTextView = view.findViewById(R.id.token_name)
        val addRemoveButton: Button = view.findViewById(R.id.add_remove_button)
        val iconParent: View = view.findViewById(R.id.icon_parent)
        val iconLetter: BaseTextView = view.findViewById(R.id.icon_letter)

        init {
            val typeface = Typeface.createFromAsset(context.assets, "fonts/CircularPro-Book.otf")
            addRemoveButton.typeface = typeface
        }
    }
}
