package com.example.music;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener {
    public static final String ACTION_MUSIC_START =
            "com.glriverside.xgqin.ggmusic.ACTION_MUSIC_START";
    public static final String ACTION_MUSIC_STOP =
            "com.glriverside.xgqin.ggmusic.ACTION_MUSIC_STOP";
    private MusicReceiver musicReceiver;

    public static final int UPDATE_PROGRESS = 1;
    private ProgressBar pbProgress;

    private MusicService mService;
    private boolean mBound = false;

    public static final String TITLE =
            "com.glriverside.xgqin.ggmusic.TITLE";
    public static final String ARTIST =
            "com.glriverside.xgqin.ggmusic.ARTIST";
    public static final String DATA_URI =
            "com.glriverside.xgqin.ggmusic.DATA_URI";

    private Boolean mPlayStatus = true;
    private ImageView ivPlay;
    MediaCursorAdapter mCursorAdapter;
    private ContentResolver mContentResolver;
    private ListView mPlaylist;
    private final int REQUEST_EXTERNAL_STORAGE =1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final String SELECTION =
            MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " +
                    MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String[] SELECTION_ARGS = {
            Integer.toString(1),
            "audio/mpeg"
    };

    private BottomNavigationView navigation;
    private TextView tvBottomTitle;
    private TextView tvBottomArtist;
    private ImageView ivAlbumThumbnail;

    private MediaPlayer  mMediaPlayer= null;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_PROGRESS:
                    int position = msg.arg1;
                    pbProgress.setProgress(position);
                    break;
                default:
                    break;
            }
        }
    };


//    @RequiresApi(api = Build.VERSION_CODES.M)

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicServiceBinder binder =
                    (MusicService.MusicServiceBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MUSIC_START);
        intentFilter.addAction(ACTION_MUSIC_STOP);
        registerReceiver(musicReceiver,intentFilter);

        mContentResolver = getContentResolver();
        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);
        mPlaylist = findViewById(R.id.lv_playlist);
        mPlaylist.setAdapter(mCursorAdapter);
        mPlaylist.setOnItemClickListener(MainActivity.this.itemClickListener);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            }else{ requestPermissions(PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        }else{
            initPlaylist();
        }

        navigation = findViewById(R.id.navigation);
        LayoutInflater.from(this).inflate(R.layout.bottom_media_toolbar,navigation,true);
        ivPlay = navigation.findViewById(R.id.iv_play);
        tvBottomArtist = navigation.findViewById(R.id.tv_bottom_artist);
        tvBottomTitle = navigation.findViewById(R.id.tv_bottom_title);
        ivAlbumThumbnail = navigation.findViewById(R.id.iv_thumbnail);

        if (ivPlay !=null){
            ivPlay.setOnClickListener(MainActivity.this);
        }

        navigation.setVisibility(View.GONE);
    }

    private void initPlaylist(){
        Cursor mCursor = mContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                SELECTION,
                SELECTION_ARGS,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
        mCursorAdapter.swapCursor(mCursor);
        mCursorAdapter.notifyDataSetChanged();
    }

    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults){
        switch (requestCode){
            case REQUEST_EXTERNAL_STORAGE:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    initPlaylist();
                }
                break;
            default:break;
        }
    }

    protected void onStart(){
        super.onStart();
//        if(mMediaPlayer==null){
//            mMediaPlayer = new MediaPlayer();
//        }
        Intent intent = new Intent(this,MusicService.class);
        bindService(intent,mConn, Context.BIND_AUTO_CREATE);
    }

    protected void onStop(){
        unbindService(mConn);
        mBound = false;
//        if(mMediaPlayer!=null){
//            mMediaPlayer.stop();
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//        }
        super.onStop();
    }

    protected void onDestroy(){
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    private ListView.OnItemClickListener itemClickListener
            = new ListView.OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> adapterView,View view, int i,long l) {
            Cursor cursor = mCursorAdapter.getCursor();
            if(cursor!= null&&cursor.moveToPosition(i)){
                int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                int dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                String title = cursor.getString(titleIndex);
                String artist = cursor.getString(artistIndex);
                Long albumId = cursor.getLong(albumIndex);
                String data = cursor.getString(dataIndex);

                Uri dataUri = Uri.parse(data);
                Intent serviceIntent = new Intent(MainActivity.this,MusicService.class);
                serviceIntent.putExtra(MainActivity.DATA_URI,data);
                serviceIntent.putExtra(MainActivity.TITLE,title);
                serviceIntent.putExtra(MainActivity.ARTIST,artist);
                startService(serviceIntent);

                if(mMediaPlayer !=null){
                    try{
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(
                                MainActivity.this,dataUri
                        );
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                navigation.setVisibility(View.VISIBLE);

                if(tvBottomTitle !=null){
                    tvBottomTitle.setText(title);
                }
                if(tvBottomArtist !=null){
                    tvBottomArtist.setText(artist);
                }

                Uri albumUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumId
                );

                Cursor albumCursor = mContentResolver.query(
                        albumUri,
                        null,
                        null,
                        null,
                        null
                );
                if(albumCursor!=null&&albumCursor.getCount()>0){
                    albumCursor.moveToFirst();
                    int albumArtIndex = albumCursor.getColumnIndex(
                            MediaStore.Audio.Albums.ALBUM_ART
                    );
                    String albumArt = albumCursor.getString(albumArtIndex);
                    Glide.with(MainActivity.this).load(albumArt).into(ivAlbumThumbnail);
                    albumCursor.close();
                }

            }
        }
    };

    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.iv_play){
            mPlayStatus = !mPlayStatus;
            if(mPlayStatus == true){
                mService.play();
                ivPlay.setImageResource(R.drawable.ic_play_on);
            }else{
                mService.pause();
                ivPlay.setImageResource(R.drawable.ic_play_off);
            }
        }
    }

    private class MusicProgressRunnable implements Runnable{
        public MusicProgressRunnable(){}

        public void run(){
            boolean mThreadWorking = true;
            while (mThreadWorking){
                try {
                    if(mService !=null){
                        int position =
                                mService.getCurrentPosition();
                        Message message = new Message();
                        message.what = UPDATE_PROGRESS;
                        message.arg1 = position;
                        mHandler.sendMessage(message);
                    }
                    mThreadWorking = mService.isPlaying();
                    Thread.sleep(100);
                }catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }
    }

    public class MusicReceiver extends BroadcastReceiver {
        public void onReceive(Context context,Intent intent){
            pbProgress = findViewById(R.id.progress);
            if(mService !=null){
                pbProgress.setMax(mService.getDuration());

                new Thread(new MusicProgressRunnable()).start();
            }
        }
    }
}