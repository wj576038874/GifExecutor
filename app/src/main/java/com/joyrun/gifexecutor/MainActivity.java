package com.joyrun.gifexecutor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.joyrun.gifcreator.FFmpegExecutor;
import com.joyrun.gifcreator.FFmpegUtils;
import com.joyrun.gifexecutor.gif.GifExtractor;
import java.io.File;
import java.lang.ref.WeakReference;


/**
 * 5.3.0话题改动
 */
public class MainActivity extends AppCompatActivity {


    private SwipeRefreshLayout swipeRefreshLayout;

    private ImageView imageView;

    private File outGifDir;//gif输出文件夹

    private File outFrameFileDir;//帧输出文件夹


    private String inputFilePath = "/storage/emulated/0/DCIM/Camera/5bcd5f4348b4c45cc084935379688b0d.mp4";
//    private String inputFilePath = "/storage/emulated/0/DCIM/Camera/VID_20191011_173051.mp4";
//    private String  inputFilePath = "/storage/emulated/0/asda.mp4";
//    private String inputFilePath = "/storage/emulated/0/DCIM/Camera/43dff20e9921e0564d2b44228f07205e.mp4";
//    private String inputFilePath = "/storage/emulated/0/DCIM/Camera/50bee567c702142d4a6facc5e0e9dac5.mp4";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        outGifDir = new File(Environment.getExternalStorageDirectory() + "/aaa_gif");
        if (!outGifDir.exists()) {
            if (outGifDir.mkdir()) {
                outGifDir = Environment.getExternalStorageDirectory();
            }
        }


        outFrameFileDir = new File(Environment.getExternalStorageDirectory() + "/aaa_frames");
        if (!outFrameFileDir.exists()) {
            boolean bol = outFrameFileDir.mkdir();
            if (!bol) {
                outFrameFileDir = Environment.getExternalStorageDirectory();
            }
        }


        swipeRefreshLayout = findViewById(R.id.srl);

