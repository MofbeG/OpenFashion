package com.sigma.openfashion.ui;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true, value = {
        HeaderConfig.LOGO_ONLY,
        HeaderConfig.BUTTON_BACK,
        HeaderConfig.BUTTON_ORDER,
        HeaderConfig.BUTTON_SEARCH,
        HeaderConfig.BUTTON_MENU
})
public @interface HeaderConfig {
    int BUTTON_BACK = 1;
    int BUTTON_ORDER = 1 << 1; // 2
    int BUTTON_SEARCH = 1 << 2; // 4
    int BUTTON_MENU = 1 << 3;   // 8
    int LOGO_ONLY = 0;
    int ALL_BUTTONS = BUTTON_BACK | BUTTON_ORDER | BUTTON_SEARCH | BUTTON_MENU;
}