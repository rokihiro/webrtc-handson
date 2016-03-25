package io.skyway.testpeerjava;

import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;
import io.skyway.Peer.PeerOption;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * ビデオチャット画面
 *
 */

public class MediaActivity extends Activity {
	private static final String TAG = MediaActivity.class.getSimpleName();

	private Peer            _peer;
	private MediaConnection _media;
	private MediaStream 	_msLocal;
	private MediaStream 	_msRemote;
	private Handler 		_handler;
	private String 			_id;
	private List<String> 	_listPeerIds;
	private boolean 		_bEstablished;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Window wnd = getWindow();
		wnd.addFlags(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_media);

		_bEstablished = false;
		_handler = new Handler(Looper.getMainLooper());
		_listPeerIds = new ArrayList<>();
		Context context = getApplicationContext();

		// APIキー、ドメインを設定

		// Peerオブジェクトのインスタンスを生成

		// コールバックを登録(OPEN)

		// コールバックを登録(CALL)

		// コールバックを登録(ERROR)

		// メディアを取得

		// 映像を表示する為のUI

		// アクションボタン
        Button btnAction = (Button) findViewById(R.id.btnAction);
        btnAction.setEnabled(true);
        btnAction.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                v.setEnabled(false);
                if (!_bEstablished){
                    getPeerList();
                }
                else{
                    close();
                }
                v.setEnabled(true);
            }
        });
	}

	// ビデオ通話をかける
	private void call(String strPeerId){

	}

    // ビデオ通話を終了する
    private void close(){

    }

	// メディアチャンネルのコールバック処理
	private void setMediaCallback(MediaConnection media){

	}

	// 接続相手を選択する
	private void getPeerList(){

	}

	// UIの表示を更新する
	private void updateUI() {
		_handler.post(new Runnable() {
			@Override
			public void run() {
				Button btnAction = (Button) findViewById(R.id.btnAction);
				if (null != btnAction) {
					if (false == _bEstablished) {
						btnAction.setText("Call");
					} else {
						btnAction.setText("Hang up");
					}
				}

				TextView tvOwnId = (TextView) findViewById(R.id.tvOwnId);
				if (null != tvOwnId) {
					if (null == _id) {
						tvOwnId.setText("");
					} else {
						tvOwnId.setText(_id);
					}
				}
			}
		});
	}

	// 接続中のピアの一覧を表示する
	void showPeerListDialog(){
		_handler.post(new Runnable() {
			@Override
			public void run() {
				FragmentManager mgr = getFragmentManager();
				ListDialogFragment dialog = new ListDialogFragment();
				dialog.setListener(
						new ListDialogFragment.PeerListDialogFragmentListener() {
							@Override
							public void onItemClick(final String item) {
								_handler.post(new Runnable() {
									@Override
									public void run() {
										call(item);
									}
								});
							}
						});
				String[] peerIds = _listPeerIds.toArray(new String[0]);
				dialog.setItems(peerIds);
				dialog.show(mgr, "peerlist");
			}
		});
	}

	//
	// ライフサイクル・メソッド
	//

	@Override
	protected void onStart(){
		super.onStart();
		// Disable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume(){
		super.onResume();
		// Set volume control stream type to WebRTC audio.
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}

	@Override
	protected void onPause(){
		// Set default volume control stream type.
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
		super.onPause();
	}

	@Override
	protected void onStop(){
		// Enable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		super.onStop();
	}

}
