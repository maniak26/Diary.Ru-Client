package adonai.diary_browser;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.Openable;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.UmailPage;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class UmailList extends Activity implements IRequestHandler, OnClickListener
{
    public void handleBackground(int opCode, Object body)
    {
    	mMessageBrowser.getRefreshableView().stopLoading();
        pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, true);
        pd.setOnCancelListener(new OnCancelListener() 
        {
            public void onCancel(DialogInterface dialog)
            {
                mDHCL.abort();
            }
        });
        mHandler.sendMessage(mHandler.obtainMessage(opCode, body));
    }

    public void handleUi(int opCode, Object body)
    {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }
    
	static final int TAB_INCOMING                                   =   0;
	static final int TAB_OUTCOMING                                  =   1;
	
	static final int HANDLE_OPEN_FOLDER                             =   0;
	static final int HANDLE_OPEN_MAIL                               =   1;
    static final int HANDLE_PROGRESS                                =   7;
    static final int HANDLE_PROGRESS_2                              =   8;
    static final int HANDLE_CONNECTIVITY_ERROR                      = -20;
    
    private static final int PART_WEB = 0;
    private static final int PART_LIST = 1;
    
    private int mCurrentComponent = 1;
	
    DiaryHttpClient mDHCL = Globals.mDHCL;
    UserData mUser = Globals.mUser;
    Map<String, String> namesUrls;
    
    DiaryWebView mMessageBrowser;
    PullToRefreshListView mFolderBrowser;
    DiaryListArrayAdapter mFolderAdapter;
    ProgressDialog pd;
    TabWidget mTabs;
    TextView mIncoming;
    TextView mOutcoming;
    
    Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, WorkerCallback);
        mUiHandler = new Handler(UiCallback);
        
        setContentView(R.layout.umail_list_a);
        mMessageBrowser = (DiaryWebView) findViewById(R.id.umessage_browser);
        mMessageBrowser.setDefaultSettings();
        mFolderBrowser = (PullToRefreshListView) findViewById(R.id.ufolder_browser);
        mFolderAdapter = new DiaryListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.currentUmails);
        mTabs = (TabWidget) findViewById(R.id.folder_selector);
        
        mIncoming = (TextView) findViewById(R.id.incoming);
        mOutcoming = (TextView) findViewById(R.id.outcoming);
        mIncoming.setOnClickListener(this);
        mOutcoming.setOnClickListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        
        handleBackground(HANDLE_OPEN_FOLDER, "http://www.diary.ru/u-mail/folder/?f_id=1");
    }

    @Override
    public void onBackPressed()
    {
        if(mCurrentComponent == PART_WEB)
            setCurrentVisibleComponent(PART_LIST);
        else
        {
            Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
            returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(returnIntent);
            finish();
        }
    }
    
    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
            	case HANDLE_OPEN_FOLDER:
            	    setCurrentVisibleComponent(PART_LIST);
            		mFolderAdapter = new DiaryListArrayAdapter(UmailList.this, android.R.layout.simple_list_item_1, mUser.currentUmails);
            		mFolderBrowser.getRefreshableView().removeFooterView(mFolderBrowser.getRefreshableView().findViewWithTag("footer"));
                    if(mUser.currentUmails.getPageLinks() != null)
                    {
                        LinearLayout LL = new LinearLayout(mFolderBrowser.getRefreshableView().getContext());
                        LL.setTag("footer");
                        Spanned pageLinks = mUser.currentUmails.getPageLinks();
                        URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                        for(URLSpan url : URLs)
                        {
                            Button click = new Button(LL.getContext());
                            click.setMaxLines(1);
                            click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)));
                            click.setTag(url.getURL());
                            click.setOnClickListener(UmailList.this);
                            LL.addView(click);
                            
                            LayoutParams LP = (LayoutParams) click.getLayoutParams();
                            LP.width = LayoutParams.MATCH_PARENT;
                            LP.weight = 1.0f;
                        }
                        mFolderBrowser.getRefreshableView().addFooterView(LL);
                    }
            		mFolderBrowser.setAdapter(mFolderAdapter);
            		pd.dismiss();
            	return true;
            	case HANDLE_OPEN_MAIL:
            	    setCurrentVisibleComponent(PART_WEB);
            	    mMessageBrowser.getRefreshableView().loadDataWithBaseURL(mUser.currentUmailPage.get_page_URL(), mUser.currentUmailPage.get_content().html(), null, "utf-8", mUser.currentUmailPage.get_page_URL());
                    setTitle(mUser.currentUmailPage.get_content().title());
                    mMessageBrowser.onRefreshComplete();
                    pd.dismiss();
                return true;
                case HANDLE_PROGRESS:
                    if(pd != null && pd.isShowing())
                        pd.setMessage(getString(R.string.parsing_data));
                return true;
                case HANDLE_CONNECTIVITY_ERROR:
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(), "Connection error", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            return false;
        }
    };
    
    
    Handler.Callback WorkerCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            
            try
            {
                switch(message.what)
                {
                    case HANDLE_OPEN_FOLDER:
                    {
                        HttpResponse page = mDHCL.postPage((String)message.obj, null);
                        if(page == null)
                        {
                            mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                            return false;
                        }
                        String uFolder = EntityUtils.toString(page.getEntity());
                        serializeUmailListPage(uFolder);
                        
                        mUiHandler.sendEmptyMessage(HANDLE_OPEN_FOLDER);
                    }
                    return true;
                    case HANDLE_OPEN_MAIL:
                    {
                        HttpResponse page = mDHCL.postPage((String)message.obj, null);
                        if(page == null)
                        {
                            mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                            return false;
                        }
                        String uMail = EntityUtils.toString(page.getEntity());
                        serializeUmailPage(uMail);
                        
                        mUiHandler.sendEmptyMessage(HANDLE_OPEN_MAIL);
                    }
                }
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return false;
        }
    };

	public void onClick(View view)
	{
		switch (view.getId())
		{
		case R.id.incoming:
			mTabs.setCurrentTab(TAB_INCOMING);
			handleBackground(HANDLE_OPEN_FOLDER, "http://www.diary.ru/u-mail/folder/?f_id=1");
			break;
		case R.id.outcoming:
			mTabs.setCurrentTab(TAB_OUTCOMING);
			handleBackground(HANDLE_OPEN_FOLDER, "http://www.diary.ru/u-mail/folder/?f_id=2");
			break;
		case R.id.title:
		    int pos = mFolderBrowser.getRefreshableView().getPositionForView((View) view.getParent());
            Openable uMail = (Openable) mFolderBrowser.getRefreshableView().getAdapter().getItem(pos);
            
		    handleBackground(HANDLE_OPEN_MAIL, uMail.get_URL());
		    break;
		}
		
		if(view instanceof Button) // нижние панельки
        {
		    handleBackground(HANDLE_OPEN_FOLDER, view.getTag());
        }
	}
	
	private void setCurrentVisibleComponent(int needed)
    {   
	    mFolderBrowser.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
	    mMessageBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
	    mCurrentComponent = needed;
    }

    void serializeUmailListPage(String dataPage)
    {
        mUser.currentUmails = new DiaryListPage();
        mUser.currentUmails.set_URL(Globals.currentURL);
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
            
        Element table = rootNode.getElementsByAttributeValue("class", "table l").first();
        if(table == null) // Нет вообще никаких сообщений, заканчиваем
            return;
        
        Element pages = rootNode.select("table.pages").first();
        if(pages != null)
            mUser.currentUmails.setPageLinks(Html.fromHtml(pages.outerHtml()));
        
        Elements rows = table.getElementsByTag("td");
        Element title = null, author = null, last_post = null;
        for (int i = 0; i < rows.size(); ++i)
        {
            if (title == null)
                title = rows.get(i).getElementsByClass("withfloat").first();

            if(author == null)
                author = rows.get(i).select("[target=_blank]").first();
            
            if (last_post == null)
                if (rows.get(i).className().equals("r"))
                    last_post = rows.get(i);
            
            if (title != null && author != null && last_post != null)
            {
                Openable diary = new Openable();
                diary.set_title(title.getElementsByTag("b").text());
                diary.set_URL(title.attr("href"));
                
                diary.set_author(author.text());
                String authorData = author.attr("href");
                diary.set_author_URL(authorData);
                diary.set_ID(authorData.substring(authorData.lastIndexOf("?") + 1));
                
                diary.set_last_post(last_post.text());
                diary.set_last_post_URL(last_post.attr("href"));
                
                mUser.currentUmails.add(diary);
                title = author = last_post = null;
            }
        }
    }
    
    public void serializeUmailPage(String dataPage) throws IOException
    {
        UmailPage scannedUmail = new UmailPage();
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        
        Document rootNode = Jsoup.parse(dataPage);
        scannedUmail.setUmail_URL(Globals.currentURL);
        scannedUmail.setUmail_ID(scannedUmail.getUmail_URL().substring(scannedUmail.getUmail_URL().lastIndexOf('=') + 1));
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        
        Elements postsArea = rootNode.select("table.box, table.box + div");
        if(postsArea.isEmpty()) // Нет вообще никаких постов, заканчиваем
            return;
        
        Elements result = postsArea.clone();
        Document resultPage = Document.createShell(Globals.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        
        scannedUmail.set_content(resultPage);
        mUser.currentUmailPage = scannedUmail;
    }
}
