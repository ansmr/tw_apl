import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import org.simpleframework.xml.core.Persister;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

/**
 * メイン画面
 */
public class MainActivity extends Activity implements BeaconEventNotifier, MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener  {

    private final String DATA_DIRECTORY_NAME = "HOGE";

    private final String CONFIG_BEACON_MAPPING_FILE = "beacons.xml";
    private final String CONFIG_SETTING_FILE = "settings.xml";

    private TextView message_tv;

    private String path;
    private String videoDir;

    //設定ファイルで定義しているbeaconと素材のマッピング情報
//    private HashMap<String, ResourceMappingBean>      beaconMst;

    private Config config = new Config();
//    private Beacons beacons;
//    private Settings settings;

    //再生する動画のキュー
    private final Queue<Contents> videoQueue = new LinkedList<Contents>();

    private int lastPlayVideoId = 0;

    private PlayContentsBean nowPlayContents;

    //現在処理中のビーコン(uuid)
    private String nowProcessingBeacon = "";

    private BeaconEngine  ybe = new BeaconEngine();

    //静止画表示領域
    private ImageView oImgCENTER_CROP;

    private ImageView iview;
    private VideoView vview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return;
        }

        // タイトルの非表示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //フルスクリーン表示にする
        ViewGroup   view = (ViewGroup)this.findViewById(android.R.id.content);
        view.setSystemUiVisibility(view.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | view.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | view.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | view.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | view.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | view.SYSTEM_UI_FLAG_IMMERSIVE);

        this.path = "android.resource://" + this.getPackageName() + "/" + R.raw.video_file_2;
        this.videoDir = "android.resource://" + this.getPackageName() + "/";

        oImgCENTER_CROP = (ImageView)findViewById(R.id.imageView);

        this.iview = (ImageView)findViewById(R.id.imageView);
        this.vview = (VideoView)findViewById(R.id.videoView);;

        //videoviewの動画再生準備完了のイベントリスナーをセット
        this.vview.setOnPreparedListener(this);

        //videoviewの動画再生終了のイベントリスナーをセット
        this.vview.setOnCompletionListener(this);

        //ビーコンと動画素材とのマッピングリストをxmlから取得する
        this.loadConfig();
//        this.beaconMst = this.getSettingFile(null);

        //Beaconマスタをセット
//        this.ybe.setBeaconMst(this.beaconMst);
        this.ybe.setConfig(this.config);

        //通知イベントをセット
        this.ybe.addBeaconEventNotifier(this);

        //動画の初期化
        this.initVideo();

        //実行
        this.ybe.run(this);

        //初期画像を表示する
        if (!this.config.settings.DefaultImage.equals("")) {
            this.ShowGuideImage(this.config.settings.DefaultImage);
        }

