package com.github.hu1won.fishbowl

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class Main : AppCompatActivity() {

    private val REQUEST_BLUETOOTH_ENABLE = 100
    private var mConnectionStatus: TextView? = null
    private var mInputEditText: EditText? = null
    private var mInputEditText_off: EditText? = null
    var mConnectedTask: ConnectedTask? = null
    var picker: TimePicker? = null

    private var mConnectedDeviceName: String? = null
    private var mConversationArrayAdapter: ArrayAdapter<String>? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        picker = findViewById<View>(R.id.time) as TimePicker
        picker!!.setIs24HourView(true)

        // 앞서 설정한 값으로 보여주기
        // 없으면 디폴트 값은 현재시간
        val sharedPreferences = getSharedPreferences("daily alarm", MODE_PRIVATE)
        val millis =
            sharedPreferences.getLong("nextNotifyTime", Calendar.getInstance().timeInMillis)

        val nextNotifyTime: Calendar = GregorianCalendar()
        nextNotifyTime.timeInMillis = millis

        val nextDate = nextNotifyTime.time
        val date_text: String =
            SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(nextDate)
        Toast.makeText(
            applicationContext,
            "[처음 실행시] 다음 알람은 " + date_text + "으로 알람이 설정되었습니다!",
            Toast.LENGTH_SHORT
        ).show()

        // 이전 설정값으로 TimePicker 초기화
        val currentTime = nextNotifyTime.time
        val HourFormat = SimpleDateFormat("kk", Locale.getDefault())
        val MinuteFormat = SimpleDateFormat("mm", Locale.getDefault())

        val pre_hour: Int = HourFormat.format(currentTime).toInt()
        val pre_minute: Int = MinuteFormat.format(currentTime).toInt()


        if (Build.VERSION.SDK_INT >= 23) {
            picker!!.hour = pre_hour
            picker!!.minute = pre_minute
        } else {
            picker!!.currentHour = pre_hour
            picker!!.currentMinute = pre_minute
        }

        mInputEditText = findViewById<View>(R.id.edit01) as EditText
        mInputEditText_off = findViewById<View>(R.id.edit02) as EditText
        mConnectionStatus = findViewById<View>(R.id.connection_status_textview) as TextView
        mConversationArrayAdapter = ArrayAdapter(this,
            android.R.layout.simple_list_item_1)
        //mMessageListview.adapter = mConversationArrayAdapter
        Log.d(TAG, "Initalizing Bluetooth adapter...")
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device is not implement Bluetooth.")
            return
        }
        if (!mBluetoothAdapter!!.isEnabled) {
            val intent2 = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent2, REQUEST_BLUETOOTH_ENABLE)
        } else {
            Log.d(TAG, "Initialisation successful.")
            //showPairedDevicesListDialog()
        }

        // 이 버튼은 나중에 아두이노에 신호 줄때 사용하세요!
        val sendButton = findViewById<View>(R.id.led_on_btn) as Button
        sendButton.setOnClickListener {
            var sendMessage = mInputEditText!!.text.toString()
//            var sendMessage = "ON"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        val off_sendButton = findViewById<View>(R.id.led_off_btn) as Button
        off_sendButton.setOnClickListener {
//            var sendMessage = mInputEditText!!.text.toString()
            var sendMessage = "OFF"
            if (sendMessage.length > 0) {
                sendMessage_off(sendMessage)
            }
        }

        val eat_auto = findViewById<View>(R.id.eat_auto_btn) as Button
        eat_auto.setOnClickListener {
            val hour: Int
            val hour_24: Int
            val minute: Int
            val am_pm: String
            if (Build.VERSION.SDK_INT >= 23) {
                hour_24 = picker!!.hour
                minute = picker!!.minute
            } else {
                hour_24 = picker!!.currentHour
                minute = picker!!.currentMinute
            }
            if (hour_24 > 12) {
                am_pm = "PM"
                hour = hour_24
            } else {
                hour = hour_24
                am_pm = "AM"
            }

            // 현재 지정된 시간으로 알람 시간 설정
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar[Calendar.HOUR_OF_DAY] = hour_24
//            calendar[Calendar.HOUR] = hour_24
            calendar[Calendar.MINUTE] = minute
            calendar[Calendar.SECOND] = 0

            // 이미 지난 시간을 지정했다면 다음날 같은 시간으로 설정
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1);
            }

            val currentDateTime = calendar.time
            val date_text =
                SimpleDateFormat("HHmm", Locale.getDefault()).format(
                    currentDateTime
                )
            Toast.makeText(applicationContext, date_text + "으로 알람이 설정되었습니다!", Toast.LENGTH_SHORT)
                .show()

            //  Preference에 설정한 값 저장
            val editor = getSharedPreferences("daily alarm", MODE_PRIVATE).edit()
            editor.putLong("nextNotifyTime", calendar.timeInMillis)
            editor.apply()

            var sendMessage = "D"+"$date_text"
            if (sendMessage.length > 0) {
                sendMessage_off(sendMessage)
            }
        }

        val eat_passive = findViewById<View>(R.id.eat_btn) as Button
        eat_passive.setOnClickListener {
//            var sendMessage = mInputEditText!!.text.toString()
            var sendMessage = "FEED"
            if (sendMessage.length > 0) {
                sendMessage_off(sendMessage)
            }
        }

