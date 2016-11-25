package gr.thmmy.mthmmy.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.data.Post;
import gr.thmmy.mthmmy.utils.CircularNetworkImageView;
import gr.thmmy.mthmmy.utils.ImageController;
import okhttp3.Request;
import okhttp3.Response;

public class TopicActivity extends BaseActivity {

//-----------------------------------------CLASS VARIABLES------------------------------------------

    /* --Posts-- */
    private List<Post> postsList;
    private LinearLayout postsLinearLayout;
    private static final int NO_POST_FOCUS = -1;
    private int postFocus = NO_POST_FOCUS;
    /* --Posts end-- */
    /* --Topic's pages-- */
    private int thisPage = 1;
    private String base_url = "";
    private int numberOfPages = 1;
    private final SparseArray<String> pagesUrls = new SparseArray<>();
    //Page select
    private TextView pageIndicator;
    private final Handler repeatUpdateHandler = new Handler();
    private final long INITIAL_DELAY = 500;
    private boolean autoIncrement = false;
    private boolean autoDecrement = false;
    private static final int SMALL_STEP = 1;
    private static final int LARGE_STEP = 10;
    private Integer pageValue;
    /* --Topic's pages end-- */
    /* --Thumbnail-- */
    private static final int THUMBNAIL_SIZE = 80;
    private ImageLoader imageLoader = ImageController.getInstance().getImageLoader();
    /* --Thumbnail end-- */

    //Other variables
    private ProgressBar progressBar;
    private static final String TAG = "TopicActivity";
    private String topicTitle;
    private String parsedTitle;
    private ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        Bundle extras = getIntent().getExtras();
        topicTitle = getIntent().getExtras().getString("TOPIC_TITLE");

        //Variables initialization
        postsLinearLayout = (LinearLayout) findViewById(R.id.posts_list);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (imageLoader == null)
            imageLoader = ImageController.getInstance().getImageLoader();

        postsList = new ArrayList<>();

        actionbar = getSupportActionBar();
        if (actionbar != null)
            if(!Objects.equals(topicTitle, ""))
                actionbar.setTitle(topicTitle);


        ImageButton firstPage = (ImageButton) findViewById(R.id.page_first_button);
        ImageButton previousPage = (ImageButton) findViewById(R.id.page_previous_button);
        pageIndicator = (TextView) findViewById(R.id.page_indicator);
        ImageButton nextPage = (ImageButton) findViewById(R.id.page_next_button);
        ImageButton lastPage = (ImageButton) findViewById(R.id.page_last_button);

        initDecrementButton(firstPage, LARGE_STEP);
        initDecrementButton(previousPage, SMALL_STEP);
        initIncrementButton(nextPage, SMALL_STEP);
        initIncrementButton(lastPage, LARGE_STEP);

        new TopicTask().execute(extras.getString("TOPIC_URL")); //Attempt data parsing
    }

    @Override
    protected void onDestroy() { //When finished cancel whatever request can still be canceled
        super.onDestroy();
        ImageController.getInstance().cancelPendingRequests();
    }

    private void initIncrementButton(ImageButton increment, final int step){
        // Increment once for a click
        increment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!autoIncrement && step == LARGE_STEP){ //If just clicked go to last page
                    changePage(numberOfPages - 1);
                    return;
                }
                //Clicked and holden
                autoIncrement = false; //Stop incrementing
                increment(step);
                changePage(pageValue - 1);
            }
        });

        // Auto increment for a long click
        increment.setOnLongClickListener(
                new View.OnLongClickListener(){
                    public boolean onLongClick(View arg0) {
                        autoIncrement = true;
                        repeatUpdateHandler.postDelayed(new RepetitiveUpdater(step), INITIAL_DELAY);
                        return false;
                    }
                }
        );

        // When the button is released
        increment.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if( event.getAction() == MotionEvent.ACTION_UP && autoIncrement ){
                    changePage(pageValue - 1);
                }
                return false;
            }
        });
    }

    private void initDecrementButton(ImageButton decrement, final int step){
        // Decrement once for a click
        decrement.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!autoDecrement && step == LARGE_STEP){ //If just clicked go to first page
                    changePage(0);
                    return;
                }
                //Clicked and holden
                autoDecrement = false; //Stop incrementing
                decrement(step);
                changePage(pageValue - 1);
            }
        });


        // Auto Decrement for a long click
        decrement.setOnLongClickListener(
                new View.OnLongClickListener(){
                    public boolean onLongClick(View arg0) {
                        autoDecrement = true;
                        repeatUpdateHandler.postDelayed( new RepetitiveUpdater(step), INITIAL_DELAY);
                        return false;
                    }
                }
        );

        // When the button is released
        decrement.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if( event.getAction() == MotionEvent.ACTION_UP && autoDecrement ){
                    changePage(pageValue - 1);
                }
                return false;
            }
        });
    }

    private void increment(int step){
        if( pageValue < numberOfPages - step){
            pageValue = pageValue + step;
        }
        else
            pageValue = numberOfPages;
        pageIndicator.setText(pageValue + "/" + String.valueOf(numberOfPages));
        if(pageValue >= 1000)
            pageIndicator.setTextSize(16);
        else
            pageIndicator.setTextSize(20);
    }

    private void decrement(int step){
        if( pageValue > step)
            pageValue = pageValue - step;
        else
            pageValue = 1;
        pageIndicator.setText(pageValue + "/" + String.valueOf(numberOfPages));
        if(numberOfPages >= 1000)
            pageIndicator.setTextSize(16);
        else
            pageIndicator.setTextSize(20);
    }

    private void changePage(int pageRequested){
        if(pageRequested != thisPage - 1){
            //Restart activity with new page
            Intent intent = getIntent();
            intent.putExtra("TOPIC_URL", pagesUrls.get(pageRequested));
            intent.putExtra("TOPIC_TITLE", topicTitle);
            finish();
            startActivity(intent);
        }
    }


