package com.kancoreBC.kancorektl

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.kancoreBC.kancorektl.databinding.FragmentDevicesbtBinding


class DevicesbtFragment : Fragment() {

    private var btAdapter: BluetoothAdapter? = null //this will be used as the bluetooth adapter
    private lateinit var btPaireddevices: Set<BluetoothDevice> //list of paired devices
    private val requestBTEnable = 1 //This value is used in the intent for bt enabling
    private lateinit var mcontext: Context
    private lateinit var binding: FragmentDevicesbtBinding


    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        try {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_devicesbt, container, false
            )

            //get context from parent and saves it in a variable for use it outside onCreateView
            mcontext = container!!.context
            //Forces the app to be on portrait
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            //bt_adapter = BluetoothAdapter.getDefaultAdapter() //this was deprecated on API 31. Lets try the new method
            val btManager: BluetoothManager =
                activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager //used for getting the BT adapter
            btAdapter = btManager.adapter
            if (btAdapter == null) { //check if the device has a BT adapter
                Toast.makeText(
                    container.context,
                    "This device doesn't support bluetooth. help :(",
                    Toast.LENGTH_LONG
                ).show()
                return view
            } else { //if the device has a bt adapter, check if it is enabled
                if (!btAdapter!!.isEnabled) {
                    tryEnabling() //if not, this tries to enable it
                }
                binding.devicesBtRefresh.setOnClickListener { refreshThis() } //Set an oncliclstener for filling the list
            }
            return binding.root

        } catch (e: Exception) {
            Log.e("My_catch", "onCreateView", e)
        }

        return view
    }

    //updates the bluetooth devices list
    @SuppressLint("MissingPermission")
    fun refreshThis(){

        if (!btAdapter!!.isEnabled){ //one more time, makes sure the bluetooth is turned on
            Toast.makeText(mcontext, "First turn on the BLuetooth and try it again", Toast.LENGTH_SHORT).show()
            tryEnabling()
        } else {
            btPaireddevices = btAdapter!!.bondedDevices //get the paired bt devices
            val deviceNamesList: ArrayList<String> = ArrayList() //prepares a list with the btDevices
            val deviceMacList: ArrayList<String> = ArrayList() //prepares a list with the btDevices adress
            if (btPaireddevices.isNotEmpty()){
                btPaireddevices.forEach { device ->
                    deviceMacList.add(device.address) //fill the list with the MAC adress
                    deviceNamesList.add(device.name) //fill the list with the device name
                    Log.i("Device", ""+device.name) //Just for purposes of debugging
                    Log.i("Adress", ""+device.address) //Just for purposes of debuggin
                }
                //creates a custom adapter for the list of devices
                val adapter = DevicesCustomAdapter(mcontext, deviceMacList, deviceNamesList)
                binding.devicesBtList.adapter = adapter
                //set OnclicListener for each element of the list of devices
                binding.devicesBtList.setOnItemClickListener{_, view, position, _ ->
                    //Select the mac adress of the selected device and stores it on a variable
                    val deviceMac: String = adapter.getAdress(position)
                    //Go to the joystick fragment and uses safeargs for send device adress :)
                    view.findNavController().navigate(DevicesbtFragmentDirections.actionDevicesbtFragmentToJoystickFragment(deviceMac))
                    //destroys this view, as we dont haves nothing more to do here
                    onDestroyView()

                }
            }
        }


    }

    @SuppressLint("MissingPermission") //ignores the missing permission sugestion
    //ask the user for enable bluetooth
    fun tryEnabling(){
        val btOnIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(btOnIntent, requestBTEnable)
    }

    //Print some message if user doesn't allow bluetooth to enable
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestBTEnable) {
            if (resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(mcontext, "You can't drive with the bluetooth off. Please turn it on!!!", Toast.LENGTH_LONG).show()
            } else if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(mcontext, "Gotcha, trying to refresh it now!", Toast.LENGTH_SHORT).show()
                refreshThis()
            }
        }
    }

}


