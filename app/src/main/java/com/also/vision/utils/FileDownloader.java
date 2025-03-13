package com.also.vision.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 文件下载工具类
 */
public class FileDownloader {
    private static final String TAG = "FileDownloader";
    private Context context;
    private DownloadTask currentTask;
    
    public FileDownloader(Context context) {
        this.context = context;
    }
    
    /**
     * 下载文件
     * @param url 文件URL
     * @param fileName 文件名
     * @param callback 下载回调
     */
    public void downloadFile(String url, String fileName, DownloadCallback callback) {
        // 创建下载目录
        File downloadDir = new File(context.getExternalFilesDir(null), "downloads");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        
        // 创建输出文件
        File outputFile = new File(downloadDir, fileName);
        
        // 如果文件已存在，直接返回成功
        if (outputFile.exists()) {
            if (callback != null) {
                callback.onDownloadComplete(outputFile);
            }
            return;
        }
        
        // 开始下载
        if (callback != null) {
            callback.onDownloadStart(fileName);
        }
        
        // 取消当前任务
        if (currentTask != null && currentTask.getStatus() != AsyncTask.Status.FINISHED) {
            currentTask.cancel(true);
        }
        
        // 创建新任务
        currentTask = new DownloadTask(url, outputFile, callback);
        currentTask.execute();
    }
    
    /**
     * 取消下载
     */
    public void cancelDownload() {
        if (currentTask != null && currentTask.getStatus() != AsyncTask.Status.FINISHED) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }
    
    /**
     * 下载任务
     */
    private class DownloadTask extends AsyncTask<Void, Integer, File> {
        private String url;
        private File outputFile;
        private DownloadCallback callback;
        private Exception exception;
        
        public DownloadTask(String url, File outputFile, DownloadCallback callback) {
            this.url = url;
            this.outputFile = outputFile;
            this.callback = callback;
        }
        
        @Override
        protected File doInBackground(Void... voids) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;
            
            try {
                URL downloadUrl = new URL(url);
                connection = (HttpURLConnection) downloadUrl.openConnection();
                connection.connect();
                
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("服务器返回错误: " + connection.getResponseCode() + " " + connection.getResponseMessage());
                }
                
                // 获取文件大小
                int fileLength = connection.getContentLength();
                
                // 下载文件
                input = connection.getInputStream();
                output = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[4096];
                long total = 0;
                int count;
                
                while ((count = input.read(buffer)) != -1) {
                    // 检查是否取消
                    if (isCancelled()) {
                        input.close();
                        output.close();
                        return null;
                    }
                    
                    total += count;
                    output.write(buffer, 0, count);
                    
                    // 更新进度
                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                }
                
                return outputFile;
            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "下载失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (callback != null) {
                callback.onDownloadProgress(values[0]);
            }
        }
        
        @Override
        protected void onPostExecute(File file) {
            if (file != null && file.exists()) {
                if (callback != null) {
                    callback.onDownloadComplete(file);
                }
            } else {
                if (callback != null) {
                    String error = (exception != null) ? exception.getMessage() : "未知错误";
                    callback.onDownloadFailed(error);
                }
                
                // 删除不完整的文件
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            }
        }
        
        @Override
        protected void onCancelled() {
            // 删除不完整的文件
            if (outputFile.exists()) {
                outputFile.delete();
            }
            
            if (callback != null) {
                callback.onDownloadFailed("下载已取消");
            }
        }
    }
    
    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        void onDownloadStart(String fileName);
        void onDownloadProgress(int progress);
        void onDownloadComplete(File file);
        void onDownloadFailed(String error);
    }
} 