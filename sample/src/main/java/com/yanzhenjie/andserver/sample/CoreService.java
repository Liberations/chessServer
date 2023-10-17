/*
 * Copyright © 2018 Zhenjie Yan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yanzhenjie.andserver.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;
import com.yanzhenjie.andserver.sample.util.NetUtils;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zhenjie Yan on 2018/6/9.
 */
public class CoreService extends Service {

    private Server mServer;

    @Override
    public void onCreate() {
        mServer = AndServer.webServer(this)
            .port(5000)
            .timeout(10, TimeUnit.SECONDS)
            .listener(new Server.ServerListener() {
                @Override
                public void onStarted() {
                    InetAddress address = NetUtils.getLocalIPAddress();
                    ServerManager.onServerStart(CoreService.this, address.getHostAddress());
                }

                @Override
                public void onStopped() {
                    ServerManager.onServerStop(CoreService.this);
                }

                @Override
                public void onException(Exception e) {
                    e.printStackTrace();
                    ServerManager.onServerError(CoreService.this, e.getMessage());
                }
            })
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext()).setAutoCancel(true);// 点击后让通知将消失
        mBuilder.setContentText("引擎");
        mBuilder.setContentTitle("引擎启动");
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setWhen(System.currentTimeMillis());//通知产生的时间，会在通知信息里显示
        mBuilder.setPriority(Notification.PRIORITY_DEFAULT);//设置该通知优先级
        mBuilder.setOngoing(false);//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
        mBuilder.setDefaults(Notification.DEFAULT_ALL);//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = "channelId" + System.currentTimeMillis();
            NotificationChannel channel = new NotificationChannel(channelId, getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }
        mBuilder.setContentIntent(null);
        startForeground(222, mBuilder.build());

        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopServer();
        super.onDestroy();
    }

    /**
     * Start server.
     */
    private void startServer() {
        mServer.startup();
    }

    /**
     * Stop server.
     */
    private void stopServer() {
        mServer.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}