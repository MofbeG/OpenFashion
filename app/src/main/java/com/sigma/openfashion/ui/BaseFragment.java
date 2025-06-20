package com.sigma.openfashion.ui;

import android.view.View;

import androidx.fragment.app.Fragment;

import com.sigma.openfashion.R;

public abstract class BaseFragment extends Fragment {

    public BaseFragment() {
    }

    public BaseFragment(int contentLayoutId) {
        super(contentLayoutId);
    }

    protected void setHeaderVisibility(boolean visible) {
        View blurHeader = requireActivity().findViewById(R.id.blurHeader);
        if (blurHeader != null) {
            if (visible) {
                blurHeader.setVisibility(View.VISIBLE);
                blurHeader.animate().alpha(1f).setDuration(200).start();
            } else {
                blurHeader.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> blurHeader.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    public void setupHeader(@HeaderConfig int config) {
        setVisibility(R.id.buttonBack, (config & HeaderConfig.BUTTON_BACK) != 0);
        setVisibility(R.id.buttonOrder, (config & HeaderConfig.BUTTON_ORDER) != 0);
        setVisibility(R.id.buttonSearch, (config & HeaderConfig.BUTTON_SEARCH) != 0);
        setVisibility(R.id.buttonMenu, (config & HeaderConfig.BUTTON_MENU) != 0);
    }

    private void setVisibility(int viewId, boolean visible) {
        requireActivity().findViewById(viewId).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected void runOnUi(Runnable block) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(block);
        }
    }
}