//        //TODO ビーコンが反応しない時があるので、bluetoothを停止&再起動する
//        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
//        ba.disable();
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException ex) {
//
//        }
//        ba.enable();

        //
    }

    @Override
    public void onDestroy() {
        Log.d("BeaconService", "onDestroy");
        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 戻るボタンの場合はアプリが終了しないように無視させる
        // TODO デモ用の暫定対応だがもっと良い方法があれば改善したい
        if(keyCode != KeyEvent.KEYCODE_BACK){
            return super.onKeyDown(keyCode, event);
        }else{
            return false;
        }
    }

    private void loadConfig() {
        this.config.beacons = this.loadBeacons();
        this.config.settings = this.loadSettings();
    }

    private Settings loadSettings() {
        Settings cfg = null;

        try {
            InputStream is;

            //ストレージにコンフィグファイルが存在する場合、ストレージから取得
            if (this.isExistsConfigSettings()) {
                is = new FileInputStream(this.getDataDirectoryPath() + "/config/" + this.CONFIG_SETTING_FILE);
            } else {
                is = getResources().openRawResource(R.raw.settings);
            }

            Persister persister = new Persister();
            cfg = persister.read(Settings.class, is);
        } catch (Exception e) {
            Log.d("BeaconService", e.getMessage());
        }

        return cfg;
    }

    private Beacons loadBeacons() {
        Beacons cfg = null;

        try {
            InputStream is;

            //ストレージにコンフィグファイルが存在する場合、ストレージから取得
            if (this.isExistsConfigBeacons()) {
                is = new FileInputStream(this.getDataDirectoryPath() + "/config/" + this.CONFIG_BEACON_MAPPING_FILE);
            } else {
                is = getResources().openRawResource(R.raw.beacons);
            }

            Persister persister = new Persister();
            cfg = persister.read(Beacons.class, is);
        } catch (Exception e) {
            Log.d("BeaconService", e.getMessage());
        }

        return cfg;
    }

    private boolean isExistsConfigBeacons() {
        File file = new File(this.getDataDirectoryPath() + "/config/" + this.CONFIG_BEACON_MAPPING_FILE);
        return file.exists();
    }

    private boolean isExistsConfigSettings() {
        File file = new File(this.getDataDirectoryPath() + "/config/" + this.CONFIG_SETTING_FILE);
        return file.exists();
    }


    /**
     * 動画再生 初期処理
     * 「検索中」動画を再生する
     */
    private void initVideo() {
        //videoQueue.add("video_detecting.mp4");
        showVideo();
    }

    private void ShowGuideImage(String ImageFileName) {
        //レイアウトパラム定数(縦横の長さの定数)の格納
        final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int MP = ViewGroup.LayoutParams.MATCH_PARENT;

//        //基礎画面の作成
//        LinearLayout oLayout = new LinearLayout(getApplicationContext());
//        oLayout.setOrientation(LinearLayout.VERTICAL);
//        setContentView(oLayout);

//        String  sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        Bitmap bmp = BitmapFactory.decodeFile("/storage/emulated/0/TW/images/" + ImageFileName + ".png");
        Bitmap bmp = BitmapFactory.decodeFile(this.getDataDirectoryPath() + "/images/" + ImageFileName);

        //CENTER_CROPさせる画像の生成
        //this.oImgCENTER_CROP = new ImageView(getApplicationContext());
        //oImgCENTER_CROP = new ImageView(getApplicationContext());

        ImageView   imageview = this.getImageView();

        //横幅縦幅ともにMAX
//        imageview.setLayoutParams(new LinearLayout.LayoutParams(MP, MP));
//        //oImgCENTER_CROP.setImageResource(R.drawable.detecting);
        imageview.setImageBitmap(bmp);
//        imageview.setScaleType(ImageView.ScaleType.CENTER_INSIDE );
//        //oLayout.addView(oImgCENTER_CROP);

//        imageview.setVisibility(View.VISIBLE);

        this.EnableGuideImage();
        this.DisEnableVideoView();
    }

    private void EnableGuideImage() {
        ImageView   imageview = this.getImageView();
        imageview.setVisibility(View.VISIBLE);
    }

    private void DisEnableGuideImage() {
        ImageView   imageview = this.getImageView();
        imageview.setVisibility(View.GONE);
    }

    private void EnableVideoView() {
        VideoView videoview = this.getVideoView();
        videoview.setVisibility(View.VISIBLE);
    }

    private void DisEnableVideoView() {
        VideoView videoview = this.getVideoView();
        videoview.setVisibility(View.GONE);
    }

    @Override
    public void onRangeEnter(MyBeaconBean beacon) {
        Log.d("BeaconService", "onRangeEnter:" + beacon.uuid);

        //受信したビーコンが現在処理中のビーコンの場合、なにも処理をしない
        if (beacon.uuid.equals(nowProcessingBeacon)) {
            return;
        }

        //再生すべき動画を取得する
        this.nowPlayContents = ybe.getPlayContentsByEnterBeacon(beacon);
        //String videofile = beaconMst.get(beacon.uuid).fileNamesBean.fileName;

        //キューを初期化し、再生する動画をセットする
        //videoQueue.clear();
        this.clearQueue();

        this.addQueue(this.nowPlayContents.getContentsList());
//        for (Contents c : this.nowPlayContents.getContentsList()) {
//            videoQueue.add(c);
//            Log.d("BeaconService", "キュー追加:" + c);
//        }

        //このuuidを現在処理中とする
        nowProcessingBeacon = beacon.uuid;

        //動画再生
        showVideo();
    }

    private void clearQueue() {
        this.videoQueue.clear();
    }

    private void addQueue(ArrayList<Contents> list) {
        for (Contents c : list) {
            this.videoQueue.add(c);
        }
    }

    private boolean isQueueEmpty() {
        return this.videoQueue.isEmpty();
    }

    private Contents pollQueue() {
        if (this.isQueueEmpty()) {
            return null;
        }

        return this.videoQueue.poll();
    }

    @Override
    public void onRangeExit() {
        Log.d("BeaconService", "onRangeExit");

        //ビーコン未処理状態にセットする
//        nowProcessingBeacon = "";

        //キューを初期化し、再生する動画をセットする
        this.clearQueue();

        //動画を停止する
//        this.stopMovie();

        //デフォルト画像を表示する
//        this.ShowGuideImage(this.config.settings.DefaultImage);
    }

    /**
     * 動画再生処理
     *
     * キューに入っている順に動画を再生する
     * キューが空になったら「検索中」動画を再生する
     * 既に動画を再生中の場合は、割り込んで動画を再生する
     */
    private void showVideo() {
        this.DisEnableGuideImage();
        this.EnableVideoView();
//
//
//
////        final VideoView videoview = (VideoView)findViewById(R.id.videoView);
//        final VideoView videoview = this.getVideoView();
//        final Context con = this;

        if (this.isQueueEmpty()) {
            //キューがない場合
            this.stopMovie();

            //デフォルト画像を表示する
            this.ShowGuideImage(this.config.settings.DefaultImage);
        } else {
            //キューがあれば動画を再生する
            Contents c = this.pollQueue();

            if (c.isImage()) {
                this.ShowGuideImage(c.filename);
            }
            if (c.isMovie()) {
                this.playMovie(c);
            }
        }

//        //videoviewの再生準備完了のイベント
//        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                videoview.start();
//            }
//        });

//        //videoviewの再生終了のイベント
//        //再生終了後、キューから次の動画を取り出し、再生する
//        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                String  videoName;
//
//                //キューが空の場合、「検索中」を再生する
//                if (videoQueue.isEmpty()) {
////                    //ビーコン未処理状態にセットする
////                    nowProcessingBeacon = "";
//
//                    if (nowPlayContents != null) {
//                        if (nowPlayContents.GuidImageFile.equals("") == false) {
//                            ShowGuideImage(nowPlayContents.GuidImageFile);
//                        }
//                    }
////                    videoName = "video_detecting.mp4";
//                } else {
//                    videoName = videoQueue.poll();
//
//                    //再生するリソースをセット
//                    int id = videoNameToResourceId(con, videoName);
//                    videoview.setVideoPath(getDataDirectoryPath() + "/videos/" + videoName);
//
//                    lastPlayVideoId = id;
//                }
//            }
//        });
    }

    /**
     * 動画を再生する
     *
     * @param c
     */
    private void playMovie(Contents c) {
        VideoView videoview = this.getVideoView();

        if (videoview.isPlaying()) {
            videoview.stopPlayback();
        }

        //コンテンツファイルが実際に存在する場合のみ、再生する
        if (!existsContentsFile(c)) {
            Log.d("BeaconService", "コンテンツファイルが存在しません。:" + c.filename);
            return;
        }

        videoview.setVideoPath(getContentsFileName(c));
    }

    /**
     * 動画を停止する
     */
    private void stopMovie() {
        VideoView videoview = this.getVideoView();

        if (videoview.isPlaying()) {
            videoview.stopPlayback();
        }
    }

    private String getContentsFileName(Contents c) {
        String path = this.getDataDirectoryPath();

        if (c.isMovie()) {
            path += "/videos/";
        } else if (c.isImage()) {
            path += "/images/";
        }

        return path += c.filename;
    }

    private boolean existsContentsFile(Contents c) {
        String filename = getContentsFileName(c);
        File    f = new File(filename);

        return f.exists();
    }


    /**
     * videoviewの再生準備完了のイベント
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        final VideoView videoview = this.getVideoView();
        videoview.start();
    }

    /**
     * videoviewの動画再生完了のイベント
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
//        String  videoName;
//
//        //キューが空の場合、「検索中」を再生する
//        if (videoQueue.isEmpty()) {
////                    //ビーコン未処理状態にセットする
////                    nowProcessingBeacon = "";
//
//            if (nowPlayContents != null) {
//                if (nowPlayContents.GuidImageFile.equals("") == false) {
//                    ShowGuideImage(nowPlayContents.GuidImageFile);
//                }
//            }
////                    videoName = "video_detecting.mp4";
//        } else {
//            videoName = videoQueue.poll();
//
//            //再生するリソースをセット
//            int id = videoNameToResourceId(con, videoName);
//            videoview.setVideoPath(getDataDirectoryPath() + "/videos/" + videoName);
//
//            lastPlayVideoId = id;
//        }

        this.showVideo();
    }


    /**
     * 動画名からリソースIDを取得する
     * @param context
     * @param videoName
     * @return
     */
    public final int videoNameToResourceId(Context context, String videoName) {
        return this.resourceNameToResourceId(context, videoName, "raw");
    }

    /**
     * リソースフォルダとリソース名からリソースIDを取得する
     * @param context
     * @param name
     * @param folder
     * @return
     */
    private int resourceNameToResourceId(Context context, String name, String folder) {
        return context.getResources().getIdentifier(name, folder, context.getPackageName());
    }

    private VideoView getVideoView() {
        return this.vview;
    }

    private ImageView getImageView() {
        return this.iview;
    }