//        val next222 = findViewById<View>(R.id.next222) as Button
//        next222.setOnClickListener(object  : View.OnClickListener{
//            override fun onClick(v: View?){
//                // Activity 수정 필요
//                val intent = Intent(this@Bluetooth, WaterAcitivity::class.java)
//                startActivity(intent)
//            }})

        val pairbluetoothButton = findViewById<View>(R.id.bluetooth_con_btn) as Button
        pairbluetoothButton.setOnClickListener(object  : View.OnClickListener{
            override fun onClick(v: View?){
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
            }})

        val connectbluetoothButton = findViewById<View>(R.id.bluetooth_con_btn2) as Button
        connectbluetoothButton.setOnClickListener (object : View.OnClickListener{
            override fun onClick(v: View?){
                showPairedDevicesListDialog()
            }})

    }

    override fun onDestroy() {
        super.onDestroy()
        if (mConnectedTask != null) {
            mConnectedTask!!.cancel(true)
        }
    }


    private inner class ConnectTask internal constructor(bluetoothDevice: BluetoothDevice) : AsyncTask<Void?, Void?, Boolean>() {
        private var mBluetoothSocket: BluetoothSocket? = null
        private var mBluetoothDevice: BluetoothDevice? = null
        protected override fun doInBackground(vararg params: Void?): Boolean? {

            // 항상 검색 취소 (연결속도느려짐방지)
            mBluetoothAdapter!!.cancelDiscovery()

            // BluetoothSocket 연결
            try {
                // 차단 호출
                mBluetoothSocket!!.connect()
            } catch (e: IOException) {
                try {
                    mBluetoothSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2)
                }
                return false
            }
            return true
        }

        override fun onPostExecute(isSucess: Boolean) {
            if (isSucess) {
                connected(mBluetoothSocket)
            } else {
                isConnectionError = true
                Log.d(TAG, "Unable to connect device")
                showErrorDialog("Unable to connect device")
            }
        }

        init {
            mBluetoothDevice = bluetoothDevice
            mConnectedDeviceName = bluetoothDevice.name

            //SPP
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            try {
                mBluetoothSocket = mBluetoothDevice!!.createRfcommSocketToServiceRecord(uuid)
                Log.d(TAG, "create socket for $mConnectedDeviceName")
            } catch (e: IOException) {
                Log.e(TAG, "socket create failed " + e.message)
            }
            mConnectionStatus!!.text = "Bluetooth - " + "$mConnectedDeviceName"
        }
    }

    fun connected(socket: BluetoothSocket?) {
        mConnectedTask = ConnectedTask(socket)
        mConnectedTask!!.execute()
    }

    inner class ConnectedTask internal constructor(socket: BluetoothSocket?) : AsyncTask<Void?, String?, Boolean>() {
        private var mInputStream: InputStream? = null
        private var mOutputStream: OutputStream? = null
        private var mBluetoothDevice: BluetoothDevice? = null
        private var mBluetoothSocket: BluetoothSocket? = null
        @SuppressLint("WrongThread")
        protected override fun doInBackground(vararg params: Void?): Boolean? {
            val readBuffer = ByteArray(1024)
            var readBufferPosition = 0

            while (true) {
                if (isCancelled) return false
                try {
                    //Log.e("check", "메시지 들어왔따!")
                    val bytesAvailable = mInputStream!!.available()
                    //Log.e("check", "바이트로 받을거야.")
                    if (bytesAvailable > 0) {
                        Log.e("check", "바이트 받는다 시작!")
                        val packetBytes = ByteArray(bytesAvailable)
                        mInputStream!!.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            var b = packetBytes[i]
                            if (b == '\n'.toByte()) {
                                var passfail = ""
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                    encodedBytes.size)
                                var recvMessage = String(encodedBytes, Charsets.UTF_8)
                                val array = recvMessage.split("X");
                                for(j in array.indices) {
                                    println(array[j])
                                }
                                println("여기");
                                println(array[array.size - 1])
//                                BPM = array[array.size - 1]
                                Log.e("apple", recvMessage + "-------------")
                                Log.e("apple", "$recvMessage" + "000000000000")

                                if(recvMessage == "okay\n")
                                    Log.e("apple", "변수 들어가는지...")

                                //Toast.makeText(this@Main, recvMessage, Toast.LENGTH_SHORT).show()
                                //readBufferPosition = 0
                                //publishProgress(recvMessage)
                                // test?.setText(recvMessage)

                            } else {
                                Log.e("check", "혹시 여기로 빠지나?.")
                                readBuffer[readBufferPosition++] = b
                                Log.e("check", "그러네.")
                            }

                            Log.e("check", "바이트 받는중.")
                        }

                        Log.e("check", "바이트 ???.")
//                        temptext.setText(abcd)
                    }
//                    Log.e("check", "바이트 다받았다..")
//                    temptext.setText(abcd)

                }
                catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    return false
                }
            }
            Log.e("check", "혹시 출력")
            //temptext.setText(abcd)

        }

        override fun onPostExecute(isSucess: Boolean) {
            super.onPostExecute(isSucess)
            if (!isSucess) {
                closeSocket()
                Log.d(TAG, "Device connection was lost")
                isConnectionError = true
                showErrorDialog("Device connection was lost")
            }
        }

        override fun onCancelled(aBoolean: Boolean) {
            super.onCancelled(aBoolean)
            closeSocket()
        }

        fun closeSocket() {
            try {
                mBluetoothSocket!!.close()
                Log.d(TAG, "close socket()")
            } catch (e2: IOException) {
                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2)
            }
        }

        fun write(msg: String) {
            var msg = msg
            msg += "\n"

            try {
                mOutputStream!!.write(msg.toByteArray())
                mOutputStream!!.flush()
                Log.e("msg", msg + "나는 메세지 보냈어!")
            } catch (e: IOException) {
                Log.e(TAG, "Exception during send", e)
            }
            mInputEditText!!.setText("")
        }

        init {
            mBluetoothSocket = socket
            try {
                mInputStream = mBluetoothSocket!!.inputStream
                mOutputStream = mBluetoothSocket!!.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "socket not created", e)
            }
            Log.d(TAG, "connected to $mConnectedDeviceName")

