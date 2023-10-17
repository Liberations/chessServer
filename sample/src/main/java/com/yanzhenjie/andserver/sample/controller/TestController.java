/*
 * Copyright 2018 Zhenjie Yan.
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
package com.yanzhenjie.andserver.sample.controller;

import static com.yanzhenjie.andserver.sample.MainActivity.ENGIN_PATH;
import static com.yanzhenjie.andserver.sample.MainActivity.LIB_PATH;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.yanzhenjie.andserver.annotation.Addition;
import com.yanzhenjie.andserver.annotation.CookieValue;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.FormPart;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PathVariable;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.PutMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.cookie.Cookie;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.http.session.Session;
import com.yanzhenjie.andserver.sample.component.LoginInterceptor;
import com.yanzhenjie.andserver.sample.model.UserInfo;
import com.yanzhenjie.andserver.sample.util.FileUtils;
import com.yanzhenjie.andserver.sample.util.Logger;
import com.yanzhenjie.andserver.util.Executors;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Zhenjie Yan on 2018/6/9.
 */
@RestController
@RequestMapping(path = "/user")
class TestController {
    private static final String TAG = "testServer";
    private static Process process;
    private static String bestMove = "";
    private Thread thread;
    private Thread engineThread;
    public TestController() {
        Log.d(TAG, "TestController: ");
        engineThread = new Thread(new Runnable() {
            @Override
            public void run() {
                initEngine();
            }
        });
        engineThread.start();
    }

    private void initEngine() {
        try {
            // 执行Shell命令
            process = Runtime.getRuntime().exec(LIB_PATH + "/libpikafish.so");
            OutputStream out = process.getOutputStream();
            Thread.sleep(1000);
            out.write("setoption name Threads value 6\n".getBytes());
            out.flush();
            out.write(("setoption name EvalFile value " + ENGIN_PATH + "\n").getBytes());
            out.flush();
            out.write("setoption name UCI_ShowWDL value false\n".getBytes());
            out.flush();
            out.write("setoption name Clear Hash\n".getBytes());
            out.flush();
            // 获取命令的输出流
            InputStream inputStream = process.getInputStream();
            // 创建输入流的读取器
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            // 逐行读取输出内容
            while ((line = reader.readLine()) != null) {
                // 处理每一行的输出
                Log.d(TAG, "cmd:" + line);
                if (line.startsWith("bestmove") && line.length() > 12) {
                    try {
                        bestMove = line.substring(9, 13);
                        Log.d(TAG, "得到bestMove" + bestMove);
                        thread.interrupt();
                    } catch (Exception e) {
                    }

                }
            }
            process.waitFor();
        } catch (Exception e) {
            Log.d(TAG, "run: " + e);
            e.printStackTrace();
        }
    }

    @PostMapping("/getBestMove")
    public String getBestMove(@RequestParam(name = "fen") String fen,
                              @RequestParam(name = "speed", defaultValue = "10") String speed) {
        Log.d(TAG, "getBestMove: fen" + fen + " speed" + speed);
        bestMove = "nobestmove";
        if (validateFEN(fen)) {
            getMove(fen, speed);
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(15000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                thread.start();
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "return bestMove"+bestMove);
        return bestMove;
    }

    private void getMove(String fen, String speed) {
        try {
            OutputStream out = process.getOutputStream();
            out.write("setoption name Threads value 6\n".getBytes());
            out.flush();
            out.write(("setoption name EvalFile value " + ENGIN_PATH + "\n").getBytes());
            out.flush();
            out.write("setoption name UCI_ShowWDL value true\n".getBytes());
            out.flush();
            out.write("setoption name Clear Hash\n".getBytes());
            out.flush();
            out.write(("position fen " + fen + "\n").getBytes());
            out.write(("go depth " + speed + "\n").getBytes());
            out.flush();
        } catch (Exception e) {
            Log.d(TAG, "getMove error: " + e);
            e.printStackTrace();
        }
    }

    public static boolean validateFEN(String fen) {
        if (TextUtils.isEmpty(fen)) return false;
        String[] fenFields = fen.split(" ");


        String boardLayout = fenFields[0];

        // 检查斜杠数量是否为9个
        int slashCount = countOccurrences(boardLayout, '/');
        if (slashCount != 9) {
            return false;
        }

        // 检查字符是否在指定范围内
        String allowedCharacters = "rnbakacpRNBAKCP/123456789";
        for (char c : boardLayout.toCharArray()) {
            if (allowedCharacters.indexOf(c) == -1) {
                return false;
            }
        }

        return true; // FEN码合法
    }

    private static int countOccurrences(String str, char target) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == target) {
                count++;
            }
        }
        return count;
    }
}