package com.breadwallet.ui.addwallets

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import com.breadwallet.R
import com.breadwallet.tools.util.TokenUtil
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import java.io.File

class AddTokenListAdapter(
    private val context: Context,
    private val tokensFlow: Flow<List<Token>>,
    private val sendChannel: SendChannel<AddWalletsEvent>
) : RecyclerView.Adapter<AddTokenListAdapter.TokenItemViewHolder>() {

    private var tokens: List<Token> = emptyList()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        tokensFlow
            .onEach { tokens ->
                this.tokens = tokens
                notifyDataSetChanged()
            }
            .launchIn(CoroutineScope(Dispatchers.Main))
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

        holder.addRemoveButton.apply {
            val token = tokens[holder.adapterPosition]
            text = context.getString(
                when {
                    token.enabled -> R.string.TokenList_remove
                    else -> R.string.TokenList_add
                }
            )

            isEnabled = !token.enabled || token.removable

            setOnClickListener {
                if (token.enabled) {
                    sendChannel.offer(AddWalletsEvent.OnRemoveWalletClicked(token))
                } else {
                    sendChannel.offer(AddWalletsEvent.OnAddWalletClicked(token))
                }
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
        val symbol: TextView = view.findViewById(R.id.token_symbol)
        val name: TextView = view.findViewById(R.id.token_name)
        val addRemoveButton: Button = view.findViewById(R.id.add_remove_button)
        val iconParent: View = view.findViewById(R.id.icon_parent)
        val iconLetter: TextView = view.findViewById(R.id.icon_letter)

        init {
            val typeface = Typeface.createFromAsset(context.assets, "fonts/CircularPro-Book.otf")
            addRemoveButton.typeface = typeface
        }
    }
}
