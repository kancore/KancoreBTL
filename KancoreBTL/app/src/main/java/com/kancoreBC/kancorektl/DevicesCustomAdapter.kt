package com.kancoreBC.kancorektl

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DevicesCustomAdapter(
    var devicesbtActivity: Context,
    var deviceMacList: ArrayList<String>,
    var deviceNamesList: ArrayList<String>
): BaseAdapter() {
    override fun getCount(): Int {
        return deviceMacList.size //returns the Total numbers of elements to be created
    }

    override fun getItem(position: Int): Any {
        return position //returns the element itself
    }

    override fun getItemId(position: Int): Long {
        return position.toLong() //returns the ID of the element itself
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(devicesbtActivity).inflate(R.layout.devices_list_row, parent, false)
        val device_name = view?.findViewById<TextView>(R.id.row_bt_device_name)
        val device_mac = view?.findViewById<TextView>(R.id.row_bt_device_mac)
        device_name?.text = deviceNamesList[position]
        device_mac?.text = deviceMacList[position]
        return view
    }

    fun getAdress(position: Int): String {
        return deviceMacList[position]
    }
}
