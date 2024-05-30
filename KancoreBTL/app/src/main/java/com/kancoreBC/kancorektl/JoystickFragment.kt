package com.kancoreBC.kancorektl

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.kancoreBC.kancorektl.databinding.FragmentJoystickBinding
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs


class JoystickFragment : Fragment(), KanJoypadView.JoypadListener {
    //Variables for the joypad:
    private var rJoypad: Int = 0 //It will take the id from the right joypad
    private var lJoypad: Int = 0 //It will take the id from the left joypad
    private var enginesArray = arrayOf(0f, 0f, 0f, 0f) //Array to be send to the arduino board
    private var lastSent = arrayOf(0f, 0f) //stores the last speed values sent to the arduino, its being used for limit the frequency in which data is being sent
    private val df = DecimalFormat("0.00") //used for formatting the float that will be sent to arduino
    //Variables for bluetooth:
    private lateinit var btManager: BluetoothManager //The bluetooth manager from the device
    private lateinit var btAdapter: BluetoothAdapter //The bluetooth adapter from the device. we know it is not null on this fragment
    private lateinit var btDevice: BluetoothDevice //The device that will be controlled with the joystick
    private lateinit var btConnectDevice: ConnectThread //The thread used for creating the BluetoothSocket with arduino

    //Variables for playing music:
    private var mediaPlayer: MediaPlayer? = null //Used for playing audio
    private var playing = false // This will handle whether or not the music is currently playing
    private val songs: MutableList<Song> = mutableListOf(
        Song("Initial D - Wings of Fire", R.raw.initial_d_wings_of_fire),
        Song("Initial D - Running in the 90's", R.raw.initial_d_running_in_the_90s),
        Song("Initial D - Deja Vu", R.raw.initial_d_deja_vu)
    ) //the array containing all the songs. Note 'Song' is a data class we're creating next
    private var currentSong: Song = songs[0] //the song that is currently selected. Its initialized by default to first song in the array
    private var songIndex = 0 //index of the songs array that is currently selected

    //For databinding
    private lateinit var binding: FragmentJoystickBinding //required for databinding
    //data class used for storing the songs
    data class Song(val name: String, val file: Int)