//---------------------------------------TOPIC ASYNC TASK-------------------------------------------
    public class TopicTask extends AsyncTask<String, Void, Boolean> {
        //Class variables
        private static final String TAG = "TopicTask"; //Separate tag for AsyncTask

        //Show a progress bar until done
        protected void onPreExecute() {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        protected Boolean doInBackground(String... strings) {
            Document document;
            base_url = strings[0].substring(0, strings[0].lastIndexOf(".")); //This topic's base url
            String pageUrl = strings[0]; //This page's url

            //Find message focus if present
            {
                if(pageUrl.contains("msg")){
                    String tmp = pageUrl.substring(pageUrl.indexOf("msg") + 3);
                    if(tmp.contains(";"))
                        postFocus = Integer.parseInt(tmp.substring(0, tmp.indexOf(";")));
                    else
                        postFocus = Integer.parseInt(tmp.substring(0, tmp.indexOf("#")));
                }
            }

            Request request = new Request.Builder()
                    .url(pageUrl)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                document = Jsoup.parse(response.body().string());
                parse(document); //Parse data
                return true;
            } catch (SSLHandshakeException e) {
                Log.w(TAG, "Certificate problem (please switch to unsafe connection).");
            } catch (Exception e) {
                Log.e("TAG", "ERROR", e);
            }
            return false;
        }

        protected void onPostExecute(Boolean result) {
            if (!result) { //Parse failed!
                //Should never happen
                Toast.makeText(getBaseContext()
                        , "Fatal error!\n Aborting...", Toast.LENGTH_LONG).show();
                finish();
            }
            //Parse was successful
            progressBar.setVisibility(ProgressBar.INVISIBLE); //Hide progress bar
            populateLayout(); //Show parsed data
            //Set current page
            pageIndicator.setText(String.valueOf(thisPage) + "/" + String.valueOf(numberOfPages));
            pageValue = thisPage;
            if(numberOfPages >= 1000)
                pageIndicator.setTextSize(16);
        }

        /* Parse method */
        private void parse(Document document) {
            //Method's variables
            final int NO_INDEX = -1;

            //Find topic title if missing
            if(topicTitle == null || Objects.equals(topicTitle, "")){
                parsedTitle = document.select("td[id=top_subject]").first().text();
                Log.d(TAG, parsedTitle);
                parsedTitle = parsedTitle.substring(parsedTitle.indexOf("Topic:") + 7
                        , parsedTitle.indexOf("(Read") - 8);
                Log.d(TAG, parsedTitle);
            }

            { //Find current page's index
                Elements findCurrentPage = document.select("td:contains(Pages:)>b"); //Contains pages
                for (Element item : findCurrentPage) {
                    if (!item.text().contains("...") //It's not "..."
                            && !item.text().contains("Pages")) { //Nor "Pages"
                        thisPage = Integer.parseInt(item.text());
                        break;
                    }
                }
            }
            { //Find number of pages
                Elements pages = document.select("td:contains(Pages:)>a.navPages"); //Contains all pages
                if (pages.size() != 0) {
                    numberOfPages = thisPage; //Initialize the number
                    for (Element item : pages) { //Just a max
                        if (Integer.parseInt(item.text()) > numberOfPages)
                            numberOfPages = Integer.parseInt(item.text());
                    }
                }
                for (int i = 0; i < numberOfPages; i++) {
                    //Generate each page's url from topic's base url +".15*numberOfPage"
                    pagesUrls.put(i, base_url + "." + String.valueOf(i * 15));
                }
            }

            //Each element is a post's row
            Elements rows = document.select("form[id=quickModForm]>table>tbody>tr:matches(on)");

            for (Element item : rows) { //For every post
                //Variables to pass
                String p_userName, p_thumbnailUrl, p_subject, p_post, p_postDate;
                int p_postNum, p_postIndex;

                //Find the Username
                Element userName = item.select("a[title^=View the profile of]").first();
                if (userName == null) { //Deleted profile
                    p_userName = item
                            .select("td:has(div.smalltext:containsOwn(Guest))[style^=overflow]")
                            .first().text();
                    p_userName = p_userName.substring(0, p_userName.indexOf(" Guest"));
                } else
                    p_userName = userName.html();

                //Find thumbnail url
                Element thumbnailUrl = item.select("img.avatar").first();
                p_thumbnailUrl = null; //In case user doesn't have an avatar
                if (thumbnailUrl != null) {
                    p_thumbnailUrl = thumbnailUrl.attr("abs:src");
                }

                //Find subject
                p_subject = item.select("div[id^=subject_]").first().select("a").first().text();

                //Find post's text
                p_post = item.select("div").select(".post").first().html();
                //Add stuff to make it work in WebView
                p_post = ("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />"
                        + p_post); //style.css

                //Find post's submit date
                Element postDate = item.select("div.smalltext:matches(on:)").first();
                p_postDate = postDate.text();
                p_postDate = p_postDate.substring(p_postDate.indexOf("on:") + 4
                        ,p_postDate.indexOf(" »"));

                //Find post's number
                Element postNum = item.select("div.smalltext:matches(Reply #)").first();
                if (postNum == null) { //Topic starter
                    p_postNum = 0;
                } else {
                    String tmp_str = postNum.text().substring(9);
                    p_postNum = Integer.parseInt(tmp_str.substring(0, tmp_str.indexOf(" on")));
                }

                //Find post's index
                Element postIndex = item.select("a[name^=msg]").first();
                if(postIndex == null)
                    p_postIndex = NO_INDEX;
                else {
                    String tmp = postIndex.attr("name");
                    p_postIndex = Integer.parseInt(tmp.substring(tmp.indexOf("msg") + 3));
                }

                //Add new post in postsList
                postsList.add(new Post(p_thumbnailUrl, p_userName, p_subject
                        , p_post, p_postDate, p_postNum, p_postIndex));
            }
        }
        /* Parse method end */
    }
