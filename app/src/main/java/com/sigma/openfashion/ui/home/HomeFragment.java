// res/java/com/sigma/openfashion/ui/home/HomeFragment.java
package com.sigma.openfashion.ui.home;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.sigma.openfashion.R;

import eightbitlab.com.blurview.BlurView;


/**
 * Заглушка главного экрана (HomeFragment).
 */
public class HomeFragment extends Fragment {

    private ConstraintLayout constaintLayoutBanner;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }
    ViewGroup rootView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = container;
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        float radius = 20f;

        BlurView blurView = view.findViewById(R.id.blurView);

        blurView.setupWith(rootView)
                .setBlurRadius(radius);

        ImageView imageView = view.findViewById(R.id.imageViewBanner);
        constaintLayoutBanner = view.findViewById(R.id.constaintLayoutBanner);

        Glide.with(this)
                .asBitmap()
                .load("https://hufhfmqquczxpxpmtzbf.supabase.co/storage/v1/object/public/images//image%2010.png")
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(bitmap);
                        extractDominantColor(bitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Очистка ресурсов
                    }
                });
    }

    private void extractDominantColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                if (palette != null) {
                    int dominantColor = palette.getDominantColor(Color.BLACK);
                    constaintLayoutBanner.setBackgroundColor(dominantColor);
                }
            }
        });
    }
}
