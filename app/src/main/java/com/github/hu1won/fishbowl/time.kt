package com.github.hu1won.fishbowl

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class time : AppCompatActivity() {

    var picker: TimePicker? = null
    
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

        
    }

}