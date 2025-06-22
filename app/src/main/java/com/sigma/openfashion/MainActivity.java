package com.sigma.openfashion;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.sigma.openfashion.data.product.Gender;

import eightbitlab.com.blurview.BlurView;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        NavController navController = navHostFragment.getNavController();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.nav_profile) {
               navController.navigate(R.id.profileFragment);
            } else if (id == R.id.nav_order) {
                navController.navigate(R.id.orderFragment);
            } else if (id == R.id.nav_mans) {
                Bundle args = new Bundle();
                args.putString("gender", Gender.MALE.name());
                navController.navigate(R.id.productsListFragment, args);
            } else if (id == R.id.nav_woman) {
                Bundle args = new Bundle();
                args.putString("gender", Gender.FEMALE.name());
                navController.navigate(R.id.productsListFragment, args);
            } else if (id == R.id.nav_all) {
                navController.navigate(R.id.productsListFragment);
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        ImageButton orderButton = findViewById(R.id.buttonOrder);
        orderButton.setOnClickListener(view -> {
            navController.navigate(R.id.orderFragment);
        });

        ImageButton searchButton = findViewById(R.id.buttonSearch);
        searchButton.setOnClickListener(view -> {
            ConstraintLayout mainTitle = findViewById(R.id.mainTitle);
            mainTitle.setVisibility(View.GONE);
            ConstraintLayout serchTitle = findViewById(R.id.serchTitle);
            serchTitle.setVisibility(View.VISIBLE);
        });

        ImageButton buttonSearchingCancel = findViewById(R.id.buttonsc);
        buttonSearchingCancel.setOnClickListener(view -> {
            ConstraintLayout mainTitle = findViewById(R.id.mainTitle);
            mainTitle.setVisibility(View.VISIBLE);
            ConstraintLayout serchTitle = findViewById(R.id.serchTitle);
            serchTitle.setVisibility(View.GONE);
        });

        ImageButton buttonSearching = findViewById(R.id.buttonSearching);
        buttonSearching.setOnClickListener(view -> {
            TextInputEditText inputSearchEditText = findViewById(R.id.inputSearchEditText);
            ConstraintLayout mainTitle = findViewById(R.id.mainTitle);
            mainTitle.setVisibility(View.VISIBLE);
            ConstraintLayout serchTitle = findViewById(R.id.serchTitle);
            serchTitle.setVisibility(View.GONE);

            Bundle args = new Bundle();
            args.putString("name", inputSearchEditText.getText().toString().trim());
            inputSearchEditText.setText("");
            navController.navigate(R.id.productsListFragment, args);
        });

        setupHeaderButtons();
    }

    private void setupHeaderButtons() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ViewGroup rootView = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = getWindow().getDecorView().getBackground();

        // Настройка BlurView
        BlurView blurHeader = findViewById(R.id.blurHeader);
        if (blurHeader != null) {
            blurHeader.setupWith(rootView)
                    .setFrameClearDrawable(windowBackground)
                    .setBlurEnabled(true)
                    .setBlurAutoUpdate(true)
                    .setBlurRadius(20f);
        }

        // Кнопка меню
        ImageButton menuButton = findViewById(R.id.buttonMenu);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> toggleDrawer(drawerLayout));
        }

        // Кнопка назад
        ImageButton backButton = findViewById(R.id.buttonBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
    }

    private void toggleDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout != null) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.navHostFragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith("openfashion://auth-callback")) {
            Log.d("MainActivity", "Email confirmed via deep link");
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}