    //onCreateView function, where most of the magic happens, it initializes most of the variables and setup touch listeners for most of the buttons
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Force the landscape orientation. Who needs portrait for drive?
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        //hides the action bar. Who needs it an action bar for drive?
        (activity as AppCompatActivity).supportActionBar?.hide()
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_joystick, container, false
        )


        //configure the DecimalFormat for format the floats from the array
        val sym: DecimalFormatSymbols? = DecimalFormatSymbols.getInstance()
        sym?.decimalSeparator = '.'
        df.decimalFormatSymbols = sym


        //Initialize the bluetooth adapter, we're assuming it will be not null as it was not null on the previous fragments
        btManager = context?.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter

        //Get the Device Address from the previous fragment
        val args = JoystickFragmentArgs.fromBundle(requireArguments())
        btDevice = btAdapter.getRemoteDevice(args.deviceAddress)

        //setting the reconnect button
        binding.reconnectBT.setOnClickListener {
            reconnect()
        }

        //setting the music buttons
        binding.playpause.setOnClickListener {
            playPause()
        }

        binding.nextSong.setOnClickListener {
            nextSong()
            binding.onPlaying.text = currentSong.name
        }

        binding.previous.setOnClickListener {
            prevSong()
            binding.onPlaying.text = currentSong.name
        }

        //Initializing the song on Playing
        binding.onPlaying.text = currentSong.name

        //initialize the media player
        setMediaPlayer()


        //returning the view
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //Initializing the thread for the connection and starting it
        btConnectDevice = ConnectThread(btDevice)
        btConnectDevice.start()

        //Initialize variables for Joystick
        rJoypad = binding.rightJoypad.id
        lJoypad = binding.leftJoypad.id
        JoypadThread().start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //btConnectDevice.interrupt()
        (activity as AppCompatActivity).supportActionBar?.show()
    }


    //the next block of functions are used for music player
    private fun playPause(){
        if (!playing){
            binding.playpause.setImageResource(R.drawable.ic_baseline_pause_36)
            mediaPlayer?.start()
            playing=true
        }else {
            binding.playpause.setImageResource(R.drawable.ic_baseline_play_arrow_36)
            mediaPlayer?.pause()
            playing=false
        }
    }

    private fun nextSong(){
        if (songIndex!=2){
            currentSong = songs[songIndex+1]
            songIndex++
        } else {
            currentSong = songs[0]
            songIndex = 0
        }
        if (playing){
            mediaPlayer?.stop()
            setMediaPlayer()
            playing = false
            playPause()
        } else {
            mediaPlayer?.stop()
            setMediaPlayer()
        }
    }

    private fun prevSong(){
        if (songIndex!=0){
            currentSong = songs[songIndex-1]
            songIndex--
        } else {
            currentSong = songs[2]
            songIndex = 2
        }
        if (playing){
            mediaPlayer?.stop()
            setMediaPlayer()
            playing = false
            playPause()
        } else {
            mediaPlayer?.stop()
            setMediaPlayer()
        }
    }

    private fun setMediaPlayer() {
        mediaPlayer = MediaPlayer.create(context, currentSong.file)
    }

    //this listener (actually its a function) handles the logic for the joypads and calls the function for sending the string with the array of data to arduino


    inner class JoypadThread : Thread(){
        override fun run() {
            val listener = object: KanJoypadView.JoypadListener {
                override fun onJoypadMove(xPercent: Float, yPercent: Float, src: Int) {
                    var btSend = false

                    if (src == lJoypad) {
                        if (abs(abs(yPercent) - lastSent[0]) > 0.05f){
                            lastSent[0] = abs(yPercent)
                            btSend = true
                        }
                        if (yPercent >= 0) {
                            enginesArray[0] = 0.00f
                            enginesArray[1] = yPercent
                        } else if (yPercent < 0) {
                            enginesArray[0] = 1.00f
                            enginesArray[1] = abs(yPercent)
                        }
                    }
                    if (src == rJoypad) {
                        if (abs(abs(yPercent) - lastSent[1]) > 0.05f)
                            lastSent[1] = abs(yPercent)
                            btSend = true
                        if (yPercent >= 0) {
                            enginesArray[2] = 0.00f
                            enginesArray[3] = yPercent
                        } else if (yPercent < 0) {
                            enginesArray[2] = 1.00f
                            enginesArray[3] = abs(yPercent)
                        }
                    }
                    if (btSend) {
                        val message = (
                                "<" + df.format(enginesArray[0]) +
                                        "," +
                                        df.format(enginesArray[1]) +
                                        "," +
                                        df.format(enginesArray[2]) +
                                        "," +
                                        df.format(enginesArray[3]) + ">"
                                )
                        btConnectDevice.sendToDevice(message)
                        Log.d("Message: ", message)
                        binding.leftPower.text = "Left wheel: " + (df.format(lastSent[0]).toFloat() * 100).toInt() + "%"
                        binding.rightPower.text = "Left wheel: " + (df.format(lastSent[1]).toFloat() * 100).toInt() + "%"
                    }
                }
            }
            binding.leftJoypad.joypadCallback = listener
            binding.rightJoypad.joypadCallback = listener
        }
    }

    private fun reconnect(){
        Log.d("Is thread alive?", btConnectDevice.isAlive.toString())
        if (btConnectDevice.isAlive){
            btConnectDevice.killme()
            btConnectDevice.interrupt()
        }
        else {
            btConnectDevice.killme()
            btConnectDevice.run()
        }
    }

    //this is the thread that makes the bluetooth socket and also has the function for sending data to arduino
    @SuppressLint("MissingPermission")
    inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val mmSocket: BluetoothSocket? = device.createInsecureRfcommSocketToServiceRecord(myUUID)
        private val outputStream = mmSocket?.outputStream
        private var isConnected = false

        override fun run() {
            // Cancel discovery because otherwise it slows down the connection.
            btAdapter.cancelDiscovery()
                if (mmSocket != null){
                    try {
                        if (!mmSocket!!.isConnected)
                            mmSocket!!.connect()
                            isConnected = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
       }

        fun sendToDevice(message: String){
            if (mmSocket!!.isConnected) {
                outputStream?.write(message.toByteArray())
            } else {
                Log.d("Socked disconnected", mmSocket.toString())
            }

        }

        // Closes the client socket and causes the thread to finish.
        private fun cancel() {
            if (mmSocket?.isConnected == true) {
                try {
                    mmSocket?.close()
                    isConnected = false
                    sleep(1000)
                } catch (e: IOException) {
                    Log.e("TAG", "Could not close the client socket", e)
                }
            }
        }

        fun killme(){
            cancel()
        }
    }

}


