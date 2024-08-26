package com.transactpay.transactpay_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso

class BankSpinnerAdapter(context: Context, private val banks: List<Bank>) :
    ArrayAdapter<Bank>(context, 0, banks) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)

        val bankLogoImageView = view.findViewById<ImageView>(R.id.bankLogoImageView)
        val bankNameTextView = view.findViewById<TextView>(R.id.bankNameTextView)

        val currentBank = banks[position]

        bankNameTextView.text = currentBank.name
        Picasso.get().load(currentBank.logoUrl).into(bankLogoImageView)

        return view
    }
}