//            mConnectionStatus!!.text = " - $mConnectedDeviceName"

        }
    }

    fun showPairedDevicesListDialog() {
        val devices = mBluetoothAdapter!!.bondedDevices
        val pairedDevices = devices.toTypedArray()
        if (pairedDevices.size == 0) {
            showQuitDialog("""
    No devices have been paired.
    You must pair it with another device.
    """.trimIndent())
            return
        }
        val items: Array<String?>
        items = arrayOfNulls(pairedDevices.size)
        for (i in pairedDevices.indices) {
            items[i] = pairedDevices[i].name
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("블루투스연결")
        //builder.setCancelable(false)
        builder.setItems(items, DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
            val task = ConnectTask(pairedDevices[which])
            task.execute()
        })
        builder.create().show()
    }

    fun showErrorDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quit")
        builder.setCancelable(false)
        builder.setMessage(message)
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
            if (isConnectionError) {
                isConnectionError = false
                //finish()
            }
        })
        builder.create().show()
    }

    fun showQuitDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quit")
        builder.setCancelable(false)
        builder.setMessage(message)
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
            finish()
        })
        builder.create().show()
    }

    fun sendMessage(msg: String) {
        if (mConnectedTask != null) {
            mConnectedTask!!.write(msg)
            Log.d(TAG, "send message: $msg")
            //mConversationArrayAdapter!!.insert("Me:  $msg", 0)
        }
    }

    fun sendMessage_off(msg: String) {
        if (mConnectedTask != null) {
            mConnectedTask!!.write(msg)
            Log.d(TAG, "send message: $msg")
            //mConversationArrayAdapter!!.insert("Me:  $msg", 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_OK) {
                // BlueTooth 활성화
                showPairedDevicesListDialog()
            }
            if (resultCode == RESULT_CANCELED) {
                showQuitDialog("You need to enable bluetooth")
            }
        }
    }
    companion object {
        var mBluetoothAdapter: BluetoothAdapter? = null
        var isConnectionError = false
        private const val TAG = "BluetoothClient"
    }
}