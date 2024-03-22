package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.MultilineBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.ApiHistoryDialogAdapter;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
public class MultiLineApiDialog extends BaseDialog {
    private final ImageView ipQRCode;
    private final TextView ipAddress;
    private final EditText inputApi;
    private final EditText inputName;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_API_URL_CHANGE) {
            inputApi.setText((String) event.obj);
        }
        if (event.type == RefreshEvent.TYPE_LIVE_URL_CHANGE) {
            inputName.setText((String) event.obj);
        }
    }

    public MultiLineApiDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_multi_line_api);
        setCanceledOnTouchOutside(true);
        ipQRCode = findViewById(R.id.ipQRCode);
        ipAddress = findViewById(R.id.ipAddress);
        inputApi = findViewById(R.id.input_api);
        inputName = findViewById(R.id.inputName);
        inputApi.setText(Hawk.get(HawkConfig.Multiline_Api_URL, ""));
        ArrayList<MultilineBean> history = Hawk.get(HawkConfig.Multiline_API_HISTORY, new ArrayList<MultilineBean>());

        for(MultilineBean mb:history){
            String n=mb.getUrl();
            String a=Hawk.get(HawkConfig.Multiline_Api_URL, "");
            if(n.equalsIgnoreCase(a)){
                inputName.setText(mb.getName());
            }

        }


        findViewById(R.id.inputSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = inputName.getText().toString().trim();
                String newApi = inputApi.getText().toString().trim();
                MultilineBean bean=new MultilineBean();

                if(newName.isEmpty()){
                    newName=newApi;
                }
                if (!newApi.isEmpty()) {
                    bean.setName(newName);
                    if(history.isEmpty()){
                        bean.setUrl(newApi);
                    }else {
                        for (MultilineBean m : history) {
                            if (!m.getUrl().equalsIgnoreCase(newApi)) {
                                bean.setUrl(newApi);
                            }else {
                                history.remove(history.indexOf(m));
                                bean.setUrl(m.getUrl());
                            }
                        }
                    }
                    history.add(bean);

                    Hawk.put(HawkConfig.Multiline_API_HISTORY, history);
                    ApiConfig.get().setMultiUrl(newApi);
                    listener.onchange(bean);
                    dismiss();
                }
                // Capture Live input into Settings & Live History (max 20)
//                Hawk.put(HawkConfig.LIVE_URL, newLive);
//                if (!newLive.isEmpty()) {
//                    ArrayList<String> liveHistory = Hawk.get(HawkConfig.LIVE_HISTORY, new ArrayList<String>());
//                    if (!liveHistory.contains(newLive))
//                        liveHistory.add(0, newLive);
//                    if (liveHistory.size() > 20)
//                        liveHistory.remove(20);
//                    Hawk.put(HawkConfig.LIVE_HISTORY, liveHistory);
//                }

            }
        });
        findViewById(R.id.multi_apiHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (history.isEmpty())
                    return;
                String current = Hawk.get(HawkConfig.Multiline_Api_URL, "");

                ArrayList<String> list=new ArrayList<String>();
                int idx = 0;

                for(MultilineBean m:history){
                    if(m.getUrl().equals(current)){
                        idx++;
                    }
                    list.add(m.getUrl());
                }

//                if (history.contains(current))
//                    idx = history.indexOf(current);
                ApiHistoryDialog dialog = new ApiHistoryDialog(getContext());
                dialog.setTip("多仓配置历史");
                dialog.setAdapter(new ApiHistoryDialogAdapter.SelectDialogInterface() {
                    @Override
                    public void click(String value) {
                        inputApi.setText(value);
                        String name=value;
                        for(MultilineBean m:history){
                            if(m.getUrl().equalsIgnoreCase(value)){
                                name=m.getName();
                            }
                        }
                        MultilineBean b=new MultilineBean();
                        b.setName(name);
                        b.setUrl(value);
//                        ApiConfig.get().setMultiUrl(value);
                        listener.onchange(b);
                        dialog.dismiss();
                    }

                    @Override
                    public void del(String value, ArrayList<String> data) {
                        Hawk.put(HawkConfig.Multiline_API_HISTORY, data);
                    }
                }, list, idx);
                dialog.show();
            }
        });

        refreshQRCode();
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        ipAddress.setText(String.format("手机/电脑扫描上方二维码或者直接浏览器访问地址\n%s", address));
        ipQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, AutoSizeUtils.mm2px(getContext(), 300), AutoSizeUtils.mm2px(getContext(), 300)));
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onchange(MultilineBean api);
    }
}