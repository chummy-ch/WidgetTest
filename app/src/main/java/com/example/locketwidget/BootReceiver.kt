package com.example.locketwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.locketwidget.work.WidgetWorkUseCase
import org.koin.java.KoinJavaComponent.inject

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
            val workUseCase: WidgetWorkUseCase by inject(WidgetWorkUseCase::class.java)
            workUseCase.createScheduleWork()
        }
    }
}