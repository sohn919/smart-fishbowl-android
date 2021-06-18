package com.github.hu1won.fishbowl

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.lang.Double.parseDouble
import java.text.SimpleDateFormat
import java.util.*


class Main : AppCompatActivity() {
    private var mtemprecieve1:EditText? = null
    private var mtemprecieve2:EditText? = null
    private var mphrecieve1:EditText? = null
    private var mphrecieve2:EditText? = null
    private var mHandler: Handler? = null //핸들러
    private var mPh: TextView? = null //ph
    private var mTemp: TextView? = null //temp
    private var mWebView: WebView? = null // 웹뷰 선언
    private var mWebSettings: WebSettings? = null //웹뷰세팅
    private val REQUEST_BLUETOOTH_ENABLE = 100
    private var mConnectionStatus: TextView? = null
    private var mInputEditText: EditText? = null
    private var mInputEditText_off: EditText? = null
    var mConnectedTask: ConnectedTask? = null
    var eatpicker: TimePicker? = null
    var ledpicker: TimePicker? = null
    var ledoffpicker: TimePicker? = null
    var waterpicker: TimePicker? = null

    private val MESSAGE_READ = 2 //핸들러 메세지 수신

    private var mConnectedDeviceName: String? = null
    private var mConversationArrayAdapter: ArrayAdapter<String>? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        //툴바
        val toolbar = findViewById<Toolbar>(R.id.toolbar)


        //툴바 관련
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayShowTitleEnabled(false)



