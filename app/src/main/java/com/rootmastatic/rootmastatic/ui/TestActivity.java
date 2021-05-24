package com.rootmastatic.rootmastatic.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.rootmastatic.rootmastatic.bean.RMSEnum;
import com.rootmastatic.rootmastatic.core.RMS;
import com.rootmastatic.rootmastatic.core.RootMaAnno;

/*
 * Created by Administrator on 2021/5/24.
 */
@RootMaAnno(module = "MODULE_1", page = "PAGE_1")
public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RMS.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RMS.page(RMSEnum.START);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RMS.page(RMSEnum.END);
        RMS.click("BUTTON_0");
    }
}
