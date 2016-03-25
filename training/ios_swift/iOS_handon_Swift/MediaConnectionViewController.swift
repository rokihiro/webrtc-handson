//
//  MediaConnectionViewController.swift
//  iOS_handon_Swift
//
//  Created by Hiroki Kato on 2016/03/23.
//  Copyright © 2016年 ntt.com. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation


class MediaConnectionViewController: UIViewController {
    
    private var _peer: SKWPeer?
    private var _msLocal: SKWMediaStream?
    private var _msRemote: SKWMediaStream?
    private var _mediaConnection: SKWMediaConnection?
    private var _id: String? = nil
    private var _bEstablished: Bool = false
    private var _listPeerIds: Array<String> = []
    @IBOutlet weak var idLabel: UILabel!
    @IBOutlet weak var callButton: UIButton!

    
    
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        self.view.backgroundColor = UIColor.whiteColor()

        setupUI()
        
        if nil != self.navigationController {
            self.navigationController?.delegate = self
        }
        
        /////////////////////////////////////////////////////////////
        /////////////////////  2.1．サーバへ接続  /////////////////////
        ////////////////////////////////////////////////////////////
        
        //APIキー、ドメインを設定
        
        
        // Peerオブジェクトのインスタンスを生成

        
        
        ///////////////////////////////////////////////////////////////
        /////////////////////  2.2．接続成功・失敗  /////////////////////
        //////////////////////////////////////////////////////////////
        
        //コールバックを登録（ERROR)

        
        // コールバックを登録(OPEN)

        
        ///////////////////////////////////////////////////////////////
        /////////////////////  2.3．メディアの取得  /////////////////////
        //////////////////////////////////////////////////////////////
        
        //メディアを取得

        
        //映像を表示する為のUI

        
        
        

        ////////////////////////////////////////////////////////////
        /////////////////////  2.4.相手から着信  /////////////////////
        ////////////////////////////////////////////////////////////
        
        //コールバックを登録（CALL)


    }
    
    func setMediaCallbacks(media:SKWMediaConnection){

    }
    
    
    
    ///////////////////////////////////////////////////////////////////
    /////////////////////  2.5.　相手へのビデオ発信　/////////////////////
    //////////////////////////////////////////////////////////////////
    
    
    func getPeerList(){

    }
    
    //ビデオ通話を開始する
    func call(strDestId: String) {

    }

    //ビデオ通話を終了する
    func closeChat(){

    }
    
    
    func showPeerDialog(){
        let vc:PeerListViewController = PeerListViewController()
        vc.items = _listPeerIds
        vc.callback = self
        
        let nc:UINavigationController = UINavigationController.init(rootViewController: vc)
 
        dispatch_async(dispatch_get_main_queue(), {
            self.presentViewController(nc, animated: true, completion: nil)
        })
        
    }

    
    
    
    //////////////////////////////////////////////////////////////////
    /////////////////////  2.6.　UIのセットアップ  /////////////////////
    /////////////////////////////////////////////////////////////////
    
    
    func setupUI(){
        
        let rcScreen:CGRect = self.view.bounds;
        
        
        //ローカルビデオ用のView
        var rcLocal:CGRect = CGRectZero;
        rcLocal.size.width = rcScreen.size.height / 5;
        rcLocal.size.height = rcLocal.size.width;
        
        rcLocal.origin.x = rcScreen.size.width - rcLocal.size.width - 8.0;
        rcLocal.origin.y = rcScreen.size.height - rcLocal.size.height - 8.0;
        rcLocal.origin.y -= (self.navigationController?.toolbar.frame.size.height)!
        
        
        let vwVideo:SKWVideo = SKWVideo.init(frame: rcLocal)
        vwVideo.tag = ViewTag.TAG_LOCAL_VIDEO.hashValue
        self.view.addSubview(vwVideo)
        
        
        //リモートビデオ用のView
        var rcRemote:CGRect = CGRectZero;
        rcRemote.size.width = rcScreen.size.width;
        rcRemote.size.height = rcRemote.size.width;
        
        rcRemote.origin.x = (rcScreen.size.width - rcRemote.size.width) / 2.0;
        rcRemote.origin.y = (rcScreen.size.height - rcRemote.size.height) / 2.0;
        rcRemote.origin.y -= 8.0;
        
        //Remote SKWVideo
        let vwRemote:SKWVideo = SKWVideo.init(frame: rcRemote)
        vwRemote.tag = ViewTag.TAG_LOCAL_VIDEO.hashValue
        vwRemote.hidden = true
        self.view.addSubview(vwVideo)
        vwRemote.tag = ViewTag.TAG_REMOTE_VIDEO.hashValue
        vwRemote.hidden = true
        self.view.addSubview(vwRemote)
        
        self.updateUI();
    }
    
    @IBAction func pushCallButton(sender: AnyObject) {
        
        if self._mediaConnection == nil {
            self.getPeerList()
        }else{
            self.performSelectorInBackground("closeChat", withObject: nil)
        }
    }
    
    
    func updateUI(){
        dispatch_async(dispatch_get_main_queue()) { () -> Void in
            
            //CALLボタンのアップデート
            if self._bEstablished == false{
                self.callButton.titleLabel?.text = "  CALL  "
            }else{
                self.callButton.titleLabel?.text = "Hang up"
            }
            
            //IDラベルのアップデート
            if self._id == nil{
                self.idLabel.text = "your Id:"
            }else{
                self.idLabel.text = "your Id:"+self._id! as String
            }
        }
    }
    
    

    
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
}

//////////////////////////////////////////////////////////////
/////////////////////  ハンズオンここまで  /////////////////////
////////////////////////////////////////////////////////////

enum ViewTag : UInt {
    case TAG_ID = 1000
    case TAG_WEBRTC_ACTION
    case TAG_REMOTE_VIDEO
    case TAG_LOCAL_VIDEO
}

extension MediaConnectionViewController: UINavigationControllerDelegate, UIAlertViewDelegate {
}