        imageView = findViewById(R.id.image_gif);
    }


    /**
     * onClick
     *
     * @param view FFmpeg生成GIF
     */
    public void createGif(View view) {
        execute();
    }


    /**
     * onClick
     *
     * @param view FFmpeg取帧保存
     */
    public void executeFrames(View view) {
        swipeRefreshLayout.setRefreshing(true);
        FramesAsynctask framesAsynctask = new FramesAsynctask(this);
        framesAsynctask.execute(inputFilePath, outFrameFileDir.getAbsolutePath());
    }

    /**
     * onClick
     *
     * @param view MediaCodec生成GIF
     */
    public void createGif2(View view) {
        new Thread() {
            @Override
            public void run() {
                final String gifFile = outGifDir.getPath() + File.separator + "aaaa" + System.currentTimeMillis() + ".gif";
                GifExtractor gifExtractor = new GifExtractor(MainActivity.this, inputFilePath, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    gifExtractor.encoder(gifFile, 0, 3000, 10, 0);
                }
            }
        }.start();
    }



    /**
     * 自己写异步asyncTask  直接调用executeFFmpegCommond
     *
     * @see FFmpegExecutor#executeFFmpegCommond(String[])
     */
    private void execute() {
        //2异步调用
        swipeRefreshLayout.setRefreshing(true);
        GifCreateAsyncTask myAsyncTask = new GifCreateAsyncTask(this);
        myAsyncTask.execute(inputFilePath,outGifDir.getAbsolutePath());
    }


    /**
     * 异步调用jni生成GIF方法
     */
    private static class GifCreateAsyncTask extends AsyncTask<String, Integer, String> {

        private WeakReference<MainActivity> weakReference;

        GifCreateAsyncTask(MainActivity activity) {
            Glide.with(activity).clear(activity.imageView);
            this.weakReference = new WeakReference<>(activity);
        }


        @Override
        protected String doInBackground(String... strings) {
            String inputFilePath = strings[0];
            String outGifDir = strings[1];

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(inputFilePath);

            int width = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int videoHeight;
            int videoWidth;

//            int rotate = 0;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                rotate = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
//            }
//            //旋转过 高度和宽度反转
//            if (rotate != 0) {
//                int temp = width;
//                width = height;
//                height = temp;
//            }

            if (width > height) {
                // 横屏视屏
                videoWidth = 426;
                videoHeight = (int) ((float) videoWidth / width * height);
            } else {
                // 竖屏视频
                videoHeight = 426;
                videoWidth = (int) ((float) videoHeight / height * width);
            }


            String gifFile = outGifDir + File.separator + "FFmpeg" + System.currentTimeMillis() + ".gif";

            String[] command = FFmpegUtils.extractVideoFramesToGIF(
                    inputFilePath,
                    0,
                    2,
                    videoWidth,
                    videoHeight,
                    8,
                    gifFile);
            FFmpegExecutor fFmpegExecutor = new FFmpegExecutor();
            fFmpegExecutor.executeFFmpegCommond(command);
            return gifFile;
        }

        @Override
        protected void onPostExecute(String gifFile) {
            super.onPostExecute(gifFile);
            final MainActivity mainActivity = weakReference.get();
            if (mainActivity != null) {
                mainActivity.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(mainActivity, "success", Toast.LENGTH_SHORT).show();
                Glide.with(mainActivity).asGif().load(new File(gifFile)).into(mainActivity.imageView);
            }
        }
    }


    /**
     * FFmpeg异步调用jni取帧
     */
    private static class FramesAsynctask extends AsyncTask<String, Integer, Integer> {

        private WeakReference<MainActivity> weakReference;

        private String outFrameFileDir;

        FramesAsynctask(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }


        @Override
        protected Integer doInBackground(String... strings) {

            String inputFilePath = strings[0];
            outFrameFileDir = strings[1];

            //取帧
            String[] command = FFmpegUtils.extractVideoFramesToImages(
                    inputFilePath,
                    0,
                    3,
                    10,
                    outFrameFileDir);
            FFmpegExecutor fFmpegExecutor = new FFmpegExecutor();



            return fFmpegExecutor.executeFFmpegCommond(command);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            final MainActivity mainActivity = weakReference.get();
            if (mainActivity != null) {
                mainActivity.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(mainActivity, "取帧完成~存储在" + outFrameFileDir, Toast.LENGTH_SHORT).show();
            }
        }
    }


    //    /**
//     * 使用FFmpegHandler.execute方式 不用异步
//     *
//     * @see FFmpegHandler#execute(String[], FFmpegExecutorCallback)
//     */
//    private void execute() {
//        swipeRefreshLayout.setRefreshing(true);
//        Glide.with(MainActivity.this).clear(imageView);
//
//        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        mmr.setDataSource(inputFilePath);
//
//        int width = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
//        int height = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
//        int videoHeight;
//        int videoWidth;
//
////        int rotate = 0;
////        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
////            rotate = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
////        }
//        //旋转过 高度和宽度反转
////        if (rotate != 0) {
////            int temp = width;
////            width = height;
////            height = temp;
////        }
//
//        if (width > height) {
//            // 横屏视屏
//            videoWidth = 426;
//            videoHeight = (int) ((float) videoWidth / width * height);
//        } else {
//            // 竖屏视频
//            videoHeight = 426;
//            videoWidth = (int) ((float) videoHeight / height * width);
//        }
//
//
//        final String gifFile = outGifDir.getPath() + File.separator + "bbbb.gif";
//        String[] command = FFmpegUtils.extractVideoFramesToGIF(
//                inputFilePath,
//                0,
//                2,
//                videoWidth,
//                videoHeight,
//                8,
//                gifFile);
//        FFmpegHandler fFmpegHandler = new FFmpegHandler();
//
//        fFmpegHandler.execute(command, new FFmpegExecutorCallback() {
//            @Override
//            public void onFailure() {
//
//            }
//
//            @Override
//            public void onSuccess(String gifFile) {
//                swipeRefreshLayout.setRefreshing(false);
//                Toast.makeText(MainActivity.this, "success", Toast.LENGTH_SHORT).show();
//                Glide.with(MainActivity.this).asGif().load(new File(gifFile)).into(imageView);
//            }
//        });
//    }
}
