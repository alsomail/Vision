package com.also.vision.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.also.vision.R;
import com.also.vision.model.DeviceFile;
import com.bumptech.glide.Glide;

import java.util.List;

public class FileListAdapter extends BaseAdapter {
    private Context context;
    private List<DeviceFile> fileList;
    
    public FileListAdapter(Context context, List<DeviceFile> fileList) {
        this.context = context;
        this.fileList = fileList;
    }
    
    @Override
    public int getCount() {
        return fileList.size();
    }
    
    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
            holder = new ViewHolder();
            holder.ivThumbnail = convertView.findViewById(R.id.ivThumbnail);
            holder.tvFileName = convertView.findViewById(R.id.tvFileName);
            holder.tvFileInfo = convertView.findViewById(R.id.tvFileInfo);
            holder.ivFileType = convertView.findViewById(R.id.ivFileType);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        DeviceFile file = fileList.get(position);
        
        // 设置文件名
        holder.tvFileName.setText(file.getFileName());
        
        // 设置文件信息（大小和日期）
        String fileSize = Formatter.formatFileSize(context, file.getFileSize());
        String fileDate = DateFormat.format("yyyy-MM-dd HH:mm", file.getCreateTime()).toString();
        
        if (file.getFileType() == DeviceFile.TYPE_VIDEO && file.getDuration() > 0) {
            int minutes = file.getDuration() / 60;
            int seconds = file.getDuration() % 60;
            holder.tvFileInfo.setText(String.format("%s • %s • %02d:%02d", fileSize, fileDate, minutes, seconds));
        } else {
            holder.tvFileInfo.setText(String.format("%s • %s", fileSize, fileDate));
        }
        
        // 设置文件类型图标
        if (file.getFileType() == DeviceFile.TYPE_VIDEO) {
            holder.ivFileType.setImageResource(R.drawable.ic_video);
        } else {
            holder.ivFileType.setImageResource(R.drawable.ic_photo);
        }
        
        // 加载缩略图
        Glide.with(context)
                .load(file.getThumbnailUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(holder.ivThumbnail);
        
        return convertView;
    }
    
    private static class ViewHolder {
        ImageView ivThumbnail;
        TextView tvFileName;
        TextView tvFileInfo;
        ImageView ivFileType;
    }
} 