//    /**
//     * 素材マッピング用の設定ファイルを読み込み、map形式で返す
//     */
//    private HashMap<String, ResourceMappingBean> getSettingFile(XmlPullParser beacons) {
//        int eventType = -1;
//        String strUuid = null;
//        String strFileName = null;
//        String strLoopFlg = null;
//        ResourceFilenamesBean fileNameBean = null;
//        int priority = 0;
//        String previous = null;
//        String previousGuidImage = null;
//        String next = null;
//        String nextGuildImage = null;
//        String error = null;
//        ResourceMappingBean bean = null;
//
//        HashMap<String, ResourceMappingBean>      beaconMst = new HashMap<String, ResourceMappingBean>();
//
////        XmlResourceParser beacons = getResources().getXml(R.xml.beacons);
//        try {
//            while(eventType != XmlResourceParser.END_DOCUMENT) {
//                if (beacons.getEventType() == XmlResourceParser.START_TAG) {
//                    String s = beacons.getName();
//
//                    if (s.equals("beacon")) {
//                        bean = new ResourceMappingBean();
//                        fileNameBean = new ResourceFilenamesBean();
//                        beacons.next();
//                        if(beacons.getName() != null && beacons.getName().equals("uuid")){
//                            strUuid = beacons.nextText();
//                            beacons.next();
//                            strFileName = beacons.nextText();
//                            beacons.next();
//                            strLoopFlg = beacons.nextText();
//                            beacons.next();
//                            priority = Integer.parseInt(beacons.nextText());
//                            beacons.next();
//                            previous = beacons.nextText();
//                            beacons.next();
//                            previousGuidImage = beacons.nextText();
//                            beacons.next();
//                            next = beacons.nextText();
//                            beacons.next();
//                            nextGuildImage = beacons.nextText();
//                            beacons.next();
//                            error = beacons.nextText();
//                        }
//
//                        bean.uuid = strUuid;
//                        fileNameBean.fileName = strFileName;
//                        fileNameBean.isLoopFlg = strLoopFlg.equals("true") ? true : false;
//                        bean.fileNamesBean = fileNameBean;
//                        bean.priority = priority;
//                        bean.previouts = removeEmpty(previous.replaceAll(" ", "").split(",", 0));
//                        bean.previousGuildImage = previousGuidImage;
//                        bean.next = removeEmpty(next.replaceAll(" ", "").split(",", 0));
//                        bean.nextGuildImage = nextGuildImage;
//                        bean.error = error;
//
//                        beaconMst.put(bean.uuid, bean);
//                    }
//                }
//                eventType = beacons.next();
//            }
//        } catch (XmlPullParserException e) {
//            e.printStackTrace();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return beaconMst;
//    }

    /**
     * 配列からempty要素を削除する
     * @param arr
     * @return
     */
    private String[] removeEmpty(String[] arr) {
        ArrayList<String>    list = new ArrayList<String>();

        for (String s : arr) {
            if (s.equals("") == false) {
                list.add(s);
            }
        }

        return (String[])list.toArray(new String[list.size()]);
    }

    /**
     * YBEが使用するデータが格納されているパスを返す
     * @return
     */
    private String getDataDirectoryPath() {
        String  path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + this.DATA_DIRECTORY_NAME;
        return path;
    }
}