//-------------------------------------TOPIC ASYNC TASK END-----------------------------------------

//----------------------------------------POPULATE UI METHOD----------------------------------------
    private void populateLayout() { //Show parsed data
        //Set topic title if not already present
        if (topicTitle == null || Objects.equals(topicTitle, "")) {
            topicTitle = parsedTitle;
            if (actionbar != null){
                actionbar.setTitle(topicTitle);
            }
        }

        //Initialize an inflater
        LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (Post item : postsList) {
            //Inflate a topic post row layout
            View convertView = inflater.inflate(R.layout.activity_topic_post_row
                    , postsLinearLayout, false);

            if (imageLoader == null)
                imageLoader = ImageController.getInstance().getImageLoader();

            //Initialize layout's graphic elements
            final FrameLayout cardExpandable = (FrameLayout) convertView.findViewById(R.id.card_expandable);
            TextView postDate = (TextView) convertView.findViewById(R.id.post_date);
            TextView postNum = (TextView) convertView.findViewById(R.id.post_number);
            CircularNetworkImageView thumbnail = (CircularNetworkImageView) convertView.findViewById(R.id.thumbnail);
            TextView username = (TextView) convertView.findViewById(R.id.username);
            TextView subject = (TextView) convertView.findViewById(R.id.subject);
            final WebView post = (WebView) convertView.findViewById(R.id.post);
            CardView cardView = (CardView) convertView.findViewById(R.id.card_view);

            //Post's WebView parameters set
            post.setClickable(true);
            post.setWebViewClient(new LinkLauncher());

            //Avoiding errors about layout having 0 width/height
            thumbnail.setMinimumWidth(1);
            thumbnail.setMinimumHeight(1);
            //Set thumbnail size
            thumbnail.setMaxWidth(THUMBNAIL_SIZE);
            thumbnail.setMaxHeight(THUMBNAIL_SIZE);

            //Thumbnail image set
            if (item.getThumbnailUrl() != null) {
                thumbnail.setImageUrl(item.getThumbnailUrl(), imageLoader);
            }

            //Username set
            username.setText(item.getAuthor());

            //Post's submit date set
            postDate.setText(item.getPostDate());

            //Post's index number set
            if (item.getPostNumber() != 0)
                postNum.setText("#" + item.getPostNumber());

            //Subject set
            subject.setText(item.getSubject());

            //Post's text set
            post.loadDataWithBaseURL("file:///android_asset/", item.getContent(), "text/html", "UTF-8", null);

            /* --"Card expand/collapse"-like functionality-- */
            if (item.getPostNumber() != 0) {
                //Should expand/collapse when card is touched
                cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        visibilityChangeAnimate(cardExpandable);
                    }
                });

                //Also when post is clicked
                post.setOnTouchListener(new View.OnTouchListener() {

                    final static int FINGER_RELEASED = 0;
                    final static int FINGER_TOUCHED = 1;
                    final static int FINGER_DRAGGING = 2;
                    final static int FINGER_UNDEFINED = 3;

                    private int fingerState = FINGER_RELEASED;

                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {

                            case MotionEvent.ACTION_DOWN:
                                if (fingerState == FINGER_RELEASED)
                                    fingerState = FINGER_TOUCHED;
                                else
                                    fingerState = FINGER_UNDEFINED;
                                break;

                            case MotionEvent.ACTION_UP:
                                fingerState = FINGER_RELEASED;

                                //If this was a link don't expand the card
                                WebView.HitTestResult htResult = post.getHitTestResult();
                                if (htResult.getExtra() != null
                                        && htResult.getExtra() != null)
                                    return false;

                                //Expand/Collapse card
                                visibilityChangeAnimate(cardExpandable);
                                break;

                            case MotionEvent.ACTION_MOVE:
                                if (fingerState == FINGER_TOUCHED || fingerState == FINGER_DRAGGING) fingerState = FINGER_DRAGGING;
                                else fingerState = FINGER_UNDEFINED;
                                break;

                            default:
                                fingerState = FINGER_UNDEFINED;

                        }
                        return false;
                    }
                });
            }
            /* --"Card expand/collapse"-like functionality end-- */

            //Add view to the linear layout that holds all posts
            postsLinearLayout.addView(convertView);

            //Set post focus
            if(postFocus != NO_POST_FOCUS){
                if(item.getPostIndex() == postFocus){
                    //TODO
                }
            }
        }
    }
