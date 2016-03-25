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
		PeerOption options = new PeerOption();
		options.key = "XXXXXXXXXXXXX";
		options.domain = "localhost";

		// Peerオブジェクトのインスタンスを生成
		_peer = new Peer(context, options);

		// コールバックを登録(OPEN)
		_peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				_id = (String) object;
				_handler.post(new Runnable() {
					@Override
					public void run() {
						// 自分のIDを表示
						TextView tv = (TextView) findViewById(R.id.tvOwnId);
                        tv.setText("ID【" + _id + "】");
						tv.invalidate();
					}
				});
			}
		});

		// コールバックを登録(CALL)
		_peer.on(Peer.PeerEventEnum.CALL, new OnCallback(){
			@Override
			public void onCallback(Object object){
				_media = (MediaConnection) object;
				_media.answer(_msLocal);
				setMediaCallback(_media);
				_bEstablished = true;
				updateUI();
			}
		});

		// コールバックを登録(ERROR)
		_peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "[On/Error]" + error);
			}
		});

		// メディアを取得
		Navigator.initialize(_peer);
		MediaConstraints constraints = new MediaConstraints();
		_msLocal = Navigator.getUserMedia(constraints);

		// 映像を表示する為のUI
		Canvas canvas = (Canvas) findViewById(R.id.svSecondary);
		canvas.addSrc(_msLocal, 0);

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
		CallOption option = new CallOption();
		_media = _peer.call(strPeerId, _msLocal, option);

		if (null != _media){
			setMediaCallback(_media);
			_bEstablished = true;
		}

		updateUI();
	}

    // ビデオ通話を終了する
    private void close(){
        if (_bEstablished) {
            _bEstablished = false;
            if (null != _media) {
                _media.close();
            }
        }
    }
    
	// メディアチャンネルのコールバック処理
	private void setMediaCallback(MediaConnection media){
		// コールバックを登録(データを受信)
		media.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				_msRemote = (MediaStream) object;
				Canvas canvas = (Canvas) findViewById(R.id.svPrimary);
				canvas.addSrc(_msRemote, 0);
			}
		});

		// コールバックを登録(チャンネルCLOSE)
		media.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				Canvas canvas = (Canvas) findViewById(R.id.svPrimary);
				canvas.removeSrc(_msRemote, 0);
				_msRemote = null;
				_media = null;
				_bEstablished = false;
				updateUI();
			}
		});
	}

	// 接続相手を選択する
	private void getPeerList(){
		if ((null == _peer) || (null == _id) || (0 == _id.length())){
			return;
		}

		_listPeerIds.clear(); // IDリストをクリア

		// シグナリングサーバから、接続状態にあるピアの一覧を取得
		_peer.listAllPeers(new OnCallback() {
			@Override
			public void onCallback(Object object) {

				// 応答のJSONデータからピアIDを抽出
				JSONArray peers = (JSONArray) object;
				for (int i = 0; peers.length() > i; i++) {
					String strValue = "";
					try {
						strValue = peers.getString(i);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// 自分以外のIDをリストに追加
					if (0 != _id.compareToIgnoreCase(strValue)) {
						_listPeerIds.add(strValue);
					}
				}

				// IDリストをダイアログで表示
				if ((null != _listPeerIds) && (0 < _listPeerIds.size())) {
					showPeerListDialog();
				}
			}
		});
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
