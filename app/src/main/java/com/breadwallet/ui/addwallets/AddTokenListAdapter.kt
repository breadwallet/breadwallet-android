/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/11/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.addwallets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.breadwallet.R
import com.breadwallet.tools.util.TokenUtil
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.Locale

class AddTokenListAdapter(
    private val tokensFlow: Flow<List<Token>>,
    private val sendChannel: SendChannel<AddWallets.E>
) : RecyclerView.Adapter<AddTokenListAdapter.TokenItemViewHolder>() {

    private var tokens: List<Token> = emptyList()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        tokensFlow
            .onEach { tokens ->
                this.tokens = tokens
                notifyDataSetChanged()
            }
            .launchIn(scope)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.coroutineContext.cancelChildren()
    }

    override fun onBindViewHolder(holder: TokenItemViewHolder, position: Int) {
        val token = tokens[position]
        val currencyCode = token.currencyCode.toLowerCase(Locale.ROOT)
        val tokenIconPath = TokenUtil.getTokenIconPath(currencyCode, true)

        val iconDrawable = holder.iconParent.background as GradientDrawable

        when {
            tokenIconPath == null -> {
                // If no icon is present, then use the capital first letter of the token currency code instead.
                holder.iconLetter.visibility = View.VISIBLE
                iconDrawable.setColor(Color.parseColor(token.startColor))
                holder.iconLetter.text = currencyCode.substring(0, 1).toUpperCase(Locale.ROOT)
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
        holder.symbol.text = token.currencyCode.toUpperCase(Locale.ROOT)

        holder.addRemoveButton.apply {
            text = context.getString(
                when {
                    token.enabled -> R.string.TokenList_remove
                    else -> R.string.TokenList_add
                }
            )

            isEnabled = !token.enabled || token.removable

            setOnClickListener {
                if (token.enabled) {
                    sendChannel.offer(AddWallets.E.OnRemoveWalletClicked(token))
                } else {
                    sendChannel.offer(AddWallets.E.OnAddWalletClicked(token))
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

        val inflater = LayoutInflater.from(parent.context)
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
    }
}
