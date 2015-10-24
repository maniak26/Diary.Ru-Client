package adonai.diary_browser;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;
import com.android.vending.util.Inventory;
import com.android.vending.util.Purchase;

import adonai.diary_browser.database.DatabaseHandler;

public class DiaryFragment extends Fragment implements Handler.Callback {
    private static final int HANDLE_APP_START = -100;
    private static final String SKU_DONATE = "small";

    protected DiarySlidePane slider;
    protected DiaryWebView mPageBrowser;
    protected DiaryFragment mainPane;
    protected MessageSenderFragment messagePane;

    public BrowseHistory browserHistory;

    int mCurrentComponent = 0;
    protected MaterialDialog pd;


    protected Handler mUiHandler;
    protected NetworkService mService;
    protected DiaryHttpClient mHttpClient;


    protected String pageToLoad;
    protected String textToWrite;
    protected Uri imageToUpload;

    protected SwipeRefreshLayout swipeList;
    protected SwipeRefreshLayout swipeBrowser;

    public void onCreate(Bundle savedInstanceState) {

        mUiHandler = new Handler(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.content_layout, container, false);
    }

    public void onStart() {
        mUiHandler.sendEmptyMessage(HANDLE_APP_START); // ensure that service is running

    }

    public DiaryActivity getDiaryActivity() {
        return (DiaryActivity) getActivity();
    }

    SlidingPaneLayout.PanelSlideListener sliderListener = new SlidingPaneLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelOpened(View view) {
            messagePane.setHasOptionsMenu(true);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelClosed(View view) {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(true);
        }
    };

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLE_APP_START:
                mService = NetworkService.getInstance(getDiaryActivity());
                if (mService == null)
                    mUiHandler.sendEmptyMessageDelayed(HANDLE_APP_START, 50);
                else {
                    getDiaryActivity().setRequestedOrientation(mService.mOrientation);
                    mHttpClient = mService.mNetworkClient;
                    mUiHandler.sendEmptyMessage(Utils.HANDLE_START); // выполняем стартовые действия для всех остальных

                    if (getDiaryActivity().getPackageName().contains("pro"))
                        break;

                    showChangesPage();
                }
                break;
            case Utils.HANDLE_SERVICE_ERROR:
                Toast.makeText(getDiaryActivity().getApplicationContext(), getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_CONNECTIVITY_ERROR:
                Toast.makeText(getDiaryActivity().getApplicationContext(), getString((Integer) msg.obj), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_NOTFOUND_ERROR:
                Toast.makeText(getDiaryActivity().getApplicationContext(), getString(R.string.notfound_error), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_JUST_DO_GET:
                Toast.makeText(getDiaryActivity().getApplicationContext(), getString(R.string.completed), Toast.LENGTH_SHORT).show();
                break;
        }

        if (pd != null) {
            pd.dismiss();
            pd = null;
        }

        return true;
    }

    private void showChangesPage() {
        // Показываем страничку изменений
        try {
            final String current = getDiaryActivity().getPackageManager().getPackageInfo(getDiaryActivity().getPackageName(), 0).versionName;
            final String stored = getDiaryActivity().mSharedPrefs.getString("stored.version", "");
            boolean show = getDiaryActivity().mSharedPrefs.getBoolean("show.version", true);
            if (show && !current.equals(stored)) {
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getDiaryActivity().isFinishing()) // бывает при неверной авторизации
                            return;

                        // TODO: move to XML
                        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getDiaryActivity());
                        TextView message = new TextView(getDiaryActivity());
                        message.setMovementMethod(LinkMovementMethod.getInstance());
                        message.setGravity(Gravity.CENTER_HORIZONTAL);
                        message.setText(Html.fromHtml(getString(R.string.ad_text)));
                        TypedValue color = new TypedValue();
                        getDiaryActivity().getTheme().resolveAttribute(R.attr.text_color_main, color, true);
                        message.setTextColor(color.data);
                        builder.setTitle(R.string.ad_title).setView(message);
                        builder.setPositiveButton(R.string.help, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                purchaseGift();
                            }
                        });
                        builder.setNegativeButton(R.string.later, null);
                        builder.create().show();
                    }
                }, 5000);

                getDiaryActivity().mSharedPrefs.edit()
                        .putString("stored.version", current)
                        .apply();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // не сработало - и ладно
        }
    }

    protected void onMessagePaneRemove(boolean reload) {
        slider.closePane();
    }
    protected void purchaseGift() {
        if (getDiaryActivity().mCanBuy) {
            getDiaryActivity().mHelper.launchPurchaseFlow(getDiaryActivity(), SKU_DONATE, 6666, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isSuccess()) {
                        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getDiaryActivity());
                        builder.setTitle(R.string.completed).setMessage(R.string.thanks);
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.create().show();
                    }

                    getDiaryActivity().mHelper.queryInventoryAsync(false, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isSuccess()) {
                                if (inv.getPurchase(SKU_DONATE) != null)
                                    getDiaryActivity().mHelper.consumeAsync(inv.getPurchase(SKU_DONATE), null);
                            }
                        }
                    });
                }
            }, "NothingAndNowhere" + getDiaryActivity().getUser().getUserName());
        }
    }


    public void handleBackground(int opCode, Object body) {
        if(pd != null && pd.isShowing()) {
            pd.setTitle(R.string.loading);
            pd.setContent(getString(R.string.loading_data));
        } else {
            pd = new MaterialDialog.Builder(getDiaryActivity())
                    .title(R.string.loading)
                    .content(R.string.loading_data)
                    .progress(true, 0)
                    .build();
            pd.show();
        }

        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mHttpClient.abort();
            }
        });
        mService.handleRequest(opCode, body);
    }

    public void handleUi(int opCode, Object body) {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }

    public void handleFontChange(String currSize) {
        try {
            int realNum = Integer.parseInt(currSize);
            mPageBrowser.getSettings().setMinimumFontSize(realNum);
        } catch (NumberFormatException ex) {
            Toast.makeText(getDiaryActivity(), R.string.invalid_number, Toast.LENGTH_SHORT).show();
        }
    }

    protected UserData getUser() {
        return getDiaryActivity().getUser();
    }


    public void onBackPressed() {

        if (slider.isOpen())
            slider.closePane();
        else if (browserHistory.hasPrevious()) {
            browserHistory.moveBack();
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(browserHistory.getUrl(), false));
        } else
            getDiaryActivity().finish();
    }

    protected void finish()
    {
        getDiaryActivity().finish();
    }


    public void handleScroll(int direction) {
        ;
    }
}
