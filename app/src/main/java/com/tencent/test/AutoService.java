package com.tencent.test;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

/**
 * 1、确保应用已获取到悬浮窗权限Manifest.permission.SYSTEM_ALERT_WINDOW，用于功能的即时开启与关闭，部分手机可能需要手动进入权限管理中开启
 * 2、使用此服务需要获取手机特殊权限：部分手机点击本demo页面中“打开辅助服务”按钮进入辅助功能页即可找到名称为“mm”的服务，然后打开即可，
 * 其他手机需要在辅助功能中找到“无障碍”项，然后在“无障碍”中找到“mm”打开即可
 * Created by Dovar66
 */
public class AutoService extends AccessibilityService implements View.OnClickListener {
    private static final String TAG = "test";
    /**
     * 微信的包名
     */
    static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    /**
     * 推送消息显示在通知栏的关键字，设置为推送账号名,如【十点读书】
     */
    static final String PUSH_TEXT_KEY = "十点读书";
    /**
     * 推送链接的关键字，所有推送链接的标题都需要包含此关键字：如【深度好文】
     */
    private static final String URL_TEXT_KEY = "深度好文";
    /**
     * 向附近的人自动打招呼的内容
     */
    private String hello = "测试APP自动打招呼功能，这是一条测试信息";

    private boolean enableFunc2;//标记是否开启抢红包功能
    private boolean enableFunc3;//标记是否开启自动添加附近的人为好友的功能;
    private int i = 0;//记录已打招呼的人数
    private int page = 1;//记录附近的人列表页码,初始页码为1
    private int prepos = -1;//记录页面跳转来源，0--从附近的人页面跳转到详细资料页，1--从打招呼页面跳转到详细资料页


    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        //通知栏事件
        //自动打开推送的链接
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> texts = event.getText();
            if (!texts.isEmpty()) {
                for (CharSequence t : texts) {
                    String text = String.valueOf(t);
                    if (text.contains("微信红包")){
                        openNotification(event);
                    }

                    //此代码段用于实现自动打开公众号或个人推送过来的链接
                    if (text.contains(PUSH_TEXT_KEY)) {
                        openNotification(event);
                        openDelay(1000, URL_TEXT_KEY);
                    }

                }
            }
        }
        //抢红包
        if (enableFunc2) {
            if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
                //当前在红包待开页面，去拆红包
                getLuckyMoney();
            } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
                //拆完红包后看详细纪录的界面
                openNext("查看我的红包记录");
            } else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
                //在聊天界面,去点中红包
                openLuckyEnvelope();
            }
        }
        //自动加人
        if (!enableFunc3) {
            return;
        }
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getClassName().equals("com.tencent.mm.ui.LauncherUI")) {
            //记录打招呼人数置零
            i = 0;
            //当前在微信聊天页就点开发现
            openNext("发现");
            //然后跳转到附近的人
            openDelay(1000, "附近的人");
        } else if (event.getClassName().equals("com.tencent.mm.plugin.nearby.ui.NearbyFriendsUI") && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            prepos = 0;
            //当前在附近的人界面就点选人打招呼
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("米以内");
            Log.d("name", "附近的人列表人数: " + list.size());
            if (i < (list.size() * page)) {
                list.get(i % list.size()).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                list.get(i % list.size()).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else if (i == list.size() * page) {
                //本页已全部打招呼，所以下滑列表加载下一页，每次下滑的距离是一屏
                for (int i = 0; i < nodeInfo.getChild(0).getChildCount(); i++) {
                    if (nodeInfo.getChild(0).getChild(i).getClassName().equals("android.widget.ListView")) {
                        AccessibilityNodeInfo node_lsv = nodeInfo.getChild(0).getChild(i);
                        node_lsv.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        page++;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException mE) {
                                    mE.printStackTrace();
                                }
                                AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                                List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByText("米以内");
                                Log.d("name", "列表人数: " + list_.size());
                                //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
                                list_.get(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                list_.get(1).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                        }).start();
                    }
                }
            }
        } else if (event.getClassName().equals("com.tencent.mm.plugin.profile.ui.ContactInfoUI") && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (prepos == 1) {
                //从打招呼界面跳转来的，则点击返回到附近的人页面
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                i++;
            } else if (prepos == 0) {
                //从附近的人跳转来的，则点击打招呼按钮
                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                if (nodeInfo == null) {
                    Log.w(TAG, "rootWindow为空");
                    return;
                }
                List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("打招呼");
                if (list.size() > 0) {
                    list.get(list.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    list.get(list.size() - 1).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } else {
                    //如果遇到已加为好友的则界面的“打招呼”变为“发消息"，所以直接返回上一个界面并记录打招呼人数+1
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    i++;
                }
            }
        } else if (event.getClassName().equals("com.tencent.mm.ui.contact.SayHiEditUI") && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            //当前在打招呼页面
            prepos = 1;
            //输入打招呼的内容并发送
            inputHello(hello);
            openNext("发送");
        }
        //自动从桌面打开微信，利用微信多开助手可实现多个微信账号之间的切换
//        if(topActivity.equals("com.huawei.android.launcher.Launcher")){
//            openNext(event,"微信");
//            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
//            nodeInfo.getChildCount();
//            for (int i=0;i<nodeInfo.getChildCount();i++){
//                String name=nodeInfo.getChild(i).getViewIdResourceName();
//            }
//        }
    }

    /**
     * 打开通知栏消息
     */
    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }
        //将通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 点击匹配的nodeInfo
     *
     * @param str text关键字
     */
    private void openNext(String str) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            Toast.makeText(this, "rootWindow为空", Toast.LENGTH_SHORT).show();
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(str);
        Log.d("name", "匹配个数: " + list.size());
        if (list.size() > 0) {
            list.get(list.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            list.get(list.size() - 1).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            Toast.makeText(this, "找不到有效的节点", Toast.LENGTH_SHORT).show();
        }
    }

    //延迟打开界面
    private void openDelay(final int delaytime, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delaytime);
                } catch (InterruptedException mE) {
                    mE.printStackTrace();
                }
                openNext(text);
            }
        }).start();
    }

    //自动输入打招呼内容
    private void inputHello(String hello) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        //找到当前获取焦点的view
        AccessibilityNodeInfo target = nodeInfo.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (target == null) {
            Log.d(TAG, "inputHello: null");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", hello);
        clipboard.setPrimaryClip(clip);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            target.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "服务已中断", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "服务已开启", Toast.LENGTH_SHORT).show();
    }