//--------------------------------------POPULATE UI METHOD END--------------------------------------

//---------------------------------VISIBILITY CHANGE ANIMATE METHOD---------------------------------
    //Method that animates views visibility changes
    private void visibilityChangeAnimate(final View mCard){
        //If the view is gone fade it in
        if (mCard.getVisibility() == View.GONE) {
            mCard.clearAnimation();
            // Prepare the View for the animation
            mCard.setVisibility(View.VISIBLE);
            mCard.setAlpha(0.0f);

            // Start the animation
            mCard.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mCard.setVisibility(View.VISIBLE);
                        }
                    });
        }
        //If the view is visible fade it out
        else {
            mCard.clearAnimation();

            // Start the animation
            mCard.animate()
                    .translationY(mCard.getHeight())
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mCard.setVisibility(View.GONE);
                        }
                    });
        }
    }
//-------------------------------VISIBILITY CHANGE ANIMATE METHOD END-------------------------------

//--------------------------------------CUSTOM WEBVIEW CLIENT---------------------------------------
    private class LinkLauncher extends WebViewClient {
        //Older versions
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            return handleUri(uri);
        }
        //Newest versions
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            return handleUri(uri);
        }

        //Handle url clicks
        private boolean handleUri(final Uri uri) {
            //Method always returns true as we don't want any url to be loaded in WebViews

            Log.i(TAG, "Uri =" + uri);
            final String host = uri.getHost(); //Get requested url's host

            //Determine if you are going to pass the url to a
            //host's application activity or load it in a browser.
            if (Objects.equals(host, "www.thmmy.gr")) {
                //This is my web site, so figure out what Activity should launch
                if (uri.toString().contains("topic=")) { //This url points to a topic
                    //Is the link pointing to current topic?
                    if(Objects.equals(
                            uri.toString().substring(0, uri.toString().lastIndexOf(".")), base_url)){
                        //Don't restart Activity
                        //Just change post focus
                        //TODO
                    }
                    else {
                        //Restart activity with new data
                        Intent intent = getIntent();
                        intent.putExtra("TOPIC_URL", uri.toString());
                        intent.putExtra("TOPIC_TITLE", "");
                        finish();
                        startActivity(intent);
                    }
                }
                return true;
            }
            //Otherwise, the link is not for a page on my site, so launch
            //another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }
    }
//------------------------------------CUSTOM WEBVIEW CLIENT END-------------------------------------

    class RepetitiveUpdater implements Runnable {
        private final int step;

        RepetitiveUpdater(int step){this.step = step;}
        public void run() {
            long REPEAT_DELAY = 250;
            if( autoIncrement ){
                increment(step);
                repeatUpdateHandler.postDelayed( new RepetitiveUpdater(step), REPEAT_DELAY);
            } else if( autoDecrement ){
                decrement(step);
                repeatUpdateHandler.postDelayed( new RepetitiveUpdater(step), REPEAT_DELAY);
            }
        }
    }
}
