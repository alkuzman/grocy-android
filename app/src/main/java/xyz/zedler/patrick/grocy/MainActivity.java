package xyz.zedler.patrick.grocy;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.DrawerBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.CustomBottomAppBar;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private final static boolean DEBUG = false;

    private RequestQueue requestQueue;
    private WebRequest request;
    private SharedPreferences sharedPrefs;
    private FragmentManager fragmentManager;
    private GrocyApi grocyApi;
    private long lastClick = 0;
    private BottomAppBarRefreshScrollBehavior scrollBehavior;
    private String uiMode = Constants.UI.STOCK_DEFAULT;

    private CustomBottomAppBar bottomAppBar;
    private Fragment fragmentCurrent;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean("night_mode", false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        setContentView(R.layout.activity_main);

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(
                getApplicationContext()
        ).getRequestQueue();

        request = new WebRequest(requestQueue);

        // API

        grocyApi = new GrocyApi(this);

        // VIEWS

        bottomAppBar = findViewById(R.id.bottom_app_bar);
        fab = findViewById(R.id.fab_main);

        // BOTTOM APP BAR

        bottomAppBar.setNavigationOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            startAnimatedIcon(bottomAppBar.getNavigationIcon());
            showBottomSheet(new DrawerBottomSheetDialogFragment());
        });
        bottomAppBar.setOnMenuItemClickListener((MenuItem item) -> {
            if (SystemClock.elapsedRealtime() - lastClick < 500) return false;
            lastClick = SystemClock.elapsedRealtime();
            startAnimatedIcon(item);
            switch (item.getItemId()) {
                // STOCK DEFAULT
                case R.id.action_search:
                    if(!uiMode.equals(Constants.UI.STOCK_DEFAULT)) return false;
                    ((StockFragment) fragmentCurrent).setUpSearch();
                    break;
            }
            return true;
        });

        scrollBehavior = new BottomAppBarRefreshScrollBehavior(
                this
        );
        scrollBehavior.setUpBottomAppBar(bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(true);

        fragmentManager = getSupportFragmentManager();

        if(sharedPrefs.getString(Constants.PREF.SERVER_URL, "").equals("")) {
            startActivityForResult(
                    new Intent(this, LoginActivity.class),
                    Constants.REQUEST.LOGIN
            );
        } else {
            setUp();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.REQUEST.LOGIN && resultCode == Constants.RESULT.SUCCESS) {
            setUp();
        }
    }

    private void setUp() {
        // STOCK FRAGMENT
        fragmentCurrent = new StockFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.linear_container_main, fragmentCurrent)
                .commit();
        bottomAppBar.changeMenu(R.menu.menu_stock, CustomBottomAppBar.MENU_END, false);
    }

    public void updateUI(String uiMode, String origin) {
        Log.i(TAG, "updateUI: " + uiMode + ", origin = " + origin);
        this.uiMode = uiMode;

        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
                scrollBehavior.setUpScroll(R.id.scroll_stock);
                updateBottomAppBar(Constants.FAB_POSITION.CENTER, R.menu.menu_stock, true);

                /*String fabPosition;
                if(sharedPrefs.getBoolean(PREF_FAB_IN_FEED, DEFAULT_FAB_IN_FEED)) {
                    fabPosition = FAB_POSITION_CENTER;
                } else {
                    fabPosition = FAB_POSITION_GONE;
                }

                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add_channel,
                        FAB_TAG_ADD,
                        animated,
                        () -> {
                            showBottomSheet(new ChannelAddBottomSheetDialogFragment());
                            setUnreadCount(
                                    sharedPrefs.getInt(PREF_UNREAD_COUNT, 0) + 1
                            );
                        }
                );*/
                break;
            default: Log.e(TAG, "updateUI: wrong uiMode argument: " + uiMode);
        }
    }

    private void updateBottomAppBar(
            int newFabPosition,
            @MenuRes int newMenuId,
            boolean animated
    ) {
        switch (newFabPosition) {
            case Constants.FAB_POSITION.CENTER:
                if(fab.isOrWillBeHidden()) fab.show();
                bottomAppBar.changeMenu(newMenuId, CustomBottomAppBar.MENU_END, animated);
                bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_CENTER);
                bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim);
                scrollBehavior.setTopScrollVisibility(true);
                break;
            case Constants.FAB_POSITION.END:
                if(fab.isOrWillBeHidden()) fab.show();
                bottomAppBar.changeMenu(newMenuId, CustomBottomAppBar.MENU_START, animated);
                bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END);
                bottomAppBar.hideNavigationIcon();
                scrollBehavior.setTopScrollVisibility(false);
                break;
            case Constants.FAB_POSITION.GONE:
                if(fab.isOrWillBeShown()) fab.hide();
                bottomAppBar.changeMenu(newMenuId, CustomBottomAppBar.MENU_END, animated);
                bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim);
                scrollBehavior.setTopScrollVisibility(true);
                break;
        }
    }

    public void setLocations(List<Location> locations) {
        Menu menu = bottomAppBar.getMenu();
        SubMenu menuLocations = menu.findItem(R.id.action_filter_location).getSubMenu();
        menuLocations.clear();
        for(Location location : locations) {
            menuLocations.add(location.getName()).setOnMenuItemClickListener(item -> {
                if(!uiMode.equals(Constants.UI.STOCK_DEFAULT)) return false;
                ((StockFragment) fragmentCurrent).filterLocation(location);
                return true;
            });
        }
    }

    public void setProductGroups(List<ProductGroup> productGroups) {
        Menu menu = bottomAppBar.getMenu();
        SubMenu menuProductGroups = menu.findItem(R.id.action_filter_product_group).getSubMenu();
        menuProductGroups.clear();
        for(ProductGroup productGroup : productGroups) {
            menuProductGroups.add(productGroup.getName()).setOnMenuItemClickListener(item -> {
                if(!uiMode.equals(Constants.UI.STOCK_DEFAULT)) return false;
                ((StockFragment) fragmentCurrent).filterProductGroup(productGroup);
                return true;
            });
        }
    }

    private void updateFab(
            @DrawableRes int iconResId,
            @StringRes int tooltipStringId,
            String tag,
            boolean animated,
            Runnable onClick
    ) {
        replaceFabIcon(iconResId, tag, animated);
        fab.setOnClickListener(v -> {
            startAnimatedIcon(fab.getDrawable());
            onClick.run();
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fab.setTooltipText(getString(tooltipStringId));
        }
    }

    @Override
    public void onBackPressed() {

        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
                super.onBackPressed();
                break;
            case Constants.UI.STOCK_SEARCH:
                ((StockFragment) fragmentCurrent).dismissSearch();
                break;

            default: Log.e(TAG, "onBackPressed: missing case, UI mode = " + uiMode);
        }
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void showSnackbar(Snackbar snackbar) {
        snackbar.setAnchorView(findViewById(R.id.fab_main)).show();
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet) {
        String tag = bottomSheet.toString();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null || !fragment.isVisible()) {
            fragmentManager.beginTransaction().add(bottomSheet, tag).commit();
            if(DEBUG) Log.i(TAG, "showBottomSheet: " + tag);
        } else if(DEBUG) Log.e(TAG, "showBottomSheet: sheet already visible");
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public void showKeyboard(EditText editText) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(
                        editText,
                        InputMethodManager.SHOW_IMPLICIT
                );
    }

    public void hideKeyboard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(
                        findViewById(android.R.id.content).getWindowToken(),
                        0
                );
    }

    public GrocyApi getGrocy() {
        return grocyApi;
    }

    private void replaceFabIcon(@DrawableRes int icon, String tag, boolean animated) {
        if(!tag.equals(fab.getTag())) {
            if(animated) {
                int duration = 400;
                ValueAnimator animOut = ValueAnimator.ofInt(fab.getImageAlpha(), 0);
                animOut.addUpdateListener(
                        animation -> fab.setImageAlpha((int) animation.getAnimatedValue())
                );
                animOut.setDuration(duration / 2);
                animOut.setInterpolator(new FastOutSlowInInterpolator());
                animOut.start();

                new Handler().postDelayed(() -> {
                    fab.setImageResource(icon);
                    ValueAnimator animIn = ValueAnimator.ofInt(0, 255);
                    animIn.addUpdateListener(
                            animation -> fab.setImageAlpha((int) (animation.getAnimatedValue()))
                    );
                    animIn.setDuration(duration / 2);
                    animIn.setInterpolator(new FastOutSlowInInterpolator());
                    animIn.start();
                }, duration / 2);
            } else {
                fab.setImageResource(icon);
            }
            fab.setTag(tag);
            Log.i(TAG, "replaceFabIcon: replaced successfully, animated = " + animated);
        } else {
            Log.i(TAG, "replaceFabIcon: not replaced, tags are identical");
        }
    }

    private void startAnimatedIcon(Drawable drawable) {
        try {
            ((Animatable) drawable).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }

    public void startAnimatedIcon(MenuItem item) {
        try {
            try {
                ((Animatable) item.getIcon()).start();
            } catch (ClassCastException e) {
                Log.e(TAG, "startAnimatedIcon(MenuItem) requires AVD!");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "startAnimatedIcon(MenuItem): Icon missing!");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestQueue.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestQueue.stop();
    }
}