//    private void sendNotificationEvent() {
//        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
//        if (!manager.isEnabled()) {
//            return;
//        }
//        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
//        event.setPackageName(WECHAT_PACKAGENAME);
//        event.setClassName(Notification.class.getName());
//        CharSequence tickerText = PUSH_TEXT_KEY;
//        event.getText().add(tickerText);
//        manager.sendAccessibilityEvent(event);
//    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void getLuckyMoney() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            Toast.makeText(this, "rootWindow为空", Toast.LENGTH_SHORT).show();
            return;
        }
        //bi3是本人写代码时微信拆红包的button的id,该id可能会在更新微信版本后发生变更,可通过Android Device Monitor查看获取
        //可创建一个hashMap,在微信发生版本变更时储存对应微信版本号与id值，用于适配多个微信版本
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("bi3");
//        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("拆红包");
        if (list != null && list.size() > 0) {
            list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return;
        }
        int c = nodeInfo.getChildCount();
        int i;
        for (i = 0; i < c; i++) {
            nodeInfo.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void openLuckyEnvelope() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            Toast.makeText(this, "rootWindow为空", Toast.LENGTH_SHORT).show();
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        if (list.isEmpty()) {
            Toast.makeText(this, "找不到红包", Toast.LENGTH_SHORT).show();
        } else {
            //选择聊天记录中最新的红包
            for (int i = list.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                Log.i(TAG, "-->领取红包:" + parent);
                if (parent != null) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatView();
    }

    private Button floatBtn1;
    private Button floatBtn2;

    //创建悬浮按钮
    private void createFloatView() {
        WindowManager.LayoutParams pl = new WindowManager.LayoutParams();
        WindowManager wm = (WindowManager) getSystemService(getApplication().WINDOW_SERVICE);
        pl.type = WindowManager.LayoutParams.TYPE_PHONE;
        pl.format = PixelFormat.RGBA_8888;
        pl.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        pl.gravity = Gravity.END | Gravity.BOTTOM;
        pl.x = 0;
        pl.y = 0;

        pl.width = WindowManager.LayoutParams.WRAP_CONTENT;
        pl.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(this);
        floatBtn1 = (Button) inflater.inflate(R.layout.floatbtn, null);
        floatBtn1.setText("打招呼");
        floatBtn2 = (Button) inflater.inflate(R.layout.floatbtn, null);
        floatBtn2.setText("抢红包");
        wm.addView(floatBtn1, pl);
        pl.gravity = Gravity.BOTTOM|Gravity.START;
        wm.addView(floatBtn2, pl);

        floatBtn1.setOnClickListener(this);
        floatBtn2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == floatBtn1) {
            if (enableFunc3) {
                floatBtn1.setText("打招呼");
            } else {
                floatBtn1.setText("off");
            }
            enableFunc3 = !enableFunc3;
        } else {
            if (enableFunc2) {
                floatBtn2.setText("抢红包");
            } else {
                floatBtn2.setText("off");
            }
            enableFunc2 = !enableFunc2;
        }
    }
}