package com.anyjob.ui.explorer.search.controls.bottomSheets.addresses.adapters

import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anyjob.R
import com.anyjob.ui.explorer.search.controls.bottomSheets.addresses.models.UserAddress

object AddressDifferenceCallback : DiffUtil.ItemCallback<Address>() {
    override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
        return oldItem.longitude == newItem.longitude && oldItem.latitude == newItem.latitude
    }
}

class AddressesAdapter(
    private val addresses: List<Address>,
    private val onClick: ((UserAddress) -> Unit)? = null
) : ListAdapter<Address, AddressesAdapter.ViewHolder>(AddressDifferenceCallback) {
    class ViewHolder(
        private val view: View,
        private val onClick: ((UserAddress) -> Unit)? = null
    ) : RecyclerView.ViewHolder(view) {
        private val _addressTitle: TextView = view.findViewById(R.id.address_primary_text)
        private val _addressSubtitle: TextView = view.findViewById(R.id.address_secondary_text)
        private var _currentAddress: Address? = null

        fun bind(address: Address) {
            _currentAddress = address

            val houseAddressLines = listOfNotNull(
                address.thoroughfare,
                address.subThoroughfare
            )

            val city = address.locality
            val adminAreaLines = listOfNotNull(
                city,
                address.adminArea,
                address.subAdminArea
            )

            val houseAddress = houseAddressLines.joinToString(", ")
            val region = adminAreaLines.joinToString(", ")

            if (houseAddress.isNotBlank()) {
                _addressTitle.text = houseAddress
                _addressSubtitle.text = region
            }
            else {
                _addressTitle.text = city
                _addressSubtitle.text = adminAreaLines.drop(1).joinToString(", ")
            }

            if (onClick != null) {
                view.setOnClickListener {
                    _currentAddress?.also {
                        val userAddress = UserAddress(
                            source = it,
                            formattedAddress = _addressTitle.text.toString()
                        )

                        onClick.invoke(userAddress)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.address_item, parent, false)

        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (addresses.any()) {
            holder.bind(addresses[position])
        }
    }

    override fun getItemCount(): Int {
        return addresses.size
    }
}