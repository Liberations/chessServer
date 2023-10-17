package com.yanzhenjie.andserver.sample.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetUtils {
    public static void extractAssetFile(Context context, String assetFileName) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // 创建缓存目录
            File cacheDir = context.getCacheDir();
            String outputFilePath = cacheDir.getAbsolutePath() + File.separator + assetFileName;
            Log.d("testServer", "extractAssetFile: to"+outputFilePath);
            // 检查文件是否已存在
            File outputFile = new File(outputFilePath);
            if (outputFile.exists()) {
                Log.d("testServer", "文件已存在，无需解压");
                return;
            }

            // 打开 assets 中的文件
            inputStream = assetManager.open(assetFileName);

            // 创建输出流
            outputStream = new FileOutputStream(outputFilePath);

            // 逐字节复制文件内容
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            Log.d("testServer", "文件解压成功");
        } catch (IOException e) {
            Log.e("testServer", "解压文件时出错: " + e.getMessage());
        } finally {
            // 关闭输入输出流
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e("testServer", "关闭流时出错: " + e.getMessage());
            }
        }
    }
}
