package adonai.diary_browser;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import ru.diary.antic1tizen.R;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends PullToRefreshWebView
{
 // текущий контекст
    public static final int IMAGE_SAVE = 0;
    public static final int IMAGE_COPY_URL = 1;
    public static final int IMAGE_OPEN = 2;

    DiaryList mActivity;
    UserData mUser;
    
    public DiaryWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public DiaryWebView(Context context, Mode mode)
    {
        super(context, mode);
        init();
    }

    public DiaryWebView(Context context)
    {
        super(context);
        init();
    }
    
    public void init()
    {
        mActivity = Globals.mMain;
        mUser = Globals.mUser;
    }
    
    public void setDefaultSettings()
    {
        WebSettings settings = getRefreshableView().getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("windows-1251");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(false);
        settings.setLightTouchEnabled(true);
        settings.setBuiltInZoomControls(true);
        getRefreshableView().setWebViewClient(new DiaryWebClient());
        getRefreshableView().setBackgroundColor(0xFFF7F2EE);
        setOnRefreshListener(new WebPageRefresher());
    }

    private class DiaryWebClient extends WebViewClient
    {
        // Override page so it's load on my view only
        @Override
        public boolean shouldOverrideUrlLoading(WebView  view, String  url)
        {
         if ( url.contains("diary.ru"))
         {
            mActivity.pd = ProgressDialog.show(view.getContext(), mActivity.getString(R.string.loading), mActivity.getString(R.string.loading_data), true, true);
            mActivity.mHandler.sendMessage(mActivity.mHandler.obtainMessage(DiaryList.HANDLE_PICK_URL, url));
            return true;
         }
          
         // Return true to override url loading (In this case do nothing).
         return true;
        }
    };
    
    public class WebPageRefresher implements OnRefreshListener<WebView>
    {

        public void onRefresh(PullToRefreshBase<WebView> refreshView)
        {
            switch (refreshView.getId())
            {
                case R.id.page_browser:
                    if(mUser.currentDiaryPage.get_post_URL().equals("")) // если это страничка постов
                        mActivity.mHandler.sendMessage(mActivity.mHandler.obtainMessage(DiaryList.HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.currentDiaryPage.get_diary_URL(), true)));
                    else // если это страничка комментариев
                        mActivity.mHandler.sendMessage(mActivity.mHandler.obtainMessage(DiaryList.HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.currentDiaryPage.get_post_URL(), true)));
                break;
            }
        }
    }
}