        mtemprecieve1 = findViewById<View>(R.id.temprecieve1) as EditText
        mtemprecieve2 = findViewById<View>(R.id.temprecieve2) as EditText
        val temprecieveButton = findViewById<View>(R.id.button1) as Button
        temprecieveButton.setOnClickListener {
            var sendMessage = "TTE" + mtemprecieve1!!.text.toString() + "/" + mtemprecieve2!!.text.toString()
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        mphrecieve1 = findViewById<View>(R.id.phrecieve1) as EditText
        mphrecieve2 = findViewById<View>(R.id.phrecieve2) as EditText
        val phrecieveButton = findViewById<View>(R.id.button2) as Button
        phrecieveButton.setOnClickListener {
            var sendMessage = "PPH" + mphrecieve1!!.text.toString() + "/" + mphrecieve2!!.text.toString()
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        eatpicker = findViewById<View>(R.id.eattime) as TimePicker
        eatpicker!!.setIs24HourView(true)

        ledpicker = findViewById<View>(R.id.ledtime) as TimePicker
        ledpicker = findViewById<View>(R.id.ledtime) as TimePicker

        ledoffpicker = findViewById<View>(R.id.ledtime2) as TimePicker
        ledoffpicker = findViewById<View>(R.id.ledtime2) as TimePicker


        waterpicker = findViewById<View>(R.id.watertime) as TimePicker
        waterpicker = findViewById<View>(R.id.watertime) as TimePicker


        // ledoff앞서 설정한 값으로 보여주기
        // 없으면 디폴트 값은 현재시간
        val ledoffsharedPreferences = getSharedPreferences("ledoff alarm", MODE_PRIVATE)
        val ledoffmillis =
                ledoffsharedPreferences.getLong("nextNotifyLedoffTime", Calendar.getInstance().timeInMillis)

        val nextNotifyLedoffTime: Calendar = GregorianCalendar()
        nextNotifyLedoffTime.timeInMillis = ledoffmillis

        val ledoffnextDate = nextNotifyLedoffTime.time
        val ledoffdate_text: String =
                SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(ledoffnextDate)
        Toast.makeText(
                applicationContext,
                "[LED OFF] 다음 알람은 " + ledoffdate_text + "으로 알람이 설정되었습니다!",
                Toast.LENGTH_SHORT
        ).show()

        // 이전 설정값으로 TimePicker 초기화
        val lfcurrentTime = nextNotifyLedoffTime.time
        val lfHourFormat = SimpleDateFormat("kk", Locale.getDefault())
        val lfMinuteFormat = SimpleDateFormat("mm", Locale.getDefault())

        val lfpre_hour: Int = lfHourFormat.format(lfcurrentTime).toInt()
        val lfpre_minute: Int = lfMinuteFormat.format(lfcurrentTime).toInt()



        // ledon앞서 설정한 값으로 보여주기
        // 없으면 디폴트 값은 현재시간
        val ledsharedPreferences = getSharedPreferences("led alarm", MODE_PRIVATE)
        val ledmillis =
                ledsharedPreferences.getLong("nextNotifyLedTime", Calendar.getInstance().timeInMillis)

        val nextNotifyLedTime: Calendar = GregorianCalendar()
        nextNotifyLedTime.timeInMillis = ledmillis

        val lednextDate = nextNotifyLedTime.time
        val leddate_text: String =
                SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(lednextDate)
        Toast.makeText(
                applicationContext,
                "[LED ON] 다음 알람은 " + leddate_text + "으로 알람이 설정되었습니다!",
                Toast.LENGTH_SHORT
        ).show()

        // 이전 설정값으로 TimePicker 초기화
        val lcurrentTime = nextNotifyLedTime.time
        val lHourFormat = SimpleDateFormat("kk", Locale.getDefault())
        val lMinuteFormat = SimpleDateFormat("mm", Locale.getDefault())

        val lpre_hour: Int = lHourFormat.format(lcurrentTime).toInt()
        val lpre_minute: Int = lMinuteFormat.format(lcurrentTime).toInt()


        // eat앞서 설정한 값으로 보여주기
        // 없으면 디폴트 값은 현재시간
        val eatsharedPreferences = getSharedPreferences("eat alarm", MODE_PRIVATE)
        val eatmillis =
                eatsharedPreferences.getLong("nextNotifyEatTime", Calendar.getInstance().timeInMillis)

        val nextNotifyEatTime: Calendar = GregorianCalendar()
        nextNotifyEatTime.timeInMillis = eatmillis

        val eatnextDate = nextNotifyEatTime.time
        val eatdate_text: String =
                SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(eatnextDate)
        Toast.makeText(
                applicationContext,
                "[먹이] 다음 알람은 " + eatdate_text + "으로 알람이 설정되었습니다!",
                Toast.LENGTH_SHORT
        ).show()

        // 이전 설정값으로 TimePicker 초기화
        val ecurrentTime = nextNotifyEatTime.time
        val eHourFormat = SimpleDateFormat("kk", Locale.getDefault())
        val eMinuteFormat = SimpleDateFormat("mm", Locale.getDefault())

        val epre_hour: Int = eHourFormat.format(ecurrentTime).toInt()
        val epre_minute: Int = eMinuteFormat.format(ecurrentTime).toInt()


        // water앞서 설정한 값으로 보여주기
        // 없으면 디폴트 값은 현재시간
        val watersharedPreferences = getSharedPreferences("water alarm", MODE_PRIVATE)
        val watermillis =
                watersharedPreferences.getLong("nextNotifyWaterTime", Calendar.getInstance().timeInMillis)

        val nextNotifyWaterTime: Calendar = GregorianCalendar()
        nextNotifyEatTime.timeInMillis = watermillis

        val waternextDate = nextNotifyEatTime.time
        val waterdate_text: String =
                SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(waternextDate)
        Toast.makeText(
                applicationContext,
                "[환수] 다음 알람은 " + waterdate_text + "으로 알람이 설정되었습니다!",
                Toast.LENGTH_SHORT
        ).show()

        // 이전 설정값으로 TimePicker 초기화
        val wcurrentTime = nextNotifyEatTime.time
        val wHourFormat = SimpleDateFormat("kk", Locale.getDefault())
        val wMinuteFormat = SimpleDateFormat("mm", Locale.getDefault())

        val wpre_hour: Int = wHourFormat.format(wcurrentTime).toInt()
        val wpre_minute: Int = wMinuteFormat.format(wcurrentTime).toInt()



        if (Build.VERSION.SDK_INT >= 23) {
            eatpicker!!.hour = epre_hour
            eatpicker!!.minute = epre_minute
        } else {
            eatpicker!!.currentHour = epre_hour
            eatpicker!!.currentMinute = epre_minute
        }

        if (Build.VERSION.SDK_INT >= 23) {
            ledpicker!!.hour = lpre_hour
            ledpicker!!.minute = lpre_minute
        } else {
            ledpicker!!.currentHour = lpre_hour
            ledpicker!!.currentMinute = lpre_minute
        }

        if (Build.VERSION.SDK_INT >= 23) {
            ledoffpicker!!.hour = lfpre_hour
            ledoffpicker!!.minute = lfpre_minute
        } else {
            ledoffpicker!!.currentHour = lfpre_hour
            ledoffpicker!!.currentMinute = lfpre_minute
        }

        if (Build.VERSION.SDK_INT >= 23) {
            waterpicker!!.hour = wpre_hour
            waterpicker!!.minute = wpre_minute
        } else {
            waterpicker!!.currentHour = wpre_hour
            waterpicker!!.currentMinute = wpre_minute
        }

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
//            var sendMessage = mInputEditText!!.text.toString()
            var sendMessage = "LLEDON"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        val off_sendButton = findViewById<View>(R.id.led_off_btn) as Button
        off_sendButton.setOnClickListener {
//            var sendMessage = mInputEditText!!.text.toString()
            var sendMessage = "LLEDOFF"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        val water_on_Button = findViewById<View>(R.id.water_on_btn) as Button
        water_on_Button.setOnClickListener {
//            var sendMessage = mInputEditText!!.text.toString()
            var sendMessage = "CCHANGE"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        val eat_on_Button = findViewById<View>(R.id.eat_btn) as Button
        eat_on_Button.setOnClickListener {
            var sendMessage = "EEAT"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }


        val eat_auto = findViewById<View>(R.id.eat_auto_btn) as Button
        eat_auto.setOnClickListener {
            val hour: Int
            val hour_24: Int
            val minute: Int
            val am_pm: String
            if (Build.VERSION.SDK_INT >= 23) {
                hour_24 = eatpicker!!.hour
                minute = eatpicker!!.minute
            } else {
                hour_24 = eatpicker!!.currentHour
                minute = eatpicker!!.currentMinute
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
            val editor = getSharedPreferences("eat alarm", MODE_PRIVATE).edit()
            editor.putLong("nextNotifyEatTime", calendar.timeInMillis)
            editor.apply()

            var sendMessage = "DD" + "$date_text"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        val water_auto = findViewById<View>(R.id.water_auto_btn) as Button
        water_auto.setOnClickListener {
            val hour: Int
            val hour_24: Int
            val minute: Int
            val am_pm: String
            if (Build.VERSION.SDK_INT >= 23) {
                hour_24 = waterpicker!!.hour
                minute = waterpicker!!.minute
            } else {
                hour_24 = waterpicker!!.currentHour
                minute = waterpicker!!.currentMinute
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
            val editor = getSharedPreferences("water alarm", MODE_PRIVATE).edit()
            editor.putLong("nextNotifyWaterTime", calendar.timeInMillis)
            editor.apply()

            var sendMessage = "WW" + "$date_text"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

        val ledoff_auto = findViewById<View>(R.id.led_autooff_btn) as Button
        ledoff_auto.setOnClickListener {
            val hour: Int
            val hour_24: Int
            val minute: Int
            val am_pm: String
            if (Build.VERSION.SDK_INT >= 23) {
                hour_24 = ledoffpicker!!.hour
                minute = ledoffpicker!!.minute
            } else {
                hour_24 = ledoffpicker!!.currentHour
                minute = ledoffpicker!!.currentMinute
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
            val editor = getSharedPreferences("ledoff alarm", MODE_PRIVATE).edit()
            editor.putLong("nextNotifyLedoffTime", calendar.timeInMillis)
            editor.apply()

            var sendMessage = "FF" + "$date_text"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }



        val led_auto = findViewById<View>(R.id.led_autoon_btn) as Button
        led_auto.setOnClickListener {
            val hour: Int
            val hour_24: Int
            val minute: Int
            val am_pm: String
            if (Build.VERSION.SDK_INT >= 23) {
                hour_24 = ledpicker!!.hour
                minute = ledpicker!!.minute
            } else {
                hour_24 = ledpicker!!.currentHour
                minute = ledpicker!!.currentMinute
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
            val editor = getSharedPreferences("led alarm", MODE_PRIVATE).edit()
            editor.putLong("nextNotifyLedTime", calendar.timeInMillis)
            editor.apply()

            var sendMessage = "NN" + "$date_text"
            if (sendMessage.length > 0) {
                sendMessage(sendMessage)
            }
        }

//        val next222 = findViewById<View>(R.id.next222) as Button
//        next222.setOnClickListener(object  : View.OnClickListener{
//            override fun onClick(v: View?){
//                // Activity 수정 필요
//                val intent = Intent(this@Bluetooth, WaterAcitivity::class.java)
//                startActivity(intent)
//            }})

        mPh = findViewById<View>(R.id.ph) as TextView
        mTemp = findViewById<View>(R.id.temp) as TextView


        mWebView = findViewById<View>(R.id.webcctv) as WebView?
        //웹뷰 관련
        mWebView!!.setWebViewClient(WebViewClient()) // 클릭시 새창 안뜨게
        mWebSettings = mWebView!!.getSettings() //세부 세팅 등록
        mWebSettings!!.setJavaScriptEnabled(true) // 웹페이지 자바스클비트 허용 여부
        mWebSettings!!.setSupportMultipleWindows(false) // 새창 띄우기 허용 여부
        mWebSettings!!.setJavaScriptCanOpenWindowsAutomatically(false) // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        mWebSettings!!.setLoadWithOverviewMode(true) // 메타태그 허용 여부
        mWebSettings!!.setUseWideViewPort(true) // 화면 사이즈 맞추기 허용 여부
        mWebSettings!!.setSupportZoom(false) // 화면 줌 허용 여부
        mWebSettings!!.setBuiltInZoomControls(false) // 화면 확대 축소 허용 여부
        mWebSettings!!.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL) // 컨텐츠 사이즈 맞추기
        mWebSettings!!.setCacheMode(WebSettings.LOAD_NO_CACHE) // 브라우저 캐시 허용 여부
        mWebSettings!!.setDomStorageEnabled(true) // 로컬저장소 허용 여부
        // wide viewport를 사용하도록 설정
        mWebView!!.getSettings().useWideViewPort = true
        // 컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정
        mWebView!!.getSettings().loadWithOverviewMode = true
        //zoom 허용
        mWebView!!.getSettings().builtInZoomControls = true
        mWebView!!.getSettings().setSupportZoom(true)
        //mWebView!!.loadUrl("http://www.naver.com")
        mWebView!!.loadUrl("http://172.20.10.6:8090/?action=stream"); // 웹뷰에 표시할 라즈베리파이 주소, 웹뷰 시작


        val builder = NotificationCompat.Builder(this, "default")

        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT))
        }

        var recieves: Array<String?>
        var tempnumber1 = 0.0
        var tempnumber2 = 0.0
        var phnumber1 = 0.0
        var phnumber2 = 0.0
        //핸들러 추가
        mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String((msg.obj as ByteArray), Charsets.UTF_8)
                        recieves = readMessage.split(",").toTypedArray()
                        mPh!!.setText(recieves[0])
                        mTemp!!.setText(recieves[1])
                        Log.e("무슨 값", recieves[1]+"")
                        Log.e("무슨 값일까", recieves[2]+"")
                        if(tempnumber1 != parseDouble(recieves[2])) {
                            mtemprecieve1!!.setText(recieves[2])
                            tempnumber1 = parseDouble(recieves[2])
                        }
                        if(tempnumber2 != parseDouble(recieves[3])) {
                            mtemprecieve2!!.setText(recieves[3])
                            tempnumber2 = parseDouble(recieves[3])
                        }
                        if(phnumber1 != parseDouble(recieves[4])) {
                            mphrecieve1!!.setText(recieves[4])
                            phnumber1 = parseDouble(recieves[4])
                        }
                        if(phnumber2 != parseDouble(recieves[5])) {
                            mphrecieve2!!.setText(recieves[5])
                            phnumber2 = parseDouble(recieves[5])
                        }
                        if(parseDouble(recieves[0]) < parseDouble(recieves[2])) {
                            builder.setSmallIcon(R.drawable.logo)
                            builder.setContentTitle("온도 경고")
                            builder.setContentText("설정한 온도보다 낮습니다.")
                            builder.setAutoCancel(true)
                            builder.setDefaults(Notification.DEFAULT_VIBRATE)
                            notificationManager.notify(1, builder.build());
                        }
                        if(parseDouble(recieves[0]) > parseDouble(recieves[3])) {
                            builder.setSmallIcon(R.drawable.logo)
                            builder.setContentTitle("온도 경고")
                            builder.setContentText("설정한 온도보다 높습니다.")
                            builder.setAutoCancel(true)
                            builder.setDefaults(Notification.DEFAULT_VIBRATE)
                            notificationManager.notify(2, builder.build());
                        }
                        if(parseDouble(recieves[1]) < parseDouble(recieves[4])) {
                            builder.setSmallIcon(R.drawable.logo)
                            builder.setContentTitle("PH 경고")
                            builder.setContentText("설정한 PH보다 낮습니다.")
                            builder.setAutoCancel(true)
                            builder.setDefaults(Notification.DEFAULT_VIBRATE)
                            notificationManager.notify(3, builder.build());
                        }
                        if(parseDouble(recieves[1]) > parseDouble(recieves[5])) {
                            builder.setSmallIcon(R.drawable.logo)
                            builder.setContentTitle("PH 경고")
                            builder.setContentText("설정한 PH보다 높습니다.")
                            builder.setAutoCancel(true)
                            builder.setDefaults(Notification.DEFAULT_VIBRATE)
                            notificationManager.notify(4, builder.build());
                        }
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
            }
        }



    }

    //툴바 관련 함수
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //각각의 버튼을 클릭할때의 수행할것을 정의해 준다.
        when (item.itemId) {
            R.id.action_settings -> showPairedDevicesListDialog()
        }
        return true
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

            //바이트 받는 부분 수정
            while (true) {
                if (isCancelled) return false
                val buffer = ByteArray(1024)
                var bytes: Int
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mInputStream!!.available()
                        if (bytes != 0) {
                            SystemClock.sleep(100)
                            bytes = mInputStream!!.available()
                            bytes = mInputStream!!.read(buffer, 0, bytes)
                            mHandler!!.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        break
                    }
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
//            mInputEditText!!.